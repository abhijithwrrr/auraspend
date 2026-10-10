# AuraSpend — Roadmap

> One file. This consolidates the two earlier plans — the original "Feature
> Enrichment, UI Redesign & Open Source Readiness" roadmap and the
> "Comprehensive Improvement Roadmap" — both of which were executed or
> overtaken by the **Aurora rebuild** (2026). The full originals remain in git
> history, and the session-by-session record is in `docs/handoffs/`.

Living documents this file defers to:

- `docs/handoffs/` — what shipped and when (read the latest first)
- `docs/handoffs/0000-master-plan.md` — the phased Aurora plan
- `docs/adr/` — durable decisions; usually more informative than a roadmap
- `docs/evals/README.md` — measured classification numbers
- `docs/design/aurora.md` — the design system

## Shipped

- **OSS foundation** — CONTRIBUTING, SECURITY, issue/PR templates, CLA,
  licence check, ADRs, CHANGELOG, CI + PR checks, tag-driven signed releases
  with Release Drafter notes.
- **Classification** — sender-routed bank parsers, a regex baseline with an
  eval harness (`RegexBaselineEval`), the on-device 22 MB MiniLM encoder
  (SHA-256-verified download), learned classification memory, and the
  `unrecognized_sms` triage table.
- **Core** — budgets, subscriptions, savings goals, insights (charts,
  month-over-month), CSV in/out, and Google Drive backup & restore from
  Settings → Data (handoff 0025).
- **UI** — the Aurora design system, 4 tabs + centre FAB, redesigned
  dashboard/Activity/Plan/Insights/Settings/onboarding, empty/loading/error
  states, EN + Hindi, edge-to-edge, System/Light/Dark/AMOLED themes.
- **Quality** — 250+ unit tests (classification, CSV, backup round-trips,
  design-system guards), lint baseline gate, macrobenchmark module, R8 and
  16 KB checks, native-runtime audit script.

## Deliberately not doing

These were proposed in the old plans and are rejected on purpose — overturn
the record first if you want to revisit:

- **Analytics, crash reporting, feature flags, A/B SDKs** (Firebase or
  otherwise): no telemetry in any build, on-screen promise (ADR 0007/0010,
  AGENTS.md). Offline evals do the A/B job.
- **Cloud sync servers** (API server, Postgres, multi-device sync): the
  project runs no servers; Drive backup is the sync story.
- **Premium / IAP gating**: removed in Phase 0 (ADR 0007).
- **SQLCipher / certificate pinning**: no server to pin to; at-rest risk is
  the device's. Revisit only alongside a cloud feature.
- **SMS sending** (SMS reminders): the app reads SMS; it never sends.

## Open ideas

Unbuilt, uncommitted, not promises — roughly in the order they would pay off:

- **Encrypt the Drive backup file** (password-derived key, standard crypto —
  the file is plain JSON today).
- **Tags** on transactions, with filter chips.
- **Opt-in local notifications**: budget thresholds and bill reminders
  (on-device only).
- **Recurring intelligence**: history-based subscription detection, an audit
  of unused/duplicate subscriptions, annual cost, skip/end refinements.
- **PDF / tax-ready reports**; search date and amount ranges; saved searches.
- **Budgets**: custom reset day, rollover, scenarios.
- **Entry**: split transactions, templates, batch entry, receipt attachments
  with on-device OCR only.
- **Data**: OFX/PDF statement import, multi-account with transfers, currency
  conversion table.
- **Categories**: hierarchy, multi-label, user rules.
- **Surface**: Glance widget(s); forecasting and anomaly insights.
- **Quality**: Room migration tests, coverage measurement, on-device
  accessibility pass.

## Maintenance

When something ships: move it to *Shipped* or delete it in the same PR, and let
the handoff carry the detail. Keep this file short; history belongs in
`docs/handoffs/` and git.
