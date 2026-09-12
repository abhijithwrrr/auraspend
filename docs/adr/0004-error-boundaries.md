# ADR 0004 — Try/catch boundaries with AuraLog

**Status:** Accepted · 2026-09

## Context

The app talks to banks (SMS parsing), a local LLM, Google Drive, CSV files,
WorkManager and SQLite. Failures in any of these used to surface as crashes or
silent breakage; a bad LLM download could take down Smart Add.

## Decision

Every IO/platform boundary **catches its exceptions, logs them through
`AuraLog`, and degrades to a typed fallback**:

```kotlin
inline fun <T> boundary(tag: String, fallback: T, block: () -> T): T =
    try { block() }
    catch (e: CancellationException) { throw e }
    catch (e: Exception) { AuraLog.e(tag, "...", e); fallback }
```

- `CancellationException` is always rethrown so structured concurrency works.
- No empty catches; every catch either maps to state (error UI) or a documented
  fallback (regex classifier when the model is unavailable).
- No `!!` in new code; `requireNotNull` with a real error path instead.
- The rules are enforced in `AGENTS.md` and reviewed with every PR.

## Consequences

- A broken bank parser or half-downloaded model degrades gracefully instead of
  crashing; users always have a regex fallback.
- Errors are visible in logcat under `AuraSpend/<tag>` for bug reports.
- Contributors must think about failure at every boundary — that is the point.
