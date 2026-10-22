package com.navadeep.kanso.companion.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.navadeep.kanso.companion.data.CompanionViewModel

@Composable
fun CompanionScreen(model: CompanionViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var channels by rememberSaveable { mutableStateOf(false) }
    val library = rememberLibrarySheetState()
    var appButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var options by rememberSaveable { mutableStateOf(false) }
    var connect by rememberSaveable { mutableStateOf(false) }
    val libraryScroll = rememberLazyGridState()
    val modalDepth by animateFloatAsState(if (connect || options) 1f else 0f, Motion.panel(), label = "remote depth")
    var handledLaunch by rememberSaveable { mutableIntStateOf(state.launchRevision) }
    LaunchedEffect(state.launchRevision) {
        if (handledLaunch != state.launchRevision) {
            handledLaunch = state.launchRevision
            library.hide()
        }
    }
    val context = LocalContext.current
    var networkAllowed by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 37 || ContextCompat.checkSelfPermission(context,
            "android.permission.ACCESS_LOCAL_NETWORK") == PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        networkAllowed = it
        if (it) { model.startDiscovery(); model.resume() }
    }
    fun openConnection() {
        connect = true
        if (!networkAllowed && Build.VERSION.SDK_INT >= 37) permission.launch("android.permission.ACCESS_LOCAL_NETWORK")
    }
    LaunchedEffect(connect, networkAllowed) {
        if (connect && networkAllowed) model.startDiscovery() else model.stopDiscovery()
    }
    LaunchedEffect(state.connected) { if (state.connected) connect = false }
    DisposableEffect(Unit) { onDispose { model.stopDiscovery() } }
    BackHandler(connect || options || library.visible) { if (connect) connect = false else if (options) options = false else library.hide() }

    BoxWithConstraints(Modifier.fillMaxSize().background(Ink.Background)) {
        val compact = maxHeight < 780.dp
        val landscape = maxWidth > maxHeight
        val accent = state.apps.find { it.id == state.activeAppId }?.let { Color(it.accent) } ?: Ink.Accent
        Column(Modifier.fillMaxSize().graphicsLayer {
            val depth = maxOf(modalDepth, library.progress.value.coerceIn(0f, 1f))
            scaleX = 1f - depth * .035f; scaleY = 1f - depth * .035f
            alpha = 1f - depth * .4f
            if (Build.VERSION.SDK_INT >= 31) {
                val radius = depth * 12.dp.toPx()
                renderEffect = if (radius > .1f) android.graphics.RenderEffect.createBlurEffect(radius, radius,
                    android.graphics.Shader.TileMode.CLAMP).asComposeRenderEffect() else null
            }
        }.then(if (library.visible || connect || options) Modifier.clearAndSetSemantics { } else Modifier)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 28.dp).padding(bottom = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (landscape) {
                Row(Modifier.weight(1f).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    RemoteSurface(mode, Modifier.weight(1f).fillMaxHeight(), model::command, accent) { PadMediaControls(state, model) }
                    Column(Modifier.width(260.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        DeviceTitle(state, open = ::openConnection)
                        SoftButton("Options", Symbols.More, Modifier.size(44.dp), plain = true) { options = true }
                        ModeSwitch(mode) { mode = it }
                        ActionCluster(state, model, channels, Modifier.fillMaxWidth().padding(top = 12.dp), compact = true)
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().padding(top = if (compact) 6.dp else 16.dp), contentAlignment = Alignment.Center) {
                    DeviceTitle(state, Modifier.padding(horizontal = 44.dp), open = ::openConnection)
                    SoftButton("Options", Symbols.More, Modifier.align(Alignment.CenterEnd).size(44.dp), plain = true) { options = true }
                }
                Box(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp, bottom = 20.dp)) {
                    RemoteSurface(mode, Modifier.fillMaxSize(), model::command, accent) { PadMediaControls(state, model) }
                    SoftButton(if (mode == 0) "Use buttons" else "Use trackpad", if (mode == 0) Symbols.DirectionPad else Symbols.Trackpad,
                        Modifier.align(Alignment.TopEnd).padding(12.dp).size(44.dp), plain = true) { mode = 1 - mode }
                }
                ActionCluster(state, model, channels, Modifier.widthIn(max = 328.dp).fillMaxWidth().padding(bottom = 8.dp))
            }

        }

        if (library.visible) Box(Modifier.fillMaxSize().clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { library.hide() })
        LibrarySheet(library, appButtonBounds) { dragHeader ->
            AppsPanel(state, model, libraryScroll, dragHeader, close = library::hide, connect = ::openConnection)
        }
        AppNavigation(library.isOpen, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp)
            .onGloballyPositioned { appButtonBounds = it.boundsInRoot() }) { if (it) library.show() else library.hide() }

        AnimatedVisibility(options, enter = fadeIn(Motion.panel()), exit = fadeOut(Motion.settle())) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).clickable { options = false })
        }
        AnimatedVisibility(options, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(Motion.panel()) { it } + fadeIn(Motion.settle()),
            exit = slideOutVertically(Motion.panel()) { it } + fadeOut(Motion.settle())) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).background(Ink.Surface)
                .navigationBarsPadding().padding(24.dp).semantics { paneTitle = "Options" }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Options", color = Ink.Text, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                    SoftButton("Close options", Icons.Outlined.Close, Modifier.size(44.dp), plain = true) { options = false }
                }
                TextButton(onClick = { model.command("volume_mute"); options = false }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Symbols.Mute, null, Modifier.size(22.dp), tint = Ink.Text)
                    Spacer(Modifier.width(12.dp)); Text("Mute / unmute", color = Ink.Text)
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(false to "Volume", true to "Channels").forEach { (value, title) ->
                        FilterChip(selected = channels == value, onClick = { channels = value; options = false },
                            label = { Text(title) }, modifier = Modifier.heightIn(min = 48.dp),
                            colors = FilterChipDefaults.filterChipColors(containerColor = Ink.Surface, labelColor = Ink.Muted,
                                selectedContainerColor = Ink.Raised, selectedLabelColor = Ink.Text))
                    }
                }
            }
        }
        AnimatedVisibility(connect, enter = fadeIn(Motion.panel()), exit = fadeOut(Motion.settle())) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).clickable { connect = false })
        }
        AnimatedVisibility(connect, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(Motion.panel()) { it } + fadeIn(Motion.settle()),
            exit = slideOutVertically(Motion.panel()) { it } + fadeOut(Motion.settle())) {
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight * .9f)
                .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)).background(Ink.Surface)
                .navigationBarsPadding().semantics { paneTitle = "Connect" }) {
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.End) {
                    SoftButton("Close", Icons.Outlined.Close, Modifier.size(40.dp)) { connect = false }
                }
                Text(if (state.selectedDevice != null) "Pair TV" else "Your TV", color = Ink.Text,
                    fontSize = 25.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 28.dp, bottom = 24.dp))
                ConnectionPanel(state, networkAllowed, model,
                    requestPermission = { permission.launch("android.permission.ACCESS_LOCAL_NETWORK") },
                    onPreview = { model.preview(); connect = false; library.hide() })
            }
        }
        AnimatedVisibility(state.notice != null, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 14.dp),
            enter = fadeIn(Motion.settle()) + slideInVertically(Motion.settle()) { -it / 2 }, exit = fadeOut(Motion.settle())) {
            Text(state.notice.orEmpty(), color = Ink.Text, fontSize = 14.sp,
                modifier = Modifier.clip(CircleShape).background(Ink.Raised).padding(horizontal = 20.dp, vertical = 13.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite })
        }
    }
}
