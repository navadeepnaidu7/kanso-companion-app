package com.navadeep.kanso.companion

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class CompanionFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun remoteDockEditingAndLibrarySearchWorkAcrossTransitions() {
        compose.onNodeWithContentDescription("Trackpad").assertIsDisplayed()
        compose.onNodeWithContentDescription("Use buttons").performClick()
        compose.onNodeWithContentDescription("Move right").assertIsDisplayed()
        compose.onNodeWithContentDescription("Use trackpad").performClick()
        compose.onNodeWithContentDescription("Open Netflix on TV").assertDoesNotExist()
        compose.onNodeWithContentDescription("Apps").performClick()
        compose.onNodeWithText("Connect your TV").performClick()
        compose.onNodeWithText("Try preview").performClick()
        compose.onNodeWithContentDescription("Mute").performClick()
        compose.onNodeWithContentDescription("Unmute").assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Muted")).performClick()
        compose.onNodeWithContentDescription("Mute").assertIsDisplayed()
        compose.onNodeWithContentDescription("Options").performClick()
        compose.onNodeWithText("Channels").performClick()
        compose.onNodeWithContentDescription("Channel up").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Channel down").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Options").performClick()
        compose.onNodeWithText("Volume").performClick()
        compose.onNodeWithContentDescription("Volume up").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Volume down").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Channel up").assertDoesNotExist()
        compose.onNodeWithContentDescription("Open Netflix on TV").assertDoesNotExist()
        repeat(3) {
            compose.onNodeWithContentDescription("Apps").performClick()
            compose.onAllNodesWithContentDescription("Open Netflix on TV").onFirst().assertIsDisplayed()
            compose.onNodeWithContentDescription("Close").performClick()
        }
        compose.onNodeWithContentDescription("Apps").performClick()
        compose.onAllNodesWithContentDescription("Open Netflix on TV").onFirst().performTouchInput { longClick() }
        compose.onNodeWithText("Remove from favorites").performClick()
        compose.onAllNodesWithContentDescription("Open Netflix on TV").assertCountEquals(1)
        compose.onNodeWithContentDescription("Open Netflix on TV").performTouchInput { longClick() }
        compose.onNodeWithText("Add to favorites").performClick()
        compose.onNodeWithContentDescription("Favorite apps").performScrollToNode(hasContentDescription("Open Netflix on TV"))
        compose.onAllNodesWithContentDescription("Open Netflix on TV").assertCountEquals(2)
        compose.onNodeWithContentDescription("Search apps").performClick()
        compose.onNodeWithContentDescription("Search apps").performTextInput("Netflix")
        compose.onNodeWithContentDescription("Open Netflix on TV").assertIsDisplayed()
        compose.onNodeWithContentDescription("Open YouTube on TV").assertDoesNotExist()
        compose.onNodeWithText("Done").performClick()
        compose.onAllNodesWithContentDescription("Open Netflix on TV").onLast().performClick()
        compose.onNodeWithContentDescription("Close").assertDoesNotExist()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Controlling Netflix")).assertIsDisplayed()
        compose.onNodeWithContentDescription("Rewind").assertIsDisplayed()
        compose.onNodeWithContentDescription("Fast forward").assertIsDisplayed()
        compose.onNodeWithContentDescription("Home").performClick()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Controlling Netflix")).assertDoesNotExist()
        compose.onNodeWithContentDescription("Rewind").assertDoesNotExist()
        compose.onNodeWithContentDescription("Apps").performClick()
        compose.onNodeWithContentDescription("Search apps").performClick()
        compose.onNodeWithContentDescription("Search apps").performTextInput("Spotify")
        compose.onNodeWithContentDescription("Open Spotify on TV").performClick()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Controlling Spotify")).assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous track").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next track").assertIsDisplayed()
    }
}
