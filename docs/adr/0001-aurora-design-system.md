# ADR 0001 — Adopt the Aurora design system

**Status:** Accepted · 2026-09 · licence note: written under Apache-2.0; see ADR 0009 for the AGPL-3.0 relicensing

## Context

The pre-rebuild UI mixed Material 3 defaults with a Cashew-style card look:
hard-coded paddings (13dp), per-screen font sizes, drop shadows on every
surface, emoji used as chrome, and a palette that did not match the app icon or
landing page. New screens were drifting further apart.

## Decision

All UI is built from a single design system:

- **Tokens** in `ui/theme` and `ui/designsystem/AuraTokens.kt`: colours
  (Aurora purple/lavender/teal), Plus Jakarta Sans typography with tabular
  figures, 4dp spacing scale (`AuraSpacing`, gutter 20dp), shape scale,
  motion specs (`AuraMotion`) and brand gradients.
- **Components** in `ui/designsystem/`: `AuraCard`, `AnimatedMoney`,
  `CategoryAvatar`, `AuraSegmentedControl`, animated charts, `AuraSkeleton`,
  `AuraEmptyState`, `Chrome` (section header, soft shadow, hide-amount toggle).
- **Rules** (enforced by review, documented in `AGENTS.md`): no raw hex
  colours, no one-off font sizes, no shadows on content, no new emoji in
  chrome, and a per-screen Definition of Done in `docs/design/aurora.md`.

The Phase-0 compatibility shim (`CashewComponents`) was retired once every
screen migrated.

## Consequences

- Consistent look and motion across light, dark and AMOLED themes.
- A deliberate visual change now happens once, in one place.
- Contributors must learn the tokens; the design spec and DoD keep that cheap.
- The system is intentionally dependency-free (no third-party UI kit), so the
  Apache-2.0 build stays clean.
