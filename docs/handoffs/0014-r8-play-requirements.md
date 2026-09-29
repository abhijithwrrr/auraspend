# 0014 — R8 and the Play Store requirements

## 1. Why this session exists

Asked whether R8 was "fully done as per new Play Store requirements". The honest
answer needed measuring rather than reading configuration, so this session measured.

The trigger is a requirement Google announced on **2026-08-26**, effective
**February 2027**: Play will enforce minimum **25% optimization, 25% obfuscation and
25% shrinking** on any app bundle containing more than **10 MB of uncompressed DEX**.
A second, older one — **16 KB page-size** support, mandatory for API 35+ targets since
2025-11-01 with hard enforcement from 2027-02-01 — is already in force.

Neither had ever been measured in this repo. The analyzer report on disk was stale
(source post-dated it) and had never been read.

## 2. The headline result

| Metric | Measured | Requirement | Verdict |
|---|---|---|---|
| Obfuscation | **99.05 %** (938/99092 pinned) | ≥25 % | pass |
| Optimization | **98.74 %** (1250/99092 pinned) | ≥25 % | pass |
| Shrinking | **99.06 %** (929/99092 pinned) | ≥25 % | pass |
| Bundle DEX | **3.68 MB** uncompressed | >10 MB for the floor to bind | **does not bind** |
| 16 KB alignment | `Verification successful` | required | pass |
| JNI classes | un-renamed | must not rename | pass |

**The requirement does not apply to AuraSpend.** R8 takes the debug build's 71.5 MB of
DEX down to 3.68 MB, well under the floor. Google's DEX-size floor exists for exactly
the apps that do not lean on DEX. The app would clear the 25 % thresholds anyway, but
that is not why it is safe.

This is a lucky consequence of decisions made for other reasons: the 22 MB encoder is
downloaded at runtime rather than bundled, and a Compose app simply does not produce
much DEX.

**Phase 5 (tuning) was therefore not needed.** Nothing was pulled.

## 3. What shipped

**`tools/verify_r8_release.sh` + `tools/r8_scores.py`** — a device-free, repeatable
check of all four questions. Exits 0 today.

Getting the numbers out of the analyzer was itself a problem. The report is a
JavaScript app over a base64-embedded protobuf, so the scores are visible only by
rendering it in a browser, and no browser was attached. `r8_scores.py` decodes the
protobuf and applies the same formula as the report's own JavaScript
(`score = 100 − pinned/live × 100`). The first attempt at this returned a nonsense
100.00 %; it was caught by cross-checking against an independent read of the same file
(the top rules by items kept) and re-derived. The shipped numbers reconcile with both.

**`app/src/androidTest/.../R8ReleaseSmokeTest.kt`** — device tests for the JNI and Room
paths. **Written, never executed** (see §6).

**`app/proguard-rules.pro`** — removed `-keep class com.arm.aichat.** { *; }`, a dead
rule for llama.cpp deleted in handoff 0013. It matched nothing but was the only
native-runtime rule in the file, which made the JNI story look handled locally.

**`gradle/libs.versions.toml`** — corrected an unverified claim (see §5).

**`.gitignore`** — two real defects, both found while staging this work (§6).

**`docs/superpowers/plans/2026-09-29-r8-play-requirements.md`** — the plan, with
measured results filled in.

## 4. The JNI finding, and why it went the other way

ONNX Runtime's AAR ships **no consumer ProGuard rules**, and
`libonnxruntime4j_jni.so` exports **zero** `Java_com_microsoft_onnxruntime_*` symbols —
it registers natives dynamically in `JNI_OnLoad` via `RegisterNatives`, resolving
classes by name. That reads like a guaranteed rename-breaking crash in the release
build, and it was the main risk assumed at the start of this session.

It is not one. AGP's default `proguard-android-optimize.txt` carries:

```
-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }
```

which keeps any class declaring a native method, names included, so `FindClass` at
`JNI_OnLoad` still resolves. Confirmed three ways: the rule is in the app's merged
`configuration.txt`; R8's keep-radius analysis shows it pinning 40 items with
`DONT_OBFUSCATE + DONT_OPTIMIZE`; and `ai.onnxruntime.OrtEnvironment`, `.OrtSession` and
`.OnnxTensor` are all present and **un-renamed** in the release `mapping.txt`.

The lesson matches the one from handoff 0013: **score the system, never the model.** The
plausible-sounding failure was real about the mechanism and wrong about the outcome. The
replaceable comment warns **against** adding a broad
`-keep class ai.onnxruntime.** { *; }`, which would suppress the obfuscation score for
nothing.

## 5. Corrections to the record

**The 16 KB claim about ONNX 1.20 was false.** `libs.versions.toml` said 1.20 was
"rejected outright because its arm64 .so is not 16 KB aligned". Measured: 1.20.0 and
1.22.0 **both** have `PT_LOAD align = 2**14` (16384) on arm64-v8a, and both deflate
their `.so` inside the AAR. The pin stays — re-justified as "1.22 is the version the
golden corpus was measured against", with an instruction to re-derive before changing
it. The old note conflated ELF segment alignment with APK zip alignment.

**I raised a false alarm mid-session and should say so plainly.** Searching the release
DEX for `com/microsoft/onnxruntime` returned zero, which looked like "the release build
ships 18 MB of native library and none of the Java API that loads it". The package is
`ai.onnxruntime`; the imports in `EmbeddingClassifier.kt` state this. The classes are
present and un-renamed. The false alarm was mine, from a wrong package name, and it
nearly became a reported defect.

## 6. Tooling problems found

**`.gitignore` line 20 was inert.** It read:

```
.zcode/          # host venvs, downloaded models, scratch build dirs
```

A trailing `# …` is **not** a comment in gitignore — only a `#` at the start of a line
is. The pattern was the literal string `.zcode/          # host venvs…`, which can never
match. `.zcode/` (a venv plus downloaded ONNX weights) was therefore stageable, and
`git add -A` would have taken it. Fixed, with a comment explaining the trap.

**`/app/play/` was not ignored.** `.gitignore` covered `build/` and `/app/release/` but
not the other variant output dirs, leaving an **79 MB `app-play-release.apk`** and its
baseline profiles stageable. `*.apk`/`*.aab` covered the artifact but not
`output-metadata.json` or the `.dm` profiles. Now ignored alongside `/app/free/`. The
APK was unsigned, so no credential leak — verified by checking for signature blocks
before drawing that conclusion.

**`testBuildType = "benchmark"` was the wrong tool, tried and reverted.** To point
instrumentation at the minified build I set `testBuildType`, expecting it to affect only
androidTest. It renames the **unit** test task too: `testFreeDebugUnitTest` disappeared
and `testFreeBenchmarkUnitTest` appeared, which would run all 253 unit tests — Robolectric
screenshot baselines included — against a shrunk build, and would break the documented
gate in `AGENTS.md` and the CI workflow. Caught by the build gate, reverted, and the
reason recorded in the test's KDoc so the next person does not repeat it.

## 7. Verification

```
./tools/verify_r8_release.sh free                 # all checks passed, exit 0
./gradlew :app:compileFreeDebugKotlin testFreeDebugUnitTest :app:lintFreeDebug
                                                # BUILD SUCCESSFUL
```

The R8 numbers come from a real `bundleFreeRelease` + `assembleFreeRelease` on this
machine, not from the stale report. `zipalign -c -P 16 -v 4` reports
`Verification successful` on `app-free-release-unsigned.apk`.

`R8ReleaseSmokeTest` has **never been executed**. Instrumentation hung with no output on
the attached device (moto g40, Android 12) across two attempts — the app process launched
but the test runner emitted zero log lines, `adb logcat` showed only unrelated device
activity, and `am instrument` hung identically. The device was in an active
screen-sharing session, so this looks environmental rather than a defect in the test, but
that is an inference, not a measurement. **Treat it as unverified code; run it on a
clean emulator before relying on it.**

Its assertion design is the part worth keeping: production (`LocalLlmProvider.build`)
catches `UnsatisfiedLinkError` and every `Throwable` and falls back to the regex
classifier, and `EmbeddingClassifier` routes each init step through `boundaryOrNull`. A
total JNI failure therefore degrades **silently** — the app looks fine and has quietly
lost its AI path. A "nothing threw" test would pass. Every assertion checks that work
actually happened.

## 8. Open

- **`verify_r8_release.sh` is manual.** Per instruction, CI gates were not part of this
  pass. Nothing prevents a future release from regressing the DEX size, the 16 KB
  alignment, or the JNI naming. Reinstating is a small `ci.yml` change.
- **The encoder has still never run on a device.** Handoff 0013's open item stands. The
  new smoke test is the vehicle for it, once it can be run somewhere.
- **16 KB behaviour is verified statically only** (`zipalign` + ELF alignment). Runtime
  behaviour on a 16 KB page-size device is untested; that needs Android 15+ hardware.
- **The F-Droid signed-release check still needs the keystore** (handoff 0013, open).

## 9. Memory

- `memory/decisions.md` D11 — the flavor/Play requirements position, and the
  measurement that settled it.
