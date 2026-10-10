# 0021 — branching convention: `main` + `dev`

**Status:** adopted 2026-10-10

## Decision

- **`main`** — release-ready, default branch, protected (PR + 1 review required,
  no force-push). Receives release merges from `dev` (and hotfixes).
- **`dev`** — integration branch. Day-to-day work (including agent sessions)
  lands here first; it is intentionally *not* protected so pushes stay
  frictionless.
- **Work branches** — short-lived, branched off `dev`, deleted once merged:
  - `phase-N-name` for a planned phase (the repo's historical convention), or
  - `<type>/<topic>` where type ∈ `feat | fix | chore | docs | perf | refactor`.
- Conventional commit subjects everywhere (unchanged, AGENTS #7).

## What changed

- `dev` created from `main` (`3c3e6d5`) and pushed with tracking.
- CI and PR checks trigger on **both** `main` and `dev` again
  (`.github/workflows/ci.yml`, `.github/workflows/pr_check.yml`).
- `AGENTS.md` requirement 7 and `CLAUDE.md`'s branch-strategy line rewritten
  to the convention above.

## Verification

- `git branch -a`: `main`, `dev`; `origin/HEAD → main`; `dev` tracks
  `origin/dev`; `dev == main == 3c3e6d5` at adoption.
- No protection rules on `dev`; `main` protection untouched.

## Next step

Continue the 0.1.1 launch checklist on `dev` (Play upload-key reset → rebuilt
AAB re-upload → preview/send for review). Merge `dev` → `main` at release time;
release-please-style chore: `main` docs will lag `dev` until that merge.
