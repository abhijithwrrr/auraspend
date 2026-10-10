# Agent memory — AuraSpend

Claude Code auto-loads only this file, so this is the entry point — everything
durable lives elsewhere. Keep it a pointer; promote findings to `memory/` or
`AGENTS.md` instead of growing this file.

**Start every session by reading `AGENTS.md` and the latest file in
`docs/handoffs/`.** The contract (hard requirements, commands, invariants) is
`AGENTS.md`; the session log is `docs/handoffs/`; the durable detail is
`memory/`.

| What | Where |
|---|---|
| Agent contract, commands, hard requirements | `AGENTS.md` |
| What shipped, session by session (latest first) | `docs/handoffs/` |
| Glossary, decisions, stack, project state | `memory/` |
| Architecture decisions | `docs/adr/` |
| Roadmap, deliberate no-gos | `ROADMAP.md` |
| Design system | `docs/design/aurora.md` + `ui/designsystem/` |
| Measured classification numbers | `docs/evals/README.md` |

## Classification hot spots (cited from source)

- `keywordCategoryFor` (`data/classification/DefaultCategories.kt`) —
  word-boundary, longest-key keyword scan; substring-unsafe keys ("fee",
  "credit", "vi") caused real misfiling. Never add one.
- `MerchantRepository.resolveMerchant` — normalized + token + phrase tiers;
  the merchant CSV is unquoted inside `install()`; wallets and card networks
  were removed on purpose (they forced wrong categories).

## Before ending a session

- Green build: `:app:compileProdDebugKotlin`, `testProdDebugUnitTest` and
  `:app:lintProdDebug`; read every `w:` warning.
- Update the "Latest:" line in `AGENTS.md` and add `docs/handoffs/NNNN-*.md`.
