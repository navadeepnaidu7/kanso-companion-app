package com.navadeep.kanso.companion.data

import org.junit.Assert.*
import org.junit.Test

class TrackpadGestureTest {
    @Test fun slowSwipeAccumulatesSmallMoves() {
        val gesture = TrackpadGesture(30f)
        assertTrue(gesture.move(12f, 0f).isEmpty())
        assertTrue(gesture.move(12f, 0f).isEmpty())
        assertEquals(listOf("right"), gesture.move(12f, 0f))
        assertEquals(listOf("right"), gesture.move(24f, 0f))
    }
    @Test fun diagonalSwipeUsesDominantAxisWithoutSpuriousTurns() {
        val gesture = TrackpadGesture(30f)
        assertEquals(listOf("down"), gesture.move(20f, 40f))
        assertTrue(gesture.move(15f, 0f).isEmpty())
        assertEquals(listOf("left"), gesture.move(-50f, 0f))
    }
    @Test fun fastSwipeEmitsMultipleStepsAndCapsOneEvent() {
        assertEquals(listOf("up", "up", "up"), TrackpadGesture(30f).move(0f, -95f))
        assertEquals(4, TrackpadGesture(30f).move(900f, 0f).size)
    }
    @Test fun resetDiscardsTravelFromPreviousTouch() {
        val gesture = TrackpadGesture(30f)
        gesture.move(25f, 0f)
        gesture.reset()
        assertTrue(gesture.move(10f, 0f).isEmpty())
    }
}
