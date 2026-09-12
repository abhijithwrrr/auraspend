# Decisions

Durable decisions with context. Latest first.

## D1 — Aurora purple replaces Cashew blue (2026-09-12)
The app icon and landing page already use purple (#5E3A8B / #6750A4 / #C4A6E6);
the in-app theme was Cashew blue (#1B447A). The rebuild adopts the existing
brand palette as the source of truth so app, icon and site finally match.
Dynamic color stays available but brand-first.

## D2 — Four tabs + center FAB; Settings behind header avatar (2026-09-12)
Bottom nav becomes Home / Activity / Plan / Insights with the FAB in the
middle. Settings moves behind the header avatar. Rationale: Budgets,
Subscriptions and Goals were buried in Settings or dead-end routes; the new
`Plan` hub makes them first-class while keeping four thumb-reachable tabs.

## D3 — material3 1.4.0 via Compose BOM 2026.09.00 (2026-09-12)
M3 1.4.0 is the newest stable and includes M3 Expressive APIs. BOM pins
Compose UI 1.12.1 and Material3 1.4.0 together. Alpha 1.5.x intentionally not
used.

## D4 — No paywall anywhere (2026-09-12)
PremiumGate, PremiumUpgradeScreen and the BillingManager stubs were unused or
stubbed; the free flavor unlocked everything already. For an Apache-2.0 project
the paywall undermined trust. Removed. `free`/`play` flavors remain for
distribution differences.

## D5 — Try/catch at every boundary is a hard rule (2026-09-12)
Every IO/platform call must catch exceptions, log via `AuraLog`, and degrade to
a typed fallback. `CancellationException` is always rethrown. Helpers live in
`core/AuraLog.kt`. See AGENTS.md.

## D6 — Phased rebuild with handoffs (2026-09-12)
The UI revamp is one phase per branch (P0–P6). Each phase ends with a handoff
doc and starts only after the previous gate passes. Agent memory
(`CLAUDE.md` + `memory/`) carries durable context between sessions.
