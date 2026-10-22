package com.navadeep.kanso.companion.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.navadeep.kanso.companion.data.CompanionState

@Composable
internal fun FloatingNavigation(apps: Boolean, modifier: Modifier = Modifier, change: (Boolean) -> Unit) {
    val haptics = LocalHapticFeedback.current
    BoxWithConstraints(modifier.width(204.dp).height(52.dp)
        .shadow(12.dp, CircleShape).clip(CircleShape)
        .background(Brush.verticalGradient(listOf(Color(0xF2262626), Color(0xF21B1B1B))))
        .border(.7.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = .09f), Color.White.copy(alpha = .025f))), CircleShape).padding(5.dp)) {
        val position by animateFloatAsState(if (apps) 1f else 0f, Motion.panel(), label = "navigation lens")
        val width = maxWidth / 2
        Box(Modifier.width(width).fillMaxHeight().graphicsLayer { translationX = position * width.toPx() }
            .clip(CircleShape).background(Color.White.copy(alpha = .16f)))
        Row(Modifier.fillMaxSize()) {
            listOf(false to "Remote", true to "Apps").forEach { (value, name) ->
                val source = remember { MutableInteractionSource() }
                val pressed by source.collectIsPressedAsState()
                val scale by animateFloatAsState(if (pressed) .94f else 1f, Motion.press(), label = "navigation press")
                Row(Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clickable(interactionSource = source, indication = null, role = Role.Tab) {
                        if (apps != value) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); change(value) }
                    }.semantics { contentDescription = name; selected = apps == value },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(if (value) Symbols.Apps else Symbols.Remote, null, Modifier.size(18.dp), tint = if (apps == value) Ink.Text else Ink.Muted)
                    Spacer(Modifier.width(7.dp))
                    Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (apps == value) Ink.Text else Ink.Muted)
                }
            }
        }
    }
}
