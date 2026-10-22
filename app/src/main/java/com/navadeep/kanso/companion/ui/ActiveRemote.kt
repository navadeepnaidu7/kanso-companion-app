package com.navadeep.kanso.companion.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.navadeep.kanso.companion.data.*

@Composable
internal fun DeviceTitle(state: CompanionState, modifier: Modifier = Modifier, open: () -> Unit) {
    val activeApp = state.apps.find { it.id == state.activeAppId && it.id != "com.navadeep.kanso.tv" }
    val name = when {
        state.preview -> "Preview"
        state.tvName.startsWith("BRAVIA", true) -> "Bravia"
        state.tvName == "Connect" -> "Your TV"
        else -> state.tvName.replace('_', ' ')
    }
    Row(modifier.height(54.dp).clip(CircleShape).clickable(role = Role.Button, onClick = open)
        .padding(horizontal = 16.dp).semantics { contentDescription = if (state.connected) "Connected to ${state.tvName}" else "Connect to TV"
            state.apps.find { it.id == state.activeAppId && it.id != "com.navadeep.kanso.tv" }?.let { stateDescription = "Controlling ${it.name}" } },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        AnimatedContent(activeApp, transitionSpec = {
            fadeIn(Motion.settle()) togetherWith fadeOut(Motion.press())
        }, contentKey = { it?.id }, label = "active app mark") { app ->
            if (app != null) Row(Modifier.padding(end = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                AppArtworkView(app, state.preview, 24.dp)
            }
        }
        Text(name, color = Ink.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.4).sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(4.dp).background(if (state.connected) Ink.Connected else Ink.Muted, CircleShape))
        Spacer(Modifier.width(10.dp))
        Icon(Symbols.Down, null, Modifier.size(19.dp), tint = Ink.Muted)
    }
}

@Composable
internal fun PadMediaControls(state: CompanionState, model: CompanionViewModel) {
    val profile = RemoteProfile.forApp(state.apps.find { it.id == state.activeAppId })
    AnimatedVisibility(profile != RemoteProfile.Navigation && (state.preview || state.receiverVersion >= 2),
        enter = fadeIn(Motion.panel()) + slideInVertically(Motion.panel()) { it / 3 }, exit = fadeOut(Motion.press())) {
        Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 18.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SoftButton(if (profile == RemoteProfile.Audio) "Previous track" else "Rewind",
                if (profile == RemoteProfile.Audio) Symbols.Previous else Symbols.Rewind, Modifier.size(48.dp), plain = true) {
                model.command(if (profile == RemoteProfile.Audio) "previous" else "rewind")
            }
            SoftButton(if (profile == RemoteProfile.Audio) "Next track" else "Fast forward",
                if (profile == RemoteProfile.Audio) Symbols.Next else Symbols.Forward, Modifier.size(48.dp), plain = true) {
                model.command(if (profile == RemoteProfile.Audio) "next" else "forward")
            }
        }
    }
}

@Composable
internal fun ActionCluster(state: CompanionState, model: CompanionViewModel, channels: Boolean,
    modifier: Modifier = Modifier, compact: Boolean = false) {
    val side = if (compact) 48.dp else 56.dp
    Row(modifier.height(132.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SoftButton("Home", Symbols.Home, Modifier.size(side)) { model.command("home") }
            SoftButton("Play or pause", Symbols.PlayPause, Modifier.size(side)) { model.command("play_pause") }
        }
        SoftButton("Back", Symbols.Back, Modifier.size(if (compact) 104.dp else 120.dp), large = true) { model.command("back") }
        Column(Modifier.width(if (compact) 50.dp else 58.dp).fillMaxHeight().controlSurface(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            HoldDirection(if (channels) "channel_up" else "volume_up", if (channels) Symbols.Up else Symbols.Plus,
                Modifier.weight(1f).fillMaxWidth(), model::command, if (channels) "Channel up" else "Volume up")
            Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
                if (channels) Text("CH", color = Ink.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                else SoftButton(when (state.muted) { true -> "Unmute"; false -> "Mute"; null -> "Mute / unmute" },
                    if (state.muted == true) Symbols.Mute else Symbols.Volume,
                    Modifier.size(44.dp).semantics {
                        stateDescription = when (state.muted) { true -> "Muted"; false -> "Sound on"; null -> "Mute state unavailable" }
                    }, plain = true, filled = state.muted == true) { model.command("volume_mute") }
            }
            HoldDirection(if (channels) "channel_down" else "volume_down", if (channels) Symbols.Down else Symbols.Minus,
                Modifier.weight(1f).fillMaxWidth(), model::command, if (channels) "Channel down" else "Volume down")
        }
    }
}
