package com.navadeep.kanso.companion

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.navadeep.kanso.companion.data.CompanionViewModel
import com.navadeep.kanso.companion.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LibrarySheetTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var sheet: LibrarySheetState

    private fun library() {
        compose.activityRule.scenario.onActivity { activity ->
            val model = ViewModelProvider(activity)[CompanionViewModel::class.java]
            model.preview()
            activity.setContent { KansoTheme {
                val state by model.state.collectAsState()
                sheet = rememberLibrarySheetState()
                val grid = rememberLazyGridState()
                LibrarySheet(sheet, Rect.Zero) { drag ->
                    AppsPanel(state, model, grid, drag, close = sheet::hide, connect = {})
                }
            } }
        }
        compose.runOnIdle { sheet.show() }
        compose.waitForIdle()
    }

    @Test fun scrollingStaysInLibraryAndHeaderDragCanReverseOrDismiss() {
        library()
        compose.onNodeWithContentDescription("App library").performTouchInput { swipeUp() }
        compose.runOnIdle { assertEquals(1f, sheet.progress.value, .001f) }
        val header = compose.onNodeWithContentDescription("Apps panel header")
        // Reverse before lifting: the sheet must return to its open position.
        header.performTouchInput {
            down(center)
            moveBy(Offset(0f, 260f), delayMillis = 120)
            moveBy(Offset(0f, -250f), delayMillis = 140)
            up()
        }
        compose.runOnIdle { assertTrue(sheet.isOpen); assertEquals(1f, sheet.progress.value, .001f) }
        header.performTouchInput { swipe(center, center + Offset(0f, 900f), durationMillis = 350) }
        compose.runOnIdle { assertFalse(sheet.visible) }
        compose.runOnIdle { sheet.show() }
        compose.onNodeWithContentDescription("App library").performScrollToIndex(0)
            .performTouchInput { swipeDown() }
        compose.runOnIdle { assertFalse(sheet.visible) }
    }

    @Test fun closingSpringCanBeReopenedWithoutJumpingToAnEndpoint() {
        library()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { sheet.hide() }
        compose.mainClock.advanceTimeBy(96)
        var interrupted = 0f
        compose.runOnIdle {
            interrupted = sheet.progress.value
            assertTrue(interrupted > 0f && interrupted < 1f)
            sheet.show()
            assertEquals(interrupted, sheet.progress.value, .001f)
        }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(sheet.isOpen); assertEquals(1f, sheet.progress.value, .001f) }
    }
}
