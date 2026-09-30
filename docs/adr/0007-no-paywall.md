# ADR 0007 — No paywall in the open-source app

**Status:** Accepted · 2026-09 · licence note: written under Apache-2.0; see ADR 0009 for the AGPL-3.0 relicensing

## Context

The project had a `free`/`play` flavor split with an IAP stub for "premium"
themes, analytics and Drive backup. The gating points were orphaned code
(`PremiumGate`, `PremiumUpgradeScreen`) and the `free` build unlocked everything
anyway. A paywall in an Apache-2.0 app undermines trust and contributor
goodwill.

## Decision

Remove premium gating entirely: no paywall, no billing permission, no orphaned
premium screens. All features ship in both flavors.

The `free`/`play` flavors remain for **distribution** only (Play signing and
listing differences), not for feature gating. Donations can be linked from the
repository, never enforced in-app.

## Consequences

- Users and F-Droid get the complete app; there is no "pro" build to maintain.
- README, code and store listing all state the same thing: every feature free.
- If monetization is ever revisited, it must respect Apache-2.0 and be proposed
  as a superseding ADR.
