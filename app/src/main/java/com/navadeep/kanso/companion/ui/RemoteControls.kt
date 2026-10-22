package com.navadeep.kanso.companion.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.navadeep.kanso.companion.data.TrackpadGesture
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SoftButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    plain: Boolean = false,
    large: Boolean = false,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = animateFloatAsState(if (pressed) .94f else 1f, Motion.press(), label = "press")
    val haptics = LocalHapticFeedback.current
    Box(modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .clip(CircleShape).background(Brush.verticalGradient(when {
            filled -> listOf(if (pressed) Color(0xFFCBCBD1) else Color(0xFFF2F2F5), Ink.Accent)
            plain -> listOf(Color.White.copy(alpha = if (pressed) .08f else 0f), Color.Transparent)
            else -> listOf(if (pressed) Color(0xFF303030) else Color(0xFF1C1C1C), Color(0xFF1C1C1C))
        }))
        .clickable(interactionSource = interaction, indication = null, role = Role.Button,
            onClickLabel = label) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(if (large) 53.dp else 25.dp), tint = if (filled) Ink.Background else Ink.Text)
    }
}

@Composable
fun ModeSwitch(mode: Int, onChange: (Int) -> Unit) {
    BoxWithConstraints(Modifier.width(200.dp).height(40.dp).clip(CircleShape).background(Color(0xFF141414)).padding(3.dp)) {
        val fraction = animateFloatAsState(mode.toFloat(), Motion.settle(), label = "mode")
        val width = maxWidth / 2
        Box(Modifier.graphicsLayer { translationX = width.toPx() * fraction.value }.width(width).fillMaxHeight()
            .clip(CircleShape).background(Color(0xFF303030)))
        Row(Modifier.fillMaxSize()) {
            listOf("Trackpad", "Buttons").forEachIndexed { value, text ->
                val haptics = LocalHapticFeedback.current
                Row(Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                    .clickable(role = Role.Tab) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onChange(value)
                    }.semantics { selected = mode == value },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(text, color = if (mode == value) Ink.Text else Ink.Muted,
                        fontSize = 13.sp, fontWeight = if (mode == value) FontWeight.SemiBold else FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun RemoteSurface(mode: Int, modifier: Modifier = Modifier, command: (String) -> Unit, accent: Color = Ink.Accent,
    footer: @Composable () -> Unit = {}) {
    val tint by animateColorAsState(accent, Motion.panel(), label = "app material")
    Box(modifier.clip(RoundedCornerShape(38.dp)).background(Color(0xFF1C1C1C))) {
        AnimatedContent(mode, modifier = Modifier.fillMaxSize(), label = "remote mode",
            transitionSpec = {
                (fadeIn(Motion.settle()) + scaleIn(Motion.settle(), initialScale = .94f)) togetherWith
                    (fadeOut(Motion.press()) + scaleOut(Motion.settle(), targetScale = .96f))
            }) { current ->
            Box(Modifier.fillMaxSize()) {
            when (current) {
                1 -> DirectionPad(command, tint)
                2 -> ChannelControls(command)
                else -> Trackpad(command, tint)
            }
            }
        }
        Box(Modifier.align(Alignment.BottomCenter)) { footer() }
    }
}

@Composable
private fun Trackpad(command: (String) -> Unit, accent: Color) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val currentCommand by rememberUpdatedState(command)
    val scope = rememberCoroutineScope()
    var touch by remember { mutableStateOf(Offset.Unspecified) }
    var touching by remember { mutableStateOf(false) }
    val glow by animateFloatAsState(if (touching) 1f else 0f, Motion.settle(), label = "touch glow")
    val threshold = with(density) { 29.dp.toPx() }
    val shape = RoundedCornerShape(36.dp)
    Canvas(Modifier.fillMaxSize().clip(shape)
        .semantics {
            contentDescription = "Trackpad"
            role = Role.Button
            onClick("Select") { currentCommand("select"); true }
            onLongClick("Long press") { currentCommand("long_select"); true }
            customActions = listOf("up", "down", "left", "right").map { direction ->
                CustomAccessibilityAction("Move $direction") { currentCommand(direction); true }
            }
        }
        .pointerInput(threshold) {
            awaitEachGesture {
                val down = awaitFirstDown()
                touching = true
                touch = down.position
                val gesture = TrackpadGesture(threshold)
                var total = Offset.Zero
                var moved = false
                var longPressed = false
                var released = false
                val timer = scope.launch {
                    delay(viewConfiguration.longPressTimeoutMillis)
                    if (!moved && touching) {
                        longPressed = true
                        currentCommand("long_select")
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) { released = true; break }
                        val delta = change.position - change.previousPosition
                        total += delta
                        touch = change.position
                        if (total.getDistance() > viewConfiguration.touchSlop) {
                            moved = true
                            timer.cancel()
                        }
                        if (moved && !longPressed) gesture.move(delta.x, delta.y).forEach { key ->
                            currentCommand(key)
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        change.consume()
                    }
                    if (released && !moved && !longPressed) {
                        currentCommand("select")
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                } finally { timer.cancel(); touching = false }
            }
        }) {
        if (touch.isSpecified && glow > .001f) {
            val radius = 130.dp.toPx()
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .18f * glow), Color.Transparent), touch, radius), radius, touch)
            drawCircle(accent.copy(alpha = .25f * glow), 18.dp.toPx(), touch)
            drawCircle(Ink.Accent.copy(alpha = .65f * glow), 3.dp.toPx(), touch)
        }

    }
}

@Composable
private fun DirectionPad(command: (String) -> Unit, accent: Color) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth, maxHeight) - 12.dp
        Box(Modifier.size(diameter).clip(CircleShape)
            .background(Color(0xFF252525))) {
            HoldDirection("up", Symbols.Up, Modifier.align(Alignment.TopCenter).fillMaxWidth(.47f).fillMaxHeight(.31f), command)
            HoldDirection("down", Symbols.Down, Modifier.align(Alignment.BottomCenter).fillMaxWidth(.47f).fillMaxHeight(.31f), command)
            HoldDirection("left", Symbols.Left, Modifier.align(Alignment.CenterStart).fillMaxWidth(.31f).fillMaxHeight(.47f), command)
            HoldDirection("right", Symbols.Right, Modifier.align(Alignment.CenterEnd).fillMaxWidth(.31f).fillMaxHeight(.47f), command)
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(if (pressed) .9f else 1f, Motion.press(), label = "select press")
            val haptics = LocalHapticFeedback.current
            Box(Modifier.align(Alignment.Center).size(diameter * .34f)
                .graphicsLayer { scaleX = scale; scaleY = scale }.clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF262524), Color(0xFF0F0F0F))))
                .combinedSelect(interaction, {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); command("select")
                }, {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress); command("long_select")
                }), contentAlignment = Alignment.Center) {
                Text("OK", color = Ink.Text, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun Modifier.combinedSelect(source: MutableInteractionSource, click: () -> Unit, longClick: () -> Unit) =
    this.then(Modifier.combinedClickable(interactionSource = source, indication = null, role = Role.Button,
        onClickLabel = "Select", onLongClickLabel = "Long press", onLongClick = longClick, onClick = click)
        .semantics { contentDescription = "Select" })

@Composable
private fun ChannelControls(command: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        val rockerHeight = minOf(242.dp, maxHeight - 24.dp).coerceAtLeast(100.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            ControlRocker("Volume", Symbols.VolumeUp, "volume_up", "volume_down", rockerHeight, command)
            ControlRocker("Channel", Symbols.Channels, "channel_up", "channel_down", rockerHeight, command)
        }
    }
}

@Composable
private fun ControlRocker(title: String, icon: ImageVector, up: String, down: String,
    height: androidx.compose.ui.unit.Dp, command: (String) -> Unit) {
    Column(Modifier.width(108.dp).height(height).clip(RoundedCornerShape(36.dp))
        .background(Color(0xFF303030)),
        horizontalAlignment = Alignment.CenterHorizontally) {
        HoldDirection(up, Symbols.Plus, Modifier.fillMaxWidth().weight(1f), command, "$title up")
        Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(21.dp), tint = Ink.Text)
            Spacer(Modifier.height(6.dp))
            Text(title, color = Ink.Text.copy(alpha = .6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        HoldDirection(down, Symbols.Minus, Modifier.fillMaxWidth().weight(1f), command, "$title down")
    }
}

@Composable
internal fun HoldDirection(key: String, icon: ImageVector, modifier: Modifier, command: (String) -> Unit,
    label: String = "Move $key") {
    var pressed by remember { mutableStateOf(false) }
    val currentCommand by rememberUpdatedState(command)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val alpha by animateFloatAsState(if (pressed) .1f else 0f, Motion.press(), label = "direction press")
    val scale by animateFloatAsState(if (pressed) .96f else 1f, Motion.press(), label = "rocker press")
    Box(modifier.graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(24.dp)).background(Color.Black.copy(alpha = alpha))
        .pointerInput(key) {
            awaitEachGesture {
                val down = awaitFirstDown()
                pressed = true
                currentCommand(key)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val repeat = scope.launch {
                    delay(380)
                    while (true) { currentCommand(key); delay(110) }
                }
                try {
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (change.position.x !in 0f..size.width.toFloat() || change.position.y !in 0f..size.height.toFloat()) break
                        change.consume()
                    }
                } finally { pressed = false; repeat.cancel() }
            }
        }.semantics {
            role = Role.Button
            contentDescription = label
            onClick { currentCommand(key); true }
        }, contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(30.dp), tint = Ink.Text)
    }
}
