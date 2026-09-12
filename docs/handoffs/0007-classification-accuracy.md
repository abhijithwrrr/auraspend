# 0007 — Classification Accuracy Pass: Keyword Boundaries, Living Merchant KB

**Date:** 2026-09-13 · **Branch:** `phase-0-aurora-foundation`
**Status:** Complete. Full unit suite green; goal was to make the AI
classification "up to the point" by fixing measurable accuracy defects in the
deterministic layers that run before (and alongside) the on-device LLM.

## 1. Why: three live misclassification bugs

The keyword resolver (`getCategoryIdForKeyword`) matched keys as raw
substrings, so ordinary SMS text resolved to wrong categories:

| Substring | Matches | Wrong result |
|---|---|---|
| `fee` | "co**ffee**" | Coffee → Education |
| `credit` | "**Credit** Card" payments | Every card expense → Salary |
| `vi` | "d**vi**dend", "act**i v**ity" | Random credits → Bills |
| `mall` | "s**mall**" | Unrelated text → Shopping |
| `book` | "face**book**", "note**book**" | Ads/bills → Education |
| `income` / `credit` → Salary | income lexicon words | Interest income → Salary |

Separately, two structural defects made the curated merchant database
near-useless:

1. **CSV fields were never unquoted** — `mapMerchantCategoryToLocal("Food")`
   was really called with `"Food"` (literal quotes), never matched, and the
   parser silently fell through to keywords. The merchant KB contributed
   zero categories since Phase 0.
2. **Lookup was exact-match only** — "SWIGGY*Zomato/BLR" or
   "IOCL PETROL PUMP 2234" never hit the KB.

## 2. What changed

- **`DefaultCategories.kt`** — new shared `keywordCategoryFor(text)`:
  word-boundary matching (punctuation → whitespace, padded haystack) with
  longest-key-wins priority. Removed the dangerous keys above; expanded the
  map from ~90 to ~230 boundary-safe entries (quick commerce, DTH/broadband,
  fuel/toll, insurers, pharmacies, diagnostics, travel, jewellers, ed-tech…).
  `BankMessageParser.messageKeywordCategory` now delegates to the same
  function so merchant-string and full-body scans can't drift. Added the
  missing `education` mapping in `mapMerchantCategoryToLocal`.
- **`MerchantRepository.kt`** — rebuilt resolution: `install(lines)` (pure,
  unit-testable) → `resolveMerchant(input)` with three tiers:
  MemoryKeys-normalized exact match, longest distinctive-token match (names +
  aliases + keywords column, filtered by a generic-word/stopword blocklist),
  and multi-word phrase containment. `suggestCategory` / `lookupMerchant` /
  `searchByKeyword` now resolve noisy strings. Asset load failures log via
  `AuraLog` and degrade to keyword heuristics (hard rule 1).
- **`merchant_database.csv`** — 192 → ~300 rows; removed rows that forced
  wrong categories (wallets GPay/PhonePe/Paytm/Amazon Pay/AAP/WA Pay, card
  networks Visa/Mastercard/Amex/Diners/RuPay, "UPI Transfer"), the "Gym
  Name" placeholder and dead/duplicate brands (Medlife, CVS, GoAir→Go First,
  Carrefour, FBB, Central, More, Idea, Dunzo Daily, Nykaa Beauty, Railway.app).
  "Google Drive,google" became "Google One,googleone" so "Google Pay" strings
  can't inherit a storage-subscription category. Added ~120 high-frequency
  Indian merchants with aliases and keywords.
- **`ClassificationMemory.kt`** — `stopWords` is now `internal` (shared with
  the merchant token index; no behavior change).
- **Tests** — `CategoryResolutionTest` (14 cases: every substring regression,
  boundary/longest-match semantics, end-to-end parser categories) and
  `MerchantRepositoryTest` (8 cases: noisy normalization, alias tokens,
  instamart>swiggy priority, generic-word rejection, CSV integrity incl.
  category-mapping completeness and confidence ranges, wallet rows gone).

## 3. Deliberate trade-offs

- **Brand-umbrella tokens are generic** ("google", "pay", "amazon" stays
  indexable): "Google Pay" resolving to Google One was worse than not
  resolving; the LLM/keyword layers still cover those strings.
- **Wallet/card-network rows deleted, not remapped**: a PhonePe debit is
  usually the underlying merchant's purchase or a transfer — forcing
  "Bills" was always wrong. Transfer detection stays with the parser
  (`upi`/`to self` keywords) and the AI layer.
- No LLM prompt changes: the 0.5B prompt is pinned by tests and the fixes
  above shrink its caseload; changing it needs eval data, not vibes.

## 4. Verification

| Check | Result |
|---|---|
| `./gradlew :app:compileFreeDebugKotlin` | ✅ |
| `./gradlew testFreeDebugUnitTest` (172 tests) | ✅ green |
| Screenshot drift gate (same run) | ✅ no baseline changes |

## 5. Open risks / next steps

- The token blocklist is curated by hand; a merchant in the CSV with an
  unlucky alias could still over-resolve. The CSV-integrity test guards
  structure, not semantics — a golden-set eval script (sample SMS → expected
  category) would make future tuning measurable.
- Merchant KB is ~300 rows vs the old roadmap's 2,000+ ambition; the
  normalization layer makes growth drop-in.
- UI untouched: the Aurora rebuild + screenshot baselines stay valid.
