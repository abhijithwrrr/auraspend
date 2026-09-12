# ADR 0006 — String resources and Hindi as first locale

**Status:** Accepted · 2026-09

## Context

All UI copy was hard-coded in Kotlin. The primary audience is Indian users;
Hindi support is table stakes for adoption and for credible i18n contributions.

## Decision

- Every user-facing string lives in `res/values/strings.xml` with a
  screen-prefixed key (`home_total_balance`, `settings_theme_dark`).
- Counts use `<plurals>`; formatted strings use typed `%1$s`/`%1$d` and
  `stringResource(id, args)`.
- **Hindi (`values-hi`)** ships as the first translation, community-maintained.
  Missing keys fall back to English per-resource, so partial translations are
  acceptable and PRs can fill gaps.
- Brand/format tokens (AuraSpend, SMS, CSV, AMOLED, ₹, placeholders) stay
  untranslated.

## Consequences

- Adding a language is a single `values-<code>/strings.xml` file.
- New features must add strings, not literals; reviewers check this.
- The Hindi file is a living document — corrections are welcome.
