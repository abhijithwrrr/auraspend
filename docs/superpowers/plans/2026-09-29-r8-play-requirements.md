# R8 / Play Store Requirements Plan

**Goal:** Prove AuraSpend meets Google's new Play technical-quality requirements, and
close the release-only correctness gap that R8 opens.

**Why now:** Google announced on **2026-08-26** that from **February 2027** Play enforces
minimum **optimization 25% / obfuscation 25% / shrinking 25%**, for apps whose bundle
contains **>10 MB uncompressed DEX**. Separately, **16 KB page-size** support became
mandatory for API 35+ targets on 2025-11-01, with a hard enforcement date of
**2027-02-01**. Both are now inside the project's planning horizon, and neither has ever
been measured here.

**Tech Stack:** AGP 9.3.1 (R8 full mode), Gradle, R8 Configuration Analyzer, `zipalign`,
Robolectric/JUnit4, Android instrumentation tests.

---

## The governing lesson

From handoff 0013: **score the system, never the model.** It applies here verbatim. The
question is not "is R8 configured correctly" — it is "does the *shrunk, obfuscated,
packaged* artifact still work, and does it score above 25%". Configuration is a proxy.
The score and the smoke test are the measurement. Phase 1 therefore changes nothing; it
only measures, and Phases 2/3/5 are driven by what it finds.

---

## What is already correct (verified, not assumed)

| Check | Result |
|---|---|
| R8 full mode | On. No `android.enableR8.fullMode=false` in `gradle.properties`; full mode is the AGP 8.0+ default |
| `isMinifyEnabled` / `isShrinkResources` | Both `true` in the `release` build type |
| Default proguard file | `proguard-android-optimize.txt` — correct for AGP 9, which dropped `proguard-android.txt` support |
| Optimized resource shrinking | Automatic in AGP 9 when `isShrinkResources = true` |
| Analyzer availability | Needs AGP ≥ 9.3.0; project is on 9.3.1 |
| 16 KB ELF alignment | ONNX Runtime 1.22.0 `.so` measure `PT_LOAD align = 2**14` (16384) — ALIGNED |
| Gson full-mode rules | Resolves to 2.11.0, which ships its own required keep rules |
| Stack-trace readability | `-keepattributes SourceFile,LineNumberTable` + `-renamesourcefileattribute` |
| JNI (`RegisterNatives`) | **Not** a risk. AGP's default file carries `-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }`, confirmed present in the merged `configuration.txt` |

The JNI row is worth stating explicitly because the naive read is wrong. ONNX Runtime's
AAR ships **no consumer ProGuard rules**, and `libonnxruntime4j_jni.so` exports **zero**
`Java_com_microsoft_onnxruntime_*` symbols — it registers natives dynamically at
`JNI_OnLoad`, which *looks* like a guaranteed rename-breaking crash. It is not, because
AGP's default rule keeps any class declaring a `native` method, names included. Adding a
broad `-keep class com.microsoft.onnxruntime.** { *; }` would be actively harmful — it
would suppress the obfuscation score for no benefit.

---

## Phase 1 — Measure (no code changes) — **DONE**

- [x] Build `bundleFreeRelease` — Play takes a bundle, and **no AAB had ever been produced
      in this repo**. CI only ran `assembleFreeRelease` (an APK).
- [x] Read the R8 Configuration Analyzer scores for the current build. The on-disk report
      was **stale** (source post-dated it), so a fresh one was generated with
      `:app:analyzeFreeReleaseR8Config`.
- [x] Measure release uncompressed DEX from the `.aab`.
- [x] Run `zipalign -c -P 16 -v 4` against the release APK.

**Measured, 2026-09-29, freeRelease:**

| Metric | Value | Requirement | Verdict |
|---|---|---|---|
| Obfuscation | **99.05 %** (938/99092 pinned) | ≥25 % | pass |
| Optimization | **98.74 %** (1250/99092 pinned) | ≥25 % | pass |
| Shrinking | **99.06 %** (929/99092 pinned) | ≥25 % | pass |
| Bundle DEX | **3.68 MB** uncompressed | >10 MB for the floor to bind | **floor does not apply** |
| 16 KB alignment | `Verification successful` | required for API 35+ | pass |

Live program 99,092 members; 107 keep rules, no global `dont*`.

**The headline finding is the 3.68 MB.** R8 takes the debug build's 71.5 MB of DEX down
to 3.68 MB, comfortably under the 10 MB floor at which Play's 25 % rule starts to
apply. The requirement therefore **does not bind on AuraSpend** — Google's floor exists
precisely for apps that do not lean on DEX. The scores are reported anyway because they
are cheap to produce and would catch a regression long before it mattered.

**Phase 5 is therefore not needed.** Nothing to tune. Worth recording that this is a
lucky consequence of a decision made for a different reason: downloading the 22 MB model
at runtime instead of bundling it, and being a small Compose app, keeps DEX tiny.

---

## Phase 2 — Correct the record — **DONE**

- [x] Removed `-keep class com.arm.aichat.** { *; }` from `app/proguard-rules.pro`, and
      replaced it with the reasoning: JNI is covered by AGP's default
      `-keepclasseswithmembernames ... native <methods>` rule, verified both in the merged
      `configuration.txt` and by R8's keep-radius analysis (it pins 40 items). The comment
      explicitly warns **against** adding a broad
      `-keep class ai.onnxruntime.** { *; }`, which would suppress the obfuscation score
      for nothing.
- [x] Corrected the `libs.versions.toml` claim that ONNX 1.20 was "rejected outright" for
      failing 16 KB alignment. Measured: 1.20.0 and 1.22.0 **both** have PT_LOAD align
      `2**14` on arm64-v8a, and both deflate their `.so` in the AAR. The pin stays,
      re-justified as "1.22 is the version the corpus was measured against".

---

## Phase 3 — Release verification — **DONE, with a substitution**

R8 defects are **release-only by definition**: debug assemble and debug tests both pass
while a shrunk, obfuscated build is broken. Nothing ran the minified app.

### 3a. `tools/verify_r8_release.sh` + `tools/r8_scores.py` — **shipped, passing**

A device-free, repeatable check of all four questions. `./tools/verify_r8_release.sh free`
exits 0. It reports the three scores (decoding the analyzer's embedded protobuf with the
same formula its own JavaScript uses, because the report is otherwise a browser-only
JavaScript app), the DEX size against the 10 MB floor, `zipalign -P 16`, and the
un-renamed status of the JNI-resolved classes.

This **subsumes** the most valuable part of a smoke test. The realistic R8 failure here
is a rename of a class that `JNI_OnLoad` resolves by name, and the mapping file answers
that deterministically and instantly — stronger evidence than "the app launched and
nothing obvious broke".

### 3b. `R8ReleaseSmokeTest` (instrumentation) — **written, NOT executed**

`app/src/androidTest/java/.../R8ReleaseSmokeTest.kt`, run with
`:app:connectedFreeDebugAndroidTest`. It covers the JNI and Room paths on a real device,
which Robolectric cannot do.

**It deliberately does not target the minified build.** Doing so needs
`testBuildType = "benchmark"`, and that flag renames the *unit* test task as well:
`testFreeDebugUnitTest` is replaced by `testFreeBenchmarkUnitTest`, which would run all
253 unit tests — screenshot baselines included — against a shrunk build, and would break
the documented gate in `AGENTS.md` and the CI workflow. Tried, measured, reverted.

**It has never been run.** Instrumentation hung with no output on the attached device
(moto g40, Android 12) across two attempts: the app process launched but the test runner
emitted zero log output, `adb logcat` showed only unrelated device activity, and
`am instrument` hung identically. This looks environmental — the device was in an active
screen-sharing session — not a defect in the test, but **it is unverified code and
should be treated as such.** Run it on a clean emulator before relying on it.

Note the assertion design, which is the point of the test: production
(`LocalLlmProvider.build`) catches `UnsatisfiedLinkError` and every `Throwable` and
falls back to the regex classifier, and `EmbeddingClassifier` routes each init step
through `boundaryOrNull`. So a total JNI failure degrades **silently** — the app looks
fine and has quietly lost its AI path. A "nothing threw" test would pass. Every
assertion checks that work actually happened.

---

## Phase 5 — Only if a score is below 25% — **NOT NEEDED**

All three scores are ~99%. No tuning, and no lever pulled. `material.icons.extended`
remains a theoretical DEX contributor but there is nothing to gain by touching it.


---

## Phase 4 — CI gates — **NOT IN SCOPE (deferred by request)**

Deliberately excluded from this pass. Consequence, stated plainly: the zipalign check,
the AAB build, and the analyzer report stay **manual**. Nothing prevents a future release
from regressing these silently. Reinstating them is a small change to
`.github/workflows/ci.yml` whenever that is wanted.

---

## Phase 5 — Only if a score is below 25%

- [ ] Tune using the R8 Configuration Analyzer's per-rule attribution rather than guesswork.
- [ ] First lever if shrinking is short: `androidx.compose.material.icons.extended` is a
      large DEX contributor. R8 strips unused icons, so this may already be a non-issue —
      measure before touching it.
- [ ] Never satisfy the threshold with a broad `-keep`. The guidance is explicit that
      `-keep **` or `-keep com.myapp.**` pays R8's build cost for none of the benefit,
      and it would also fail the metric it was meant to help.

---

## Risks

- **The smoke test needs a device.** One is attached (moto g40, Android 12, arm64). It
  validates R8 correctness, not 16 KB page-size behaviour — that needs an Android 15+
  16 KB device, and is covered statically by `zipalign` only.
- **Scoring is against the bundle, not the APK.** Local `assembleFreeRelease` output can
  differ slightly from what Play derives from the `.aab`. The analyzer measures the APK
  graph; treat a near-threshold result as unresolved until Play reports it.
- **`warningsAsErrors = true`** in lint means a lint change here fails the build
  deliberately. That is correct behaviour, not an obstacle.
