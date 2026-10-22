package com.navadeep.kanso.companion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.navadeep.kanso.companion.data.TvApp

@Stable
internal class LaunchMotion {
    var app by mutableStateOf<TvApp?>(null)
    var origin by mutableStateOf(Rect.Zero)
    var destination by mutableStateOf(Rect.Zero)
    fun select(value: TvApp, bounds: Rect) { app = value; origin = bounds }
}

internal val LocalLaunchMotion = staticCompositionLocalOf<LaunchMotion?> { null }

/** A successful launch carries that exact tile into the active app badge. */
@Composable
internal fun LaunchFlight(motion: LaunchMotion, progress: Float, preview: Boolean) {
    val app = motion.app ?: return
    val from = motion.origin
    val to = motion.destination
    if (from.width <= 0 || to.width <= 0) return
    val fraction = progress.coerceIn(0f, 1f)
    val density = LocalDensity.current
    val width = with(density) { from.width.toDp() }
    val height = with(density) { from.height.toDp() }
    AppBannerView(app, preview, Modifier.size(width, height).graphicsLayer {
        transformOrigin = TransformOrigin(0f, 0f)
        translationX = from.left + (to.left - from.left) * fraction
        translationY = from.top + (to.top - from.top) * fraction
        scaleX = 1f + (to.width / from.width - 1f) * fraction
        scaleY = 1f + (to.height / from.height - 1f) * fraction
        alpha = 1f - ((fraction - .85f) / .15f).coerceIn(0f, 1f)
    }.clip(RoundedCornerShape(16.dp)).clearAndSetSemantics { })
}
