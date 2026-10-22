package com.navadeep.kanso.companion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class CompanionFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun remoteDockEditingAndLibrarySearchWorkAcrossTransitions() {
        compose.onNodeWithContentDescription("Trackpad").assertIsDisplayed()
        compose.onNodeWithText("Buttons").performClick()
        compose.onNodeWithContentDescription("Move right").assertIsDisplayed()
        compose.onNodeWithText("Trackpad").performClick()
        compose.onNodeWithContentDescription("Apps").performClick()
        compose.onNodeWithText("Connect your TV").performClick()
        compose.onNodeWithText("Try preview").performClick()
        compose.onAllNodesWithContentDescription("Channel buttons").onFirst().performClick()
        compose.onNodeWithContentDescription("Channel up").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Channel down").assertIsDisplayed().performClick()
        compose.onAllNodesWithContentDescription("Volume buttons").onFirst().performClick()
        compose.onNodeWithContentDescription("Volume up").assertIsDisplayed().performTouchInput { down(center); advanceEventTime(650); up() }
        compose.onNodeWithContentDescription("Volume down").assertIsDisplayed().performClick()
        compose.onNodeWithText("Buttons").performClick()
        compose.onNodeWithContentDescription("Channel up").assertDoesNotExist()
        compose.onNodeWithContentDescription("Move right").assertIsDisplayed()
        compose.onNodeWithText("Trackpad").performClick()
        compose.onNodeWithContentDescription("Open Netflix on TV").assertIsDisplayed()
        repeat(3) {
            compose.onNodeWithContentDescription("Apps").performClick()
            compose.onAllNodesWithContentDescription("Open Netflix on TV").onFirst().assertIsDisplayed()
            compose.onNodeWithContentDescription("Close").performClick()
        }
        compose.onNodeWithContentDescription("Open Netflix on TV").performTouchInput { longClick() }
        compose.onNodeWithText("Remove from dock").performClick()
        compose.onNodeWithContentDescription("Open Netflix on TV").assertDoesNotExist()
        compose.onNodeWithContentDescription("Apps").performClick()
        compose.onNodeWithContentDescription("Open Netflix on TV").performTouchInput { longClick() }
        compose.onNodeWithText("Add to dock").performClick()
        compose.onAllNodesWithContentDescription("Open Netflix on TV").assertCountEquals(1)
        compose.onNodeWithContentDescription("Search apps").performClick()
        compose.onNodeWithContentDescription("Search apps").performTextInput("Netflix")
        compose.onNodeWithContentDescription("Open Netflix on TV").assertIsDisplayed()
        compose.onNodeWithContentDescription("Open YouTube on TV").assertDoesNotExist()
        compose.onNodeWithText("Done").performClick()
        compose.onAllNodesWithContentDescription("Open Netflix on TV").onLast().performClick()
        compose.onNodeWithContentDescription("Close").assertDoesNotExist()
        compose.onNodeWithContentDescription("Controlling Netflix").assertIsDisplayed()
        compose.onNodeWithContentDescription("Rewind").assertIsDisplayed()
        compose.onNodeWithContentDescription("Fast forward").assertIsDisplayed()
        compose.onNodeWithContentDescription("Home").performClick()
        compose.onNodeWithContentDescription("Controlling Netflix").assertDoesNotExist()
        compose.onNodeWithContentDescription("Rewind").assertDoesNotExist()
        compose.onNodeWithContentDescription("Apps").performClick()
        compose.onNodeWithContentDescription("Search apps").performClick()
        compose.onNodeWithContentDescription("Search apps").performTextInput("Spotify")
        compose.onNodeWithContentDescription("Open Spotify on TV").performClick()
        compose.onNodeWithContentDescription("Controlling Spotify").assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous track").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next track").assertIsDisplayed()
    }
}
