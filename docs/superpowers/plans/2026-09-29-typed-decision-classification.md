# Typed-Decision Classification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make on-device classification non-destructive and remove the 468 MiB model's
justification, by replacing the hard-boolean AI veto with a calibrated probability and
routing classification through sender-identified bank parsers plus a keyword category map.

**Architecture:** Classification stops treating the model as authoritative. `SmsExtraction`
gains a calibrated `isTransactionProbability`; `AiSignalFusion` may only discard a
transaction when that probability is high *and* the regex layer found no amount. Sender ID
routes to a per-bank parser registry (Axis, Canara first) whose formats are hand-written.
A keyword category map fills category gaps the parser left. Anything no parser claims
surfaces as an unrecognized message instead of being guessed at.

**Tech Stack:** Kotlin, Jetpack Compose, Room, llama.cpp (`:llama`), JUnit4, Kotlin
Multiplatform-free JVM unit tests.

**Spec:** This plan is itself the spec. It supersedes the runtime-swap direction in
`docs/handoffs/0011-on-device-ai-foundation.md`, whose Appendices A/B measured that
Needle 3 fails its accuracy gate.

## Why this exists — the measurements that forced it

Measured through the **production pipeline** (`FusedAiEval`, 65-case golden corpus):

| runtime | exact match | type | category | txns destroyed by veto | rescued |
|---|---|---|---|---|---|
| regex only (no AI) | 13.8 % | 95.4 % | 36.9 % | 0 | — |
| Qwen2.5-0.5B (shipped) | 10.8 % | 84.6 % | 38.5 % | **7** | 1 |
| SmolLM2-135M | 1.5 % | 33.8 % | 3.1 % | **40** | 0 |

The model currently shipped makes the app **worse on four of five fields**. A false veto
is not a mis-filing: `AiSignalFusion` nulls `amount` and `type`, the pipeline's
unresolved-fields gate then drops the row, and the transaction is deleted. Both models fail
identically in *kind* and differ only in *rate*, which is the signature of a rule that
trusts the model too much rather than of a model that is too small.

Reference architecture: `sarim2000/pennywiseai-tracker` (AGPL-3.0 — architecture only, no
code copied) routes 200+ per-bank parsers by sender ID, categorises by keyword map, and
uses its LLM **only** for the chat assistant. Zero LLM calls in its SMS pipeline.

## Global Constraints

- **No new Gradle module, no JNI, no vendored native code.** The `:llama` runtime stays.
- **On-device only.** No network call may carry SMS content. Auto Backup exclusion and
  `backup_rules.xml` stay as they are.
- **Every accuracy claim is a number, not a claim.** Any change to classification is
  scored by `FusedAiEval` before and after, and both numbers go in the commit message.
- **No `!!`, no `lateinit`** in new code (`AGENTS.md` §2).
- **Try/catch at every IO boundary** via `boundary { }` from `core/AuraLog.kt`
  (`AGENTS.md` §1).
- **Strings need both `values/` and `values-hi/`**; apostrophes escaped as `\'`.
- **Design-system rules are build-enforced** by `DesignSystemGuardTest`.
- `./gradlew :app:compileFreeDebugKotlin testFreeDebugUnitTest` green before every commit.
- **Do not regress the OTP/alert filter** — `isOtpOrAlertMessage` keeps its amount+verb
  rescue and its unconditional hard fraud veto (`AGENTS.md` §4).

## Review Focus

Inputs most likely to bite a user, each pinned by a test in the task that owns the code:

1. **A real debit the model is unsure about.** Expected: the transaction is kept and the
   regex parse stands. A model may never delete a transaction on a low-confidence verdict.
2. **A genuine phishing lure quoting an amount.** Expected: still vetoed. The hard fraud
   veto keeps no rescue.
3. **An SMS from a bank we have no parser for.** Expected: surfaces as unrecognized; never
   silently mis-categorised into a confident wrong category.
4. **An OTP with no amount or movement verb.** Expected: skipped, as today.
5. **A debit that ends in an OTP code.** Expected: kept, as today (handoff 0011 §3).

---

### Task 1: Calibrated veto — the fix that makes any model safe

**Files:**
- Modify: `app/src/main/java/com/awbuilds/auraspend/data/ai/OnDeviceClassifier.kt`
- Modify: `app/src/main/java/com/awbuilds/auraspend/data/classification/AiSignalFusion.kt`
- Modify: `app/src/main/java/com/awbuilds/auraspend/data/ai/QwenMessageCategorizer.kt`
- Test: `app/src/test/java/com/awbuilds/auraspend/data/classification/AiSignalFusionTest.kt`
- Test: `app/src/test/java/com/awbuilds/auraspend/data/classification/FusedAiEvalTest.kt`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: `SmsExtraction.isTransactionProbability: Float?` (null = runtime cannot say);
  `AiSignalFusion.DISCARD_MIN_CONFIDENCE: Float`; `AiSignalFusion.fuse` signature unchanged.

- [ ] **Step 1: Write the failing test**

Add to `AiSignalFusionTest.kt`:

```kotlin
@Test
fun `an unsure model verdict must not discard a transaction`() {
    val parsed = BankMessageParser.parse("Spent INR 25\nAxis Bank Card\nno. XX9054\nYasar")
    val unsure = SmsExtraction(
        isTransaction = false,
        type = null,
        category = null,
        merchant = null,
        isSubscription = false,
        rawModelOutput = null,
        isTransactionProbability = 0.4f
    )
    val fused = AiSignalFusion.fuse(parsed, unsure)
    assertTrue(
        "a 0.4-confidence veto must not delete a parsed transaction",
        fused.parsed.amount != null && fused.parsed.type != null
    )
}

@Test
fun `a confident model verdict on a message the parser could not read may discard it`() {
    val parsed = BankMessageParser.parse("Your OTP is 482913 do not share")
    val confident = SmsExtraction(
        isTransaction = false, type = null, category = null, merchant = null,
        isSubscription = false, isTransactionProbability = 0.99f
    )
    val fused = AiSignalFusion.fuse(parsed, confident)
    assertNull("a 0.99 veto on an unreadable message may discard it", fused.parsed.amount)
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testFreeDebugUnitTest --tests '*AiSignalFusionTest*'`
Expected: FAIL — `isTransactionProbability` does not exist.

- [ ] **Step 3: Add the probability to the seam**

In `OnDeviceClassifier.kt`, add to `SmsExtraction`:

```kotlin
    /**
     * How sure the runtime is that [isTransaction] is right, in [0, 1].
     *
     * Null means "this runtime cannot say" and is treated as *no opinion* — the
     * fusion layer then falls back to the regex verdict. A bare Boolean cannot
     * express doubt, which is why a model answering `false` with a coin-flip
     * confidence was indistinguishable from one that was certain, and both
     * destroyed the transaction.
     */
    val isTransactionProbability: Float? = null
```

- [ ] **Step 4: Populate it from the model's own output**

`QwenMessageCategorizer.parse` cannot read a calibrated score out of a GGUF, so it must
not invent one. Leave `isTransactionProbability` null there — null is the honest answer
and the fusion layer already treats null as "no opinion". Add a comment recording that a
future constrained runtime (which can read probabilities) should fill it.

- [ ] **Step 5: Make the veto conditional in `AiSignalFusion`**

Replace the unconditional veto at `AiSignalFusion.kt:57`:

```kotlin
        if (ai != null && !ai.isTransaction && mayDiscard(ai, base)) {
            return Fused(base.copy(amount = null, type = null), isSubscription = false, usedAi = true)
        }
```

and add:

```kotlin
    /**
     * Confidence above which a model may discard a transaction outright.
     *
     * Deliberately high. A false veto deletes a real debit, and the measured
     * rates were 7/46 (Qwen) and 40/46 (SmolLM2) — the model is far more often
     * wrong about this than right.
     */
    const val DISCARD_MIN_CONFIDENCE = 0.9f

    /**
     * Whether a model verdict is allowed to discard a parsed transaction.
     *
     * Two conditions, and both must hold. The model must be *confident*, and the
     * regex layer must have failed to find an amount — because a message the
     * parser could not read has nothing worth protecting, while one it read
     * correctly is exactly the case where the model is most likely to be wrong.
     */
    private fun mayDiscard(ai: SmsExtraction, base: ParsedBankMessage): Boolean {
        val confidence = ai.isTransactionProbability ?: return true
        return confidence >= DISCARD_MIN_CONFIDENCE && base.amount == null
    }
```

Note `?: return true` — a runtime with no calibrated head keeps today's behaviour, so
this task is behaviour-preserving for Qwen until a constrained runtime ships. The
*seam* is what changes, and that is the point.

- [ ] **Step 6: Run to verify it passes**

Run: `./gradlew :app:testFreeDebugUnitTest --tests '*AiSignalFusionTest*'`
Expected: PASS.

- [ ] **Step 7: Record the measurement**

Run: `./gradlew :app:testFreeDebugUnitTest --tests '*FusedAiEvalTest*' --rerun-tasks`
Expected: unchanged vs before (7 destroyed), because Qwen reports no probability. This is
the control: Task 1 alone must not move the number. Record it in the commit body.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/awbuilds/auraspend/data/ai/OnDeviceClassifier.kt \
        app/src/main/java/com/awbuilds/auraspend/data/classification/AiSignalFusion.kt \
        app/src/main/java/com/awbuilds/auraspend/data/ai/QwenMessageCategorizer.kt \
        app/src/test/java/com/awbuilds/auraspend/data/classification/AiSignalFusionTest.kt
git commit -m "feat(classification): calibrated veto seam; no behaviour change without a calibrated runtime

SmsExtraction gains isTransactionProbability (null = no opinion). A model may
discard a parsed transaction only when it is confident AND the regex layer
found no amount.

Measured (FusedAiEval, 65 cases): unchanged at 7 destroyed / 1 rescued, because
llama.cpp exposes no calibrated score. The seam is what this delivers."
```

---

### Task 2: Sender-routed bank parser registry

**Files:**
- Create: `app/src/main/java/com/awbuilds/auraspend/data/classification/bank/BankParser.kt`
- Create: `app/src/main/java/com/awbuilds/auraspend/data/classification/bank/BankParserRegistry.kt`
- Create: `app/src/main/java/com/awbuilds/auraspend/data/classification/bank/AxisBankParser.kt`
- Create: `app/src/main/java/com/awbuilds/auraspend/data/classification/bank/CanaraBankParser.kt`
- Create: `app/src/main/java/com/awbuilds/auraspend/data/classification/bank/SbiBankParser.kt`
- Test: `app/src/test/java/com/awbuilds/auraspend/data/classification/bank/BankParserRegistryTest.kt`
- Modify: `app/src/test/resources/golden/sms_corpus.jsonl`

**Interfaces:**
- Consumes: `ParsedBankMessage` (from `domain/model/Models.kt`).
- Produces:
  ```kotlin
  interface BankParser {
      val bankName: String
      fun canHandle(sender: String): Boolean
      fun extract(rawMessage: String): ParsedBankMessage
  }
  object BankParserRegistry {
      fun parserFor(sender: String): BankParser?
      val all: List<BankParser>
  }
  ```

**The measurement that must pass:** Axis corpus cases go from the current 24/65 category
and 62/65 type to 100 % on the eight `axis_*` cases. If a bank parser does not beat the
global regex on its own bank's cases, it is not worth existing.

- [ ] **Step 1: Add sender IDs to the corpus**

Sender is free metadata the app already receives and the corpus never captured, which is
why sender routing has never been measurable. Add `"sender"` to each case. Use the real
DLT IDs banks send from:

```json
{"id":"axis_multiline_yasar","sender":"AXISBK", ...}
{"id":"axis_classic_sil","sender":"AXISBANK", ...}
{"id":"axis_mcdonalds","sender":"AXIS-BANK", ...}
{"id":"axis_yas_mart","sender":"AXISBK", ...}
{"id":"canara_neft_credit","sender":"CANARA", ...}
{"id":"canara_debit_cyber_fraud","sender":"CANARA", ...}
{"id":"canara_debit_10000","sender":"CANARA", ...}
```

Every other case gets `"sender": ""` (unknown), which is a real state: many users bank
with institutions we have no parser for, and the plan must behave correctly there.

- [ ] **Step 2: Write the failing registry test**

Create `BankParserRegistryTest.kt`:

```kotlin
class BankParserRegistryTest {
    @Test
    fun `resolves the DLT sender id to the right bank`() {
        assertEquals("Axis Bank", BankParserRegistry.parserFor("AXISBK")?.bankName)
        assertEquals("Axis Bank", BankParserRegistry.parserFor("VI-AXISBK-S")?.bankName)
        assertEquals("Axis Bank", BankParserRegistry.parserFor("AXIS")?.bankName)
        assertEquals("Canara Bank", BankParserRegistry.parserFor("CANARA")?.bankName)
        assertNull(BankParserRegistry.parserFor("UNKNOWN-BANK"))
        assertNull(BankParserRegistry.parserFor(""))
    }
}
```

- [ ] **Step 3: Run to verify it fails**

Run: `./gradlew :app:testFreeDebugUnitTest --tests '*BankParserRegistryTest*'`
Expected: FAIL — `BankParserRegistry` does not exist.

- [ ] **Step 4: Write the interface and registry**

`BankParser.kt`:

```kotlin
package com.awbuilds.auraspend.data.classification.bank

import com.awbuilds.auraspend.domain.model.ParsedBankMessage

/**
 * One bank's SMS formats, hand-written.
 *
 * A per-bank parser exists because a bank's message carries several amounts and
 * several near-miss tokens, and only that bank's format disambiguates them —
 * the Axis case in the golden corpus is "Spent INR 25", then a card number, a
 * timestamp, the merchant, and `Avl Limit: INR 48058.57`. A single global regex
 * has to disambiguate that; a parser that knows it is Axis does not.
 */
interface BankParser {
    val bankName: String
    fun canHandle(sender: String): Boolean
    fun extract(rawMessage: String): ParsedBankMessage
}
```

`BankParserRegistry.kt`:

```kotlin
package com.awbuilds.auraspend.data.classification.bank

object BankParserRegistry {
    val all: List<BankParser> = listOf(
        AxisBankParser(), CanaraBankParser(), SbiBankParser()
    )

    fun parserFor(sender: String): BankParser? {
        if (sender.isBlank()) return null
        val normalized = sender.trim().uppercase()
        return all.firstOrNull { it.canHandle(normalized) }
    }
}
```

- [ ] **Step 5: Write the Axis parser**

`AxisBankParser.kt` — match PennyWise's sender-matching idea (learned from the public
repo, reimplemented):

```kotlin
class AxisBankParser : BankParser {
    override val bankName = "Axis Bank"

    // Axis sends from short IDs and DLT templates of the form "<CHANNEL>-AXISBK-S".
    private val senderIds = listOf("AXISBK", "AXISBANK", "AXISB", "AXIS")
    private val dltTemplate = Regex("^[A-Z]{2}-AXIS(BK|BANK|B)?-S?$")

    override fun canHandle(sender: String) = true // filtered by registry; see below
```

Correct this immediately — the honest implementation filters on the ids, and the registry
calls `canHandle` on the *normalized* sender:

```kotlin
    override fun canHandle(sender: String): Boolean =
        senderIds.contains(sender) || dltTemplate.containsMatchIn(sender)
```

with, for the amount:

```kotlin
    // "Spent INR 3059" / "INR 2299" — the amount is always the one beside a
    // movement verb, never the Avl Limit line, which is why this is per-bank.
    private val spend = Regex("""(?:Spent|INR|Rs\.?)\s*([0-9,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val avl = Regex("""Avl\s+Limit:\s*(?:INR|Rs\.?)\s*([0-9,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val cardLine = Regex("""no\.\s*[X\d]{2,}""", RegexOption.IGNORE_CASE)
    private val timestampLine = Regex("""\d{2}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\s+IST""")
    private val date = Regex("""(\d{2})-(\d{2})-(\d{2})""")
```

and `extract` returning a `ParsedBankMessage` with `bankName = "Axis Bank"`, the amount
from `spend` (never `avl`), the merchant from the line between the timestamp and the
`Avl Limit` line, `bankName` set, and `rawMessage` preserved.

- [ ] **Step 6: Write Canara and SBI parsers**

Canara must handle the *credit* wording and the "Dial 1930 to report cyber fraud" footer
that decorates every genuine Canara debit — the corpus case
`canara_debit_cyber_fraud` is exactly this. SBI handles `credited`/`debited` phrasing
with `A/c` and `avl`.

- [ ] **Step 7: Run the registry test**

Run: `./gradlew :app:testFreeDebugUnitTest --tests '*BankParserRegistryTest*'`
Expected: PASS.

- [ ] **Step 8: Measure the parsers against the regex floor**

Add to the same test class a per-bank accuracy assertion over the corpus `axis_*` cases,
using `ClassificationEval.loadCorpus()` and comparing to the `RegexBaselineEval` numbers
(95.4 % type, 36.9 % category). Expected: the eight Axis/Canara cases reach 100 % type.
If not, fix the parser before committing — a parser that does not beat the global regex is
not worth having.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/awbuilds/auraspend/data/classification/bank/ \
        app/src/test/java/com/awbuilds/auraspend/data/classification/bank/ \
        app/src/test/resources/golden/sms_corpus.jsonl
git commit -m "feat(classification): sender-routed per-bank parsers (Axis, Canara, SBI)

Adds sender IDs to the golden corpus — free metadata the app already receives
and the corpus never captured, which is why routing was unmeasurable.

Measured: type 8/8 on the axis_*/canara_* cases (global regex: 62/65 overall)."
```

---

### Task 3: Keyword category map as the gap-filler

**Files:**
- Create: `app/src/main/java/com/awbuilds/auraspend/data/classification/CategoryKeywordMap.kt`
- Test: `app/src/test/java/com/awbuilds/auraspend/data/classification/CategoryKeywordMapTest.kt`

**Interfaces:**
- Consumes: `merchant: String?`, `type: TransactionType?`, the `id -> name` map.
- Produces: `CategoryKeywordMap.resolve(merchant: String?, isExpense: Boolean): String?`

- [ ] **Step 1: Write the failing test**

```kotlin
class CategoryKeywordMapTest {
    @Test
    fun `resolves merchants to the seeded category ids`() {
        assertEquals("cat_food", CategoryKeywordMap.resolve("Swiggy", isExpense = true))
        assertEquals("cat_grocery", CategoryKeywordMap.resolve("BigBasket", isExpense = true))
        assertEquals("cat_transport", CategoryKeywordMap.resolve("IOCL Petrol Pump", isExpense = true))
        assertEquals("cat_subscription", CategoryKeywordMap.resolve("Netflix", isExpense = true))
        assertEquals("cat_salary", CategoryKeywordMap.resolve("Digitide Solutions Limited", isExpense = false))
    }

    @Test
    fun `income never resolves to a spending category`() {
        assertEquals("cat_salary", CategoryKeywordMap.resolve("refund from Swiggy", isExpense = false))
    }

    @Test
    fun `an unknown merchant resolves to null so the caller keeps looking`() {
        assertNull(CategoryKeywordMap.resolve("ZZQ Unknown Merchant", isExpense = true))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testFreeDebugUnitTest --tests '*CategoryKeywordMapTest*'`
Expected: FAIL — `CategoryKeywordMap` does not exist.

- [ ] **Step 3: Implement with word boundaries**

Build a longest-keyword-first map, matching on word boundaries. Reuse the invariant
already recorded in `CLAUDE.md`: substring-unsafe keys ("fee", "credit", "vi") caused real
misfiling, so keys must be whole words.

```kotlin
object CategoryKeywordMap {
    private val EXPENSE: Map<String, String> = mapOf(
        "swiggy" to "cat_food", "zomato" to "cat_food", "mcdonalds" to "cat_food",
        "dominos" to "cat_food", "starbucks" to "cat_food", "cafe" to "cat_food",
        "restaurant" to "cat_food", "bigbasket" to "cat_grocery", "dmart" to "cat_grocery",
        "reliance fresh" to "cat_grocery", "more" to "cat_grocery", "blinkit" to "cat_grocery",
        "amazon" to "cat_shopping", "flipkart" to "cat_shopping", "myntra" to "cat_shopping",
        "ajio" to "cat_shopping", "meesho" to "cat_shopping",
        "uber" to "cat_transport", "ola" to "cat_transport", "rapido" to "cat_transport",
        "petrol" to "cat_transport", "pump" to "cat_transport", "indian oil" to "cat_transport",
        "bharat petroleum" to "cat_transport", "hpcl" to "cat_transport",
        "netflix" to "cat_subscription", "spotify" to "cat_subscription",
        "amazon prime" to "cat_subscription", "hotstar" to "cat_subscription",
        "youtube premium" to "cat_subscription", "icloud" to "cat_subscription",
        "google one" to "cat_subscription",
        "hospital" to "cat_healthcare", "pharmacy" to "cat_healthcare",
        "apollo" to "cat_healthcare", "medplus" to "cat_healthcare",
        "school" to "cat_education", "college" to "cat_education", "tuition" to "cat_education",
        "electricity" to "cat_bills", "adani electricity" to "cat_bills",
        "tata power" to "cat_bills", "airtel" to "cat_bills", "jio" to "cat_bills",
        "vodafone" to "cat_bills", "broadband" to "cat_bills", "recharge" to "cat_bills"
    )

    private val INCOME: Map<String, String> = mapOf(
        "salary" to "cat_salary", "payroll" to "cat_salary",
        "refund" to "cat_other", "cashback" to "cat_other", "interest" to "cat_other"
    )

    fun resolve(merchant: String?, isExpense: Boolean): String? {
        val m = merchant?.lowercase()?.trim() ?: return null
        if (m.isEmpty()) return null
        val table = if (isExpense) EXPENSE else INCOME
        return table.entries
            .sortedByDescending { it.key.length }
            .firstOrNull { (key, _) -> containsWord(m, key) }
            ?.value
    }

    private fun containsWord(haystack: String, key: String): Boolean {
        val pattern = Regex("\\b" + Regex.escape(key).replace("\\ ", "\\s+") + "\\b")
        return pattern.containsMatchIn(haystack)
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testFreeDebugUnitTest --tests '*CategoryKeywordMapTest*'`
Expected: PASS.

- [ ] **Step 5: Wire it into `AiSignalFusion` as a fallback, never an override**

In `fuse`, change `resolvedCategory` so the keyword map only fills a gap:

```kotlin
        val resolvedCategory = when {
            isSubscription -> "cat_subscription"
            base.categoryId != null -> base.categoryId
            else -> ai?.category
                ?: CategoryKeywordMap.resolve(resolvedMerchant, resolvedType == TransactionType.EXPENSE)
        }
```

- [ ] **Step 6: Measure**

Run: `./gradlew :app:testFreeDebugUnitTest --tests '*RegexBaselineEvalTest*' --rerun-tasks`
Expected: category ≥ 36.9 % (the floor — the map may only fill gaps, never override), and
`type` unchanged at 95.4 %.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/awbuilds/auraspend/data/classification/CategoryKeywordMap.kt \
        app/src/test/java/com/awbuilds/auraspend/data/classification/CategoryKeywordMapTest.kt \
        app/src/main/java/com/awbuilds/auraspend/data/classification/AiSignalFusion.kt
git commit -m "feat(classification): keyword category map fills parser gaps only

Whole-word matching, longest key first. Substring-unsafe keys caused real
misfiling before (see CLAUDE.md), so keys are word-bounded.

Measured (RegexBaselineEval): category >= 36.9% floor, type unchanged at 95.4%."
```

---

### Task 4: Surface unrecognized messages instead of guessing

**Files:**
- Modify: `app/src/main/java/com/awbuilds/auraspend/data/local/dao/Daos.kt`
- Modify: `app/src/main/java/com/awbuilds/auraspend/data/local/BackupData.kt`
- Modify: `app/src/main/java/com/awbuilds/auraspend/data/local/BackupSerializer.kt`
- Create: `app/src/main/java/com/awbuilds/auraspend/data/local/entity/UnrecognizedSmsEntity.kt`
- Test: `app/src/test/java/com/awbuilds/auraspend/data/local/BackupSerializerToleranceTest.kt`

**Interfaces:**
- Consumes: `BankParserRegistry.parserFor(sender)` from Task 2.
- Produces: `unrecognized_sms` table; `BackupData.unrecognizedSms` (format v4).

- [ ] **Step 1: Write the failing tolerance test**

Extend `BackupSerializerToleranceTest.kt`: a v3 backup that predates the table must still
restore, defaulting to an empty list. A v4 backup round-trips its entries.

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testFreeDebugUnitTest --tests '*BackupSerializerToleranceTest*'`
Expected: FAIL — `unrecognizedSms` does not exist.

- [ ] **Step 3: Add the entity, DAO, and bump `BackupData` to v4**

`UnrecognizedSmsEntity`: `(id, sender, body, timestamp, createdAt)`. Only sender, body and
timestamp — no amounts, no merchant, nothing derived.

**Format version must bump to 4** and deserialization must default a missing key to an
empty list, per the `AGENTS.md` rule that `BackupData` carries every user-owned table and
stays tolerant of legacy payloads.

- [ ] **Step 4: Run the restore + full suite**

Run: `./gradlew :app:compileFreeDebugKotlin testFreeDebugUnitTest`
Expected: PASS, and no migration crash — verify `exportSchema` is unaffected.

- [ ] **Step 5: Write rows when nothing claims the message**

In `SmsPipelineProcessor`, when `BankParserRegistry.parserFor(sender) == null` **and** the
regex path produces no saveable transaction, insert an `unrecognized_sms` row rather than
discarding the message. This is the difference from PennyWise's `UnrecognizedSms` bucket
and from today's silent drop.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/awbuilds/auraspend/data/local/ \
        app/src/main/java/com/awbuilds/auraspend/data/classification/SmsPipelineProcessor.kt
git commit -m "feat(classification): surface unrecognized SMS instead of dropping them

A message no parser claims and the regex layer cannot read is now recorded for
the user to see, rather than vanishing. BackupData is format v4; v3 payloads
restore with an empty list."
```

---

### Task 5: Measure the whole system, then decide the model's fate

**Files:**
- Modify: `app/src/main/java/com/awbuilds/auraspend/data/ai/ModelConstants.kt` (only if a
  measurement justifies it)
- Modify: `docs/handoffs/0012-*.md`

**This task decides whether the 468 MiB download survives at all.** Tasks 1–4 change
`FusedAiEval`'s inputs, so the earlier numbers no longer describe the system.

- [ ] **Step 1: Re-run the fused evaluation**

Run: `./gradlew :app:testFreeDebugUnitTest --tests '*FusedAiEvalTest*' --rerun-tasks`
Expected: the "regex only" side of the comparison now reflects the bank parsers and the
keyword map. Record every number.

- [ ] **Step 2: Decide by the recorded floors, not by preference**

| criterion | bar |
|---|---|
| txns destroyed by veto | **0** |
| exact match | ≥ 13.8 % (the no-model floor) |
| category | ≥ 36.9 % |
| type | ≥ 95.4 % |

- If **destroyed == 0** and every floor holds with the model present: the model is no
  longer destructive. Keep it, and the size question is separable.
- If the model is still net-negative on any field: **delete the model path** —
  `ModelConstants`, `ModelDownloadManager`, `LocalLlmProvider`, `LlamaCppLlm`,
  `QwenMessageCategorizer`, the `:llama` module and `third_party/llama.cpp` — and let
  `UnavailableClassifier` be the permanent answer. That removes 468 MiB, twelve `.so` per
  ABI, and the audit surface, and it is the outcome the measurements currently point at.
- Either way, record the numbers and the decision in `docs/handoffs/0012-*.md` with the
  commands and the before/after table.

- [ ] **Step 3: Commit the decision**

```bash
git add -A app/src/main docs/handoffs
git commit -m "docs(handoff): 0012 typed-decision classification, with the measured decision"
```

---

## Explicitly out of scope

- **Jev (TypeSafe AI).** Hosted, proprietary, waitlisted, no open weights. It would ship
  every bank SMS to a third party, contradicting the product's central promise. Its
  *shape* — typed values with calibrated probabilities instead of prose — is the idea
  this plan implements locally via `isTransactionProbability`.
- **Any further GGUF swap.** Two have been measured and rejected (Qwen fused 10.8 %,
  SmolLM2 fused 1.5 %). A third 0.5B is not expected to change the verdict.
- **An embedding / kNN classifier** over the merchant database. A genuinely different
  bet that would give calibrated probabilities for free, but it is a hypothesis with no
  measurement. Worth prototyping *after* this plan, not inside it.
- **Fine-tuning.** Local LoRA drops the calibration head; hosted fine-tuning uploads bank
  SMS. Both were analysed in handoff 0011 Appendix B.
- **A chat assistant.** PennyWise's use of its LLM, and a separate product decision.
