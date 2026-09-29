package com.awbuilds.auraspend.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState

/**
 * The project's single swipe-to-act implementation, used by the Activity list
 * (swipe left to delete) and the classification triage inbox (swipe right to
 * save, left to dismiss).
 *
 * ## Why this exists rather than calling SwipeToDismissBox directly
 *
 * Both call sites used to pass `confirmValueChange`, which in the current
 * Compose Foundation (BOM 2026.09.00) is **deprecated without replacement**:
 * `SwipeToDismissBox` is built on `anchoredDraggable`, and the callback is no
 * longer consulted. The row therefore animated to the swiped anchor, painted its
 * coloured background, and did **nothing** — swipe-to-delete silently failed to
 * delete, and swipe-to-save silently failed to save. Both were found on-device
 * on 2026-09-29, after the deprecation had been visible in a green build and
 * been read past twice. `AGENTS.md` rule 8 exists because of it.
 *
 * The replacement is to drive the action from the state that actually settles,
 * keyed on `settledValue`. Keying on it also means the effect re-runs only when
 * the settled value *changes*, so a recomposition cannot re-fire a delete.
 *
 * [onAction] receives the direction once the gesture comes to rest. It is the
 * caller's job to make it undoable: both current callers delete or create a
 * transaction, and neither should be one accidental gesture from permanent.
 */
@Composable
fun SwipeToAct(
    onAction: (SwipeToDismissBoxValue) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Drawn behind the row while it is being dragged. Receives the **current**
     * direction, not the settled one, so the affordance can appear and grow with
     * the gesture rather than only appearing at the end.
     */
    backgroundContent: @Composable (SwipeToDismissBoxValue) -> Unit = {},
    enabledFromStartToEnd: Boolean = true,
    enabledFromEndToStart: Boolean = true,
    thresholdFraction: Float = 0.5f,
    content: @Composable (androidx.compose.foundation.layout.RowScope) -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        // 50% of the row by default. A shorter fling already overshoots it, so
        // this only governs the slow deliberate drag.
        positionalThreshold = { total -> total * thresholdFraction }
    )

    LaunchedEffect(state.settledValue) {
        val settled = state.settledValue
        if (settled != SwipeToDismissBoxValue.Settled) onAction(settled)
    }

    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundContent = { backgroundContent(state.dismissDirection) },
        enableDismissFromStartToEnd = enabledFromStartToEnd,
        enableDismissFromEndToStart = enabledFromEndToStart,
        content = content
    )
}
