package com.navadeep.kanso.companion.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*

@Composable
internal fun AppNavigation(apps: Boolean, modifier: Modifier = Modifier, change: (Boolean) -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .95f else 1f, Motion.press(), label = "apps press")
    val haptics = LocalHapticFeedback.current
    val label = if (apps) "Remote" else "Apps"
    Row(modifier.width(112.dp).height(48.dp).graphicsLayer { scaleX = scale; scaleY = scale }
        .controlSurface(pressed = pressed)
        .clickable(interactionSource = source, indication = null, role = Role.Button) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); change(!apps)
        }.semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(if (apps) Symbols.Remote else Symbols.Apps, null, Modifier.size(19.dp), tint = Ink.Text)
        Spacer(Modifier.width(9.dp))
        Text(label, color = Ink.Text, fontSize = 14.sp)
    }
}
