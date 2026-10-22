package com.navadeep.kanso.companion

import androidx.compose.foundation.layout.size
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.navadeep.kanso.companion.ui.KansoTheme
import com.navadeep.kanso.companion.ui.RemoteSurface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RemoteRockerTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun holdRepeatsAndReleaseOrLeavingTheButtonCancels() {
        val commands = mutableListOf<String>()
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                KansoTheme { RemoteSurface(2, Modifier.size(360.dp, 400.dp), commands::add) }
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        val up = compose.onNodeWithContentDescription("Channel up")
        up.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(700)
        compose.runOnIdle {
            assertTrue(commands.size >= 3)
            assertTrue(commands.all { it == "channel_up" })
        }
        up.performTouchInput { up() }
        val afterRelease = commands.size
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle { assertEquals(afterRelease, commands.size) }

        val volume = compose.onNodeWithContentDescription("Volume down")
        volume.performTouchInput { down(center); moveTo(center.copy(x = -30f)) }
        compose.mainClock.advanceTimeBy(700)
        compose.runOnIdle { assertEquals(afterRelease + 1, commands.size) }
        volume.performTouchInput { up() }
    }
}
