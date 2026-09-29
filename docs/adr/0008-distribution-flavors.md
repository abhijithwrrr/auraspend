# ADR 0008 — Distribution flavors carry no features, no analytics, no ads

**Status:** Accepted · 2026-09-29 · **Reinforces** ADR 0007 (does not supersede it)

## Context

The question was whether the Play Store build should add analytics, ads and
premium gating that the GitHub/F-Droid build does not. That is a common
open-source business model (GitLab, Nextcloud, VLC all do it), so it deserved a
considered answer rather than a reflex.

Three findings decided it.

**1. The app's core promise is an on-screen claim, not a policy document.**
`onboarding_page2_description` tells every user who installs it:

> Bank SMS is categorised automatically, on-device, by a local AI that never
> uploads your data.

Analytics and ad SDKs both upload. A Play build carrying either would make the
store listing contradict the first-run screen of the same APK, and Play Store
policy requires the listing to disclose data collection. This is not a
policy-page detail — it is the product's differentiating claim, and the data
involved (bank SMS) is among the most sensitive category on a phone.

**2. ADR 0007 already ruled on feature gating.** It is Accepted, and it says:

> The `free`/`play` flavors remain for **distribution** only, not for feature
> gating. Users and F-Droid get the complete app; there is no "pro" build to
> maintain.

Reversing it would require a superseding ADR, and there is still no good reason
to: the accuracy work in handoff 0013 showed the app's value *is* the
classifier, so gating it means the free build is worse at the one job it does.
A user comparing the two would conclude the open-source build is the crippled
one — inverting the usual dynamic where the paid tier feels premium.

**3. Both flavor source sets are empty.** `app/src/free` and `app/src/play`
contain only a manifest and no Kotlin, and there are zero `BuildConfig`
references in the tree. The split has never gated anything, so there is nothing
to unwind — only a convention to state and protect.

## Decision

**Both published builds are feature-identical, and neither carries analytics or
ads.**

- `free` → F-Droid, GitHub self-build. All features. No analytics, no ads.
- `play` → Play Store. All features. No analytics, no ads.
- The flavors exist for **distribution configuration only** — signing, listing
  metadata, and the `com.android.vending` permission if a distribution-specific
  capability is ever added.
- **No feature may live in `src/play` that is absent from `main`.** Only
  *services* and *configuration* may differ. This is the invariant that makes the
  free build incapable of being a degraded build.
- No monetization is implemented. If it is ever added it must be a
  donation/supporter tier that unlocks nothing, consistent with ADR 0007.

## Consequences

- **One codebase, two APKs.** A fix lands once and ships everywhere; the free
  build cannot rot into a stale fork.
- **F-Droid compatibility is preserved.** F-Droid builds from source and accepts
  only FOSS. Keeping `src/free` free of proprietary dependencies is a hard
  requirement, not a preference.
- **The privacy claim stays true in every build.** No build needs a disclosure
  screen for data collection, because none collects.
- **CI keeps four variant tasks** (two assembles, two lints) for two identical
  apps. Accepted: the flavors are the seam a future distribution difference
  would use, and the cost is one extra assemble already in the matrix.
- **No crash insight beyond what users volunteer.** Accepted deliberately. If
  that changes, the route is a self-hosted PostHog in *all* builds, or an
  opt-in toggle — not a silent Play-only SDK.

## How this is protected

- `AGENTS.md` records the invariant, so an agent adding a `src/play` code path
  sees why it is wrong before doing it.
- The `play` manifest is the single place a distribution difference may live;
  it is currently empty and that is correct.
- `CONTRIBUTING.md` and `README.md` state the same rule for humans.

## Rejected alternatives

| Option | Why not |
|---|---|
| Play-only analytics | Contradicts the on-screen privacy claim. Would need opt-in, a disclosure screen, and an explicit rule that transaction content is never in scope. |
| Play-only ads | Ads in a bank-SMS app cost more trust than they return. |
| Play-only premium tier | Reverses ADR 0007; makes the open-source build measurably worse at its one job. |
| Collapse to a single flavor | The flavors are the seam a real distribution difference would use. Keeping them empty is cheaper than adding them later. |
