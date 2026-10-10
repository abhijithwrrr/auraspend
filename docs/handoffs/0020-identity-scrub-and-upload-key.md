# 0020 — identity scrub, org move, and a clean upload key

**Status:** complete · **Repo now:** `github.com/auraspend/auraspend`

## 1. Why

Before open-sourcing, the repository still carried the maintainer's real name,
personal email and GitHub handle, Play Console account identifiers, a private
tester mailing list, local machine paths — and, most seriously, **real
production bank SMS** used as golden test data: card digits, account tails,
balances, exact timestamps, an employer name in an NEFT credit, a person's
name, and a merchant branch. All of it existed in the working tree, in earlier
commits, and in the shipped signing certificate. This handoff records the scrub.

## 2. What changed

### Working tree (39 files across code, tests, docs, website)

- Every personal identifier replaced: real name → **AuraSpend**, personal email
  → `auraspend@users.noreply.github.com`, GitHub handle → the `auraspend` org,
  Play Console IDs → `REDACTED`, tester group → "a private Google Group
  (address withheld)", `~/RealWorldProjects` → `~/projects`. In-app copyright
  strings (EN + HI) and README now read "Copyright 2026 AuraSpend".
- **Golden corpus + every test/doc/eval that quoted it** moved from real
  production SMS to format-identical synthetic data: card `XX4821`, account
  tails `XXXX6021` / `XXXX1134`, merchants `Tulip`, `TULIP MART`,
  `AMBER LEAF`, `SUNRISE DINER`, employer `ACME SOLUTIONS LIMITED`, dates
  `05-07-26 …`, balances/amounts re-rolled. Masker-test phone/UTR/folio/hash
  identifiers rotated too (both sides of each assertion, so tests stay valid).
  Provenance notes now say "Realistic bank SMS" instead of claiming real
  production messages.
- `.gitignore` additions: `.claude/`, `.zcodeignore`, `/.kotlin/`.

### Git history (`git filter-repo`, run twice for residual model-echoed tokens)

- Author **and** committer on every commit → `AuraSpend
  <auraspend@users.noreply.github.com>`; bot commits keep their bot identity.
- The same string map applied to **all blobs and commit messages**, plus
  `.github/private/`, `bank_sms_dataset.json`, `synthetic_v3_train.jsonl`,
  `app/src/main/assets/model_v2.onnx` and `model_version.txt` removed from
  every commit.
- Deleted local `refs/codex/*`, `refs/cline/*`, `refs/kanban/*` checkpoint
  refs (pre-scrub snapshots), expired reflogs, pruned: **pack 696 MB → 19 MB**.

### GitHub

- Repo lives at `github.com/auraspend/auraspend`; all 13 branches force-pushed.
  `main` was repointed to the current state (the old `main` line was stale and
  its sole unique commit was a dependabot bump already superseded).
- Deleted the two stale `v0.1.0` draft releases (their notes credited the old
  handle).
- All four signing secrets rotated.
- `main`'s branch protection (1 review, no force-push) was snapshotted, lifted
  for the push, and restored exactly.
- `release-drafter`'s `change-template` no longer interpolates `@$AUTHOR`, so
  future release notes cannot leak a personal handle.
- Dependabot closed its 8 open PRs when the rewritten base branches landed; it
  re-opens them on its own schedule (do not hand-recreate).

### Upload key

- New RSA-4096 keystore, certificate `CN=AuraSpend, OU=Mobile, O=AuraSpend`.
  The previous certificate carried the maintainer's name **and home
  city/state**, and that name shipped inside every signed artifact.
- Old key archived outside the repo; `secrets.properties` rewritten with a
  relative path; passwords stored in the Keychain item "AuraSpend Upload Key".
- `:app:bundleProdRelease` + `:app:assembleProdRelease` rebuilt and verified:
  AAB (`keytool -printcert -jarfile`) and APK (`apksigner --print-certs`) both
  report the new certificate.

## 3. Deliberately kept (brand, not person)

`awbuilds` survives in the package id (`com.awbuilds.auraspend`), the Play
developer account name, the support mailbox and the CLA's copyright holder.
Renaming the package id would orphan the already-staged Play listing (new app,
all declarations and the 178-country closed test redone from zero). Treat a
rename as a conscious relaunch, not a cleanup.

## 4. Pending — user actions

1. **Play Console → App integrity → Reset upload key**, upload the new
   certificate PEM, then replace the AAB in the closed-testing draft with the
   rebuilt one (`~/RealWorldProjects/auraspend-artifacts-2026-10-10/`) before
   sending for review.
2. **GitHub Support**: ask for unreachable-object cleanup so the old commits
   cached on PR pages #2, #5, #7, #14, #26, #27 stop rendering (their author
   identity is still the old personal one). The request must not be committed
   to the repo — it names the old identity; see the session report.
3. Delete the local safety mirror
   `~/RealWorldProjects/auraspend-backup-pre-scrub.git` (contains the old
   history) once satisfied that nothing needs recovering.

## 5. Verification evidence

- `./gradlew testProdDebugUnitTest` — **274 tests, 0 failures** after the scrub.
- Token sweep across the working tree and `git log -p --all` for the former
  identity strings — **zero hits**.
- `git log --all --format='%an <%ae> | %cn <%ce>'` — only `AuraSpend …`,
  `GitHub <noreply@github.com>` and `dependabot[bot]`.
- `git count-objects -vH` — `size-pack: 19.07 MiB`, 0 garbage.
- New signatures: AAB + APK `CN=AuraSpend, OU=Mobile, O=AuraSpend`
  (SHA-256 `3e7fed0677f3…3549`).
- GitHub Pages (main `/docs`), homepage and description intact; protection
  restored (`allow_force_pushes=false`, 1 required review).

## 6. Next step

Resume the 0.1.1 launch checklist from handoff 0019 — reset the Play upload
key, re-upload the rebuilt AAB, then the usual "Preview and confirm" → send
for review → 12 testers × 14 days.
