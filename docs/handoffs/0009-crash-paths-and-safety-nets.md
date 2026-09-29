# 0009 — Crash Paths, Signing, and CI Safety Nets

**Date:** 2026-09-28 · **Branch:** `phase-0-aurora-foundation`
**Status:** Complete. Fixed both user-facing crash paths, the unsigned-release
problem, and the ViewModel lifecycle bug. Wired tests, lint, and the benchmark
module into CI. Added 23 regression tests.

## 1. What changed

### Fixed (user-facing)

- **CSV export crashed on every supported device.** `CsvManager.exportToCsv` wrote
  to `Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)`. With
  `minSdk = 30` and no `MANAGE_EXTERNAL_STORAGE` permission, that throws
  `FileNotFoundException`, and the call site was a bare `scope.launch` with no
  try/catch — so tapping "Export CSV" was a hard crash. It now writes straight to
  the SAF `CreateDocument` uri on `Dispatchers.IO` and returns a row count.
- **CSV import silently duplicated rows and reported nothing.** The import path
  never consulted `DuplicateDetector`, so re-importing the same file duplicated
  every transaction, and a malformed file was indistinguishable from success. It
  now filters against existing transactions, counts unreadable rows, and shows a
  snackbar (added `snackbarHostState` to `SettingsScreen`, previously unwrapped in
  a `Box`).
- **Drive restore could crash and was not atomic.** `BackupSerializer.deserialize`
  used `getString`/`getDouble`/`enum.valueOf` unguarded, so any truncated or
  older-schema backup threw into an unguarded coroutine. The five DAO calls also
  ran outside a transaction, leaving a half-restored database if the process died.
  Restore now goes through a new `BackupRestoreManager` (single
  `database.withTransaction { }`), and deserialization skips individual unreadable
  entries instead of failing wholesale.
- **Restore was a merge, not a restore.** Nothing was deleted first, and the SMS
  queue was re-inserted with `INSERT OR IGNORE`, so rows already present kept their
  *local* status and pending messages in the backup were never retried. The queue
  is now cleared and re-inserted within the same transaction.
- **Restore silently dropped savings goals and classification memory.**
  `BackupData`/`BackupSerializer` did not carry either. Backup format is now v3
  and round-trips both; v2 backups still load.
- **Release builds were unsigned.** There was no `signingConfigs` block at all, so
  `assembleFreeRelease` produced an uninstallable APK while the handoff reported
  "green". Signing now reads `secrets.properties`; without credentials the build
  logs a loud warning and AGP names the artifact `-unsigned`. Verified both paths.

### Fixed (architecture)

- **ViewModels were built with `remember {}`, not `viewModel()`** (`NavGraph` had
  four sites). They were not retained across configuration change, so every
  rotation reset filters/paging/in-flight work, `onCleared()` never ran, and the
  internal coroutine scopes leaked. Added
  `androidx.lifecycle:lifecycle-viewmodel-compose` and a `factoryOf { }` helper
  using the `viewModelFactory { initializer { } }` DSL. `ClassificationViewModel`
  now receives `applicationContext` instead of the Activity, since it is retained.

### Fixed (privacy)

- **`allowBackup="true"` with no backup rules** meant the entire Room database —
  transactions, raw bank SMS bodies, classification memory — was eligible for
  Android Auto Backup, contradicting the app's own on-device promise. Added
  `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml` excluding the
  DB, prefs, and the model directory from both cloud backup and device transfer.

### Safety nets

- **CI ran no tests, no lint, and never built `:benchmark`.** `pr_check.yml` ran
  only two `assemble` tasks. It now runs `testFreeDebugUnitTest`, `lintFreeDebug`,
  `lintPlayDebug`, both debug assembles, `:app:assembleFreeBenchmark` and
  `:benchmark:assemble`, and uploads lint reports. Also widened `paths` to include
  `**.xml`, `app/src/main/**`, `app/src/test/**`, and the `dev` branch.
- **Added a lint config** with `abortOnError`, `warningsAsErrors`,
  `checkDependencies`, `checkReleaseBuilds`, and a generated `app/lint-baseline.xml`
  pinning the 94 pre-existing issues so only *new* ones fail the build.

### Tests added (23)

- `CsvManagerTest` (10) — quote/comma/newline round trip, formula-injection
  guard, malformed-row accounting, negative/zero amounts.
- `BackupSerializerToleranceTest` (13) — malformed/legacy backup tolerance, plus
  round trips for savings goals and classification memory.

## 2. Decisions

- **CSV write target:** write directly to the SAF uri. There is deliberately no
  public-Downloads fallback, since that path cannot succeed on API 30+.
- **Restore semantics:** treat restore as a *replace*, since a merge of an old
  backup onto newer local data silently kept both.
- **Privacy on restore:** restored transactions are re-masked with
  `SensitiveDataMasker` inside the restore transaction, keeping restore behind the
  same choke-point as every other write path.
- **Signing:** optional credentials, never a hard build failure — but the artifact
  is honestly named `-unsigned` and the build warns, so a green
  `assembleFreeRelease` cannot be mistaken for a shippable artifact again.
- **Lint:** baseline the 94 existing issues rather than fixing them all in one
  session; the point is that *new* issues now fail CI.
- Restored strings are resolved with `stringResource` in composition (not
  `context.getString` inside a coroutine) to stay configuration-aware, and CSV
  messages use `String.format` on the template.

## 3. Verification

| Check | Result |
|---|---|
| `./gradlew :app:compileFreeDebugKotlin` | ✅ BUILD SUCCESSFUL |
| `./gradlew testFreeDebugUnitTest` | ✅ 195 tests, 0 failures, 0 errors |
| `./gradlew :app:lintFreeDebug` | ✅ no new issues (94 filtered by baseline) |
| `./gradlew :app:lintPlayDebug` | ✅ no new issues (94 filtered by baseline) |
| `./gradlew :app:assembleFreeBenchmark :benchmark:assemble` | ✅ BUILD SUCCESSFUL |
| `./gradlew :app:assembleFreeRelease` (no creds) | ✅ builds, warns UNSIGNED, artifact named `-unsigned` |
| `./gradlew :app:assembleFreeRelease` (temp keystore) | ✅ signed artifact `app-free-release.apk` |
| `secrets.properties` removed after keystore test | ✅ gitignored, not committed |

New-test breakdown: `CsvManagerTest` 10/10, `BackupSerializerToleranceTest` 13/13,
`BackupSerializerTest` 5/5 (pre-existing, still green).

## 4. Open risks / next step

- **`Boundary`/cancellation debt is untouched.** `boundary {}` is still barely used
  in the data layer, and `CancellationException` is still swallowed at most catch
  sites — notably `SmsAiEnricher.kt:62`, which defeats the documented
  "zero LLM work while any activity is visible" guarantee, and
  `LlamaCppLlm.generate`'s `runBlocking`, which breaks structured cancellation.
  These are the next P1 target.
- **The duplicate-SMS guard in `SmsPipelineProcessor` is still dead code.**
  `insertTransaction` is `@Insert(onConflict = REPLACE)` against a unique index,
  so SQLite resolves the conflict by delete+insert and never raises a constraint
  violation; the branch can never run, and the test that "covers" it fabricates
  the exception string. The real failure mode is a transaction silently changing
  primary key on reprocess.
- **`Outcome.RETRY` still has no backoff** — the failed row is re-selected
  immediately by `getPending(ORDER BY receivedAt DESC LIMIT 1)`.
- **No DAO/migration/repository tests.** `exportSchema = false` still prevents
  writing `MigrationTestHelper` tests. Enabling it is the prerequisite.
- **The screenshot drift gate still only covers a component gallery**, not real
  screens, and its baselines were made on macOS while CI runs ubuntu.
- Lint baseline hides 94 real issues (missing translations, `DefaultLocale`,
  `MissingPermission`, `StaticFieldLeak`). Worth a cleanup pass now that the gate
  exists.
