package com.navadeep.kanso.companion.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** One spring owns both button transitions and drag release, including reversals mid-flight. */
@Stable
internal class LibrarySheetState(private val savedOpen: MutableState<Boolean>, private val scope: CoroutineScope) {
    val progress = Animatable(if (savedOpen.value) 1f else 0f)
    val isOpen get() = savedOpen.value
    val visible by derivedStateOf { isOpen || progress.value > 0f || dragging }
    private var animation: Job? = null
    private var dragging = false

    fun show() = animate(true)
    fun hide() = animate(false)

    private fun animate(open: Boolean, velocity: Float = progress.velocity) {
        dragging = false
        savedOpen.value = open
        animation?.cancel()
        animation = scope.launch { progress.animateTo(if (open) 1f else 0f, Motion.panel(), initialVelocity = velocity) }
    }

    fun beginDrag() {
        animation?.cancel()
        dragging = true
        savedOpen.value = true
    }

    fun dragBy(pixels: Float, travel: Float): Float {
        if (!dragging) beginDrag()
        val old = progress.value
        val next = (old - pixels / travel).coerceIn(0f, 1f)
        scope.launch(start = CoroutineStart.UNDISPATCHED) { progress.snapTo(next) }
        return (old - next) * travel
    }

    fun release(pixelsPerSecond: Float, travel: Float) {
        val velocity = -pixelsPerSecond / travel
        val open = when {
            velocity < -.55f -> false
            velocity > .35f -> true
            else -> progress.value >= .78f
        }
        animate(open, velocity)
    }

    fun nestedScroll(travel: Float) = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (dragging && source == NestedScrollSource.UserInput)
                Offset(0f, dragBy(available.y, travel)) else Offset.Zero

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (available.y > 0f && source == NestedScrollSource.UserInput)
                Offset(0f, dragBy(available.y, travel)) else Offset.Zero

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (!dragging) return Velocity.Zero
            release(available.y, travel)
            return Velocity(0f, available.y)
        }
    }
}

@Composable
internal fun rememberLibrarySheetState(): LibrarySheetState {
    val open = rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    return remember(scope) { LibrarySheetState(open, scope) }
}

/** Reveal the surface from its button; keep artwork and typography at their natural size. */
@Composable
internal fun LibrarySheet(state: LibrarySheetState, origin: Rect, content: @Composable (Modifier) -> Unit) {
    if (!state.visible) return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val source = if (origin.width > 0f) origin else Rect(width * .35f, height * .9f, width * .65f, height * .96f)
        val travel = source.top.coerceAtLeast(1f)
        val nested = remember(state, travel) { state.nestedScroll(travel) }
        val drag = Modifier.draggable(rememberDraggableState { state.dragBy(it, travel) }, Orientation.Vertical,
            startDragImmediately = state.progress.isRunning,
            onDragStarted = { state.beginDrag() }, onDragStopped = { state.release(it, travel) })
        Box(Modifier.fillMaxSize().nestedScroll(nested).graphicsLayer {
            shape = LibraryRevealShape(state.progress.value.coerceIn(0f, 1f), source)
            clip = true
        }.background(Ink.Surface)) {
            Box(Modifier.fillMaxSize().graphicsLayer {
                val p = state.progress.value.coerceIn(0f, 1f)
                translationY = source.top * (1f - p)
                alpha = ((p - .08f) / .62f).coerceIn(0f, 1f)
            }.statusBarsPadding()) { content(drag) }
        }
    }
}

private class LibraryRevealShape(private val progress: Float, private val source: Rect) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val remaining = 1f - progress
        val bounds = Rect(source.left * remaining, source.top * remaining,
            size.width + (source.right - size.width) * remaining,
            size.height + (source.bottom - size.height) * remaining)
        val radius = with(density) { 36.dp.toPx() } * (remaining * 8f).coerceIn(0f, 1f)
        return Outline.Rounded(RoundRect(bounds, CornerRadius(radius, radius)))
    }
}
