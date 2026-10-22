package com.navadeep.kanso.companion.data

import kotlin.math.abs

/** Converts accumulated travel into discrete TV focus steps, preserving the remainder. */
class TrackpadGesture(private val threshold: Float) {
    init { require(threshold > 0) }
    private var x = 0f
    private var y = 0f
    fun reset() { x = 0f; y = 0f }
    fun move(dx: Float, dy: Float): List<String> {
        x += dx
        y += dy
        val steps = mutableListOf<String>()
        while (maxOf(abs(x), abs(y)) >= threshold && steps.size < 4) {
            if (abs(x) > abs(y)) {
                steps += if (x > 0) "right" else "left"
                x += if (x > 0) -threshold else threshold
                y = 0f
            } else {
                steps += if (y > 0) "down" else "up"
                y += if (y > 0) -threshold else threshold
                x = 0f
            }
        }
        return steps
    }
}
