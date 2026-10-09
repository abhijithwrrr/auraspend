# ADR 0010 — `dev`/`prod` flavors: two environments, four variants

**Status:** Accepted · 2026-10-09 · **Supersedes ADR 0008's naming; keeps its substance**

## Context

The `free`/`play` split (ADR 0008) was named for distribution channels, but the
channel stopped explaining the builds:

- Both flavors were feature-identical and both source sets held only an empty
  manifest, so "free" and "play" described nothing a developer could act on.
- The APK published on GitHub Releases and the AAB uploaded to Play are now
  signed by the same project and built by the same workflow; the meaningful
  question became "which build do I install while developing?".
- A third build type (`benchmark`, release-like with debug signing) existed
  only so Macrobenchmark could install a profileable app without the release
  keystore. One module, six variants, and :benchmark duplicated the flavor
  dimension to line up.

The same developer's other apps all use `dev`/`prod` with a
`.dev` application id for the co-installable development build.

## Decision

**Exactly four variants: `devDebug`, `devRelease`, `prodDebug`, `prodRelease`.**

- **`prod`** is the only distributed build: `com.awbuilds.auraspend`, no
  version-name suffix. Play receives `bundleProdRelease`; self-installers get
  `assembleProdRelease`.
- **`dev`** is a development environment: `applicationIdSuffix ".dev"`,
  `versionNameSuffix "-dev"`, and an "AuraSpend Dev" label (EN + HI) so it is
  unmistakable on a device. It is **feature-identical** — no debug menus, no
  test backends; a feature present only in dev is exactly the kind of
  flavor-only capability ADR 0008 forbids, just in the other direction.
- **No `benchmark` build type in `:app`.** The macrobenchmark module declares
  `matchingFallbacks ["release"]` and runs against `prodRelease` (profileable,
  not debuggable). Cost: running benchmarks locally now needs the release
  keystore in `secrets.properties`. Benefit: one less variant, and benchmarks
  measure the exact artifact users run.
- **Drive sign-in in dev** requires its own OAuth client, because Google
  matches package id + signing certificate. That is Google Cloud
  configuration, not code — and it is documented in
  `secrets.properties.example`.

## Consequences

- CI (`.github/workflows/`) tests `testProdDebugUnitTest`, lints both debug
  variants, and compiles `:app:assembleProdRelease :benchmark:assemble`.
- The release workflow builds only the prod flavor and names the assets
  `AuraSpend-V<version>.Alpha.{aab,apk}`.
- ADR 0008's guarantees are unchanged: no analytics, no ads, no paywall,
  feature-identical builds, and no Play-only capability without superseding
  this ADR (and 0007). The rename changes naming and the variant count, not
  what ships or what it promises.
