package com.awbuilds.auraspend.ui.designsystem

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Enforces the design-system rules from AGENTS.md at build time.
 *
 * These rules were documented but never checked, which is why the codebase
 * accumulated 68 hardcoded `fontSize` values and a raw hex in the chrome despite
 * a complete Material3 type scale sitting right next to them. A test is the only
 * enforcement that survives: a review comment does not.
 *
 * The checks are deliberately source-level rather than runtime — they run on every
 * unit-test invocation, need no emulator, and fail in under a second.
 */
class DesignSystemGuardTest {

    private val uiRoot: File = File("src/main/java/com/awbuilds/auraspend/ui")

    /** Files that DEFINE the tokens, so literal sizes/colors are correct there. */
    private val tokenDefinitions = setOf("AuraTokens.kt", "Theme.kt", "Color.kt", "Shape.kt")

    private fun screenFiles(): List<File> =
        uiRoot.walkTopDown()
            .filter { it.extension == "kt" && it.name !in tokenDefinitions }
            .toList()

    private fun assertNoMatches(description: String, pattern: Regex, skipDesignSystem: Boolean) {
        val offenders = screenFiles()
            .filter { !skipDesignSystem || !it.path.contains("${File.separator}designsystem${File.separator}") }
            .filter { file ->
                file.readLines().any { line ->
                    // Ignore commented-out/prose lines so a disabled feature or a doc
                    // comment does not block the build.
                    !line.isCommentLine() && pattern.containsMatchIn(line)
                }
            }
            .map { it.name }
            .distinct()

        assertTrue(
            "$description — found in: ${offenders.joinToString()}. " +
                "Use ui/designsystem tokens instead.",
            offenders.isEmpty()
        )
    }

    @Test
    fun `screens do not hardcode font sizes`() {
        // The type scale in ui/theme/Theme.kt is complete (displayLarge..labelSmall);
        // a literal size bypasses it and breaks large-font accessibility.
        assertNoMatches("Hardcoded `fontSize = N.sp`", Regex("""fontSize\s*=\s*\d+(\.\d+)?\.sp"""), false)
    }

    @Test
    fun `screens do not use raw hex colors`() {
        // Only ui/theme/Color.kt and the token files in designsystem/ may define
        // literal colors; a screen reaching for a hex breaks theming in every
        // non-light mode.
        assertNoMatches("Raw hex color `Color(0x...)`", Regex("""Color\(\s*0x"""), true)
    }

    @Test
    fun `content surfaces do not use Modifier shadow`() {
        // Floating chrome (FAB, sheets) uses the sanctioned `softShadow` helper;
        // content cards must be tonal/outlined, not elevated with a shadow.
        assertNoMatches("Raw `.shadow(` on content", Regex("""\.shadow\s*\("""), true)
    }

    @Test
    fun `no emoji in chrome`() {
        // AGENTS.md rule 5 — the old tag-emoji fallback in CategoryAvatar broke this.
        // Iterates code points rather than using a regex: a surrogate range in a
        // Java character class matches almost everything.
        val offenders = screenFiles()
            .filter { file ->
                file.readLines().any { line -> !line.isCommentLine() && line.containsEmoji() }
            }
            .map { it.name }
            .distinct()
        assertTrue("Emoji in UI code — found in: ${offenders.joinToString()}", offenders.isEmpty())
    }

    /** True if any code point is a pictographic symbol or emoji. */
    private fun String.containsEmoji(): Boolean {
        var i = 0
        while (i < length) {
            val cp = codePointAt(i)
            val isEmoji = when {
                cp in 0x1F300..0x1FAFF -> true   // pictographs, symbols, extended-A
                cp in 0x1F000..0x1F2FF -> true   // mahjong, dominoes, enclosed
                cp in 0x2600..0x27BF -> true     // misc symbols + dingbats
                cp in 0xFE00..0xFE0F -> true     // variation selectors
                cp in 0x1F1E6..0x1F1FF -> true   // regional indicators (flags)
                else -> false
            }
            if (isEmoji) return true
            i += Character.charCount(cp)
        }
        return false
    }

    @Test
    fun `design system has an error state to go with its empty state`() {
        // A failure must be visually distinct from "no data yet" — a user staring at
        // a bank balance needs to tell the two apart.
        val feedback = File(uiRoot, "designsystem/Feedback.kt")
        val state = File(uiRoot, "designsystem/State.kt")
        val all = listOf(feedback, state).filter { it.exists() }.joinToString("\n") { it.readText() }
        assertTrue("AuraErrorState is missing from the design system", all.contains("fun AuraErrorState"))
    }

    @Test
    fun `ui has no empty catch blocks`() {
        // AGENTS.md hard requirement 1. Empty catches swallow real failures.
        val offenders = screenFiles()
            .filter { file ->
                file.readText().contains(Regex("""catch\s*\([^)]*\)\s*\{\s*\}"""))
            }
            .map { it.name }
            .distinct()
        assertTrue("Empty catch block — found in: ${offenders.joinToString()}", offenders.isEmpty())
    }
}

/** True for a line that is prose rather than code (line or block comment). */
internal fun String.isCommentLine(): Boolean {
    val t = trimStart()
    return t.startsWith("//") || t.startsWith("*") || t.startsWith("/")
}

/**
 * A screen state can carry an error the screen never renders.
 *
 * This happened twice: `DashboardViewState.error` and `BudgetViewState.error` were
 * both populated by their ViewModels and then silently dropped, so a failed load
 * rendered as an empty-looking screen and a failed save made the sheet simply stop
 * responding. Nothing failed loudly, so nothing was caught.
 *
 * The rule: if a screen's state class declares an `error` field, some composable in
 * the same feature must read it.
 */
class ErrorStateWiringTest {

    private val featureDir = File("src/main/java/com/awbuilds/auraspend/ui")

    private fun featuresWithErrorState(): List<Pair<String, String>> =
        (featureDir.listFiles()?.toList() ?: emptyList())
            .filter { it.isDirectory }
            .flatMap { dir ->
                dir.listFiles { f -> f.name.endsWith("Mvi.kt") }
                    ?.toList()
                    .orEmpty()
                    .filter { it.readText().contains("val error:") }
                    .map { dir.name to dir.absolutePath }
            }

    @Test
    fun `every screen state that declares an error is actually rendered`() {
        val unrendered = featuresWithErrorState().filter { (feature, statePath) ->
            val screens = featureDir.resolve(feature)
                .listFiles { f -> f.name.endsWith("Screen.kt") || f.name.endsWith("Tab.kt") }
                ?.toList()
                .orEmpty()
            // The MVI file itself declares the field, so exclude it from the search.
            val screenText = screens
                .filterNot { it.absolutePath == statePath }
                .joinToString("\n") { it.readText() }
            !screenText.contains(".error")
        }.map { it.first }

        assertTrue(
            "These features declare an error state that no composable reads: " +
                "${unrendered.joinToString()}. Render it or remove the field.",
            unrendered.isEmpty()
        )
    }

    @Test
    fun `no screen renders a raw exception message as user-facing error text`() {
        // The ViewModels used to do `error = e.message`, which put things like
        // "SQLiteConstraintException: ..." in front of users. Screen state must carry
        // a typed UiError whose string is resolved from resources.
        val offenders = featureDir.walkTopDown()
            .filter { it.extension == "kt" }
            .filter { file ->
                // Skip comment lines: UiError.kt documents the old bug in its KDoc.
                file.readLines().any { line ->
                    !line.isCommentLine() &&
                        Regex("""error\s*=\s*[a-zA-Z]*\.message""").containsMatchIn(line)
                }
            }
            .map { it.name }
            .distinct()
            .toList()

        assertTrue(
            "Raw exception message assigned to screen state — found in: " +
                "${offenders.joinToString()}. Use UiError and log the cause instead.",
            offenders.isEmpty()
        )
    }
}
