# Tech Stack & Environment

## Build
| Item | Version |
|------|---------|
| AGP | 9.3.1 (built-in Kotlin 2.2.10) |
| Gradle | 9.5.0 (config cache + parallel + caching on) |
| KSP | 2.2.10-2.0.2 |
| Compose BOM | 2026.09.00 → Compose UI 1.12.1, Material 3 1.4.0 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 30 |
| Room | 2.8.5 |
| WorkManager | 2.10.0 |
| Navigation Compose | 2.10.1 |
| LLM runtime | Vendored llama.cpp (`:llama`), Qwen2.5-0.5B Q4_K_M (~400 MB, downloaded at runtime) |

## Commands
```bash
./gradlew :app:compileFreeDebugKotlin   # fast compile check (use this constantly)
./gradlew testFreeDebugUnitTest         # unit tests (required before handoff)
./gradlew assembleFreeDebug             # APK; triggers native llama build (NDK 29 + CMake >= 3.31.6)
```

## Dependency management
`gradle/libs.versions.toml` is the single source of truth. Do not add versions
inline in build files. Unused catalog entries (`lottie`, `paging`) are staged
for the phase that introduces them.

## Key source locations
| Path | Contents |
|------|---------|
| `app/src/main/java/com/awbuilds/auraspend/ui/designsystem/` | Aurora components + tokens |
| `app/src/main/java/com/awbuilds/auraspend/ui/theme/` | Palette, typography, shapes, theme |
| `app/src/main/java/com/awbuilds/auraspend/ui/navigation/NavGraph.kt` | Current navigation (rebuilt in P1) |
| `app/src/main/java/com/awbuilds/auraspend/data/classification/` | SMS parsing + classification |
| `app/src/main/java/com/awbuilds/auraspend/core/AuraLog.kt` | Logging + error boundaries |
| `docs/design/aurora.md` | Design language spec |
| `docs/handoffs/` | Session handoffs |
