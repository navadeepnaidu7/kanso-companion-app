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
    var apps by rememberSaveable { mutableStateOf(false) }
    var connect by rememberSaveable { mutableStateOf(false) }
    val libraryScroll = rememberLazyGridState()
    val launchMotion = remember { LaunchMotion() }
    val flight = remember { Animatable(0f) }
    var flying by remember { mutableStateOf(false) }
    val depth by animateFloatAsState(if (apps || connect) 1f else 0f, Motion.panel(), label = "remote depth")
    var handledLaunch by rememberSaveable { mutableIntStateOf(state.launchRevision) }
    LaunchedEffect(state.launchRevision) {
        if (handledLaunch != state.launchRevision) {
            handledLaunch = state.launchRevision
            if (launchMotion.app?.id == state.activeAppId) {
                flight.snapTo(0f)
                flying = true
                apps = false
                try { flight.animateTo(1f, Motion.settle()) } finally { flying = false }
            } else apps = false
        }
    }
    LaunchedEffect(state.activeAppId) {
        val id = state.activeAppId.orEmpty().lowercase()
        channels = listOf("jiotv", "livetv", "tuner").any { it in id }
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
    BackHandler(connect || apps) { if (connect) connect = false else apps = false }

    CompositionLocalProvider(LocalLaunchMotion provides launchMotion) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Ink.Background)) {
        val compact = maxHeight < 780.dp
        val landscape = maxWidth > maxHeight
        val accent = state.apps.find { it.id == state.activeAppId }?.let { Color(it.accent) } ?: Ink.Accent
        Column(Modifier.fillMaxSize().graphicsLayer {
            scaleX = 1f - depth * .055f; scaleY = 1f - depth * .055f
            alpha = 1f - depth * .55f
            if (Build.VERSION.SDK_INT >= 31) {
                val radius = depth * 12.dp.toPx()
                renderEffect = if (radius > .1f) android.graphics.RenderEffect.createBlurEffect(radius, radius,
                    android.graphics.Shader.TileMode.CLAMP).asComposeRenderEffect() else null
            }
        }.then(if (apps || connect) Modifier.clearAndSetSemantics { } else Modifier)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 28.dp).padding(bottom = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (landscape) {
                Row(Modifier.weight(1f).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    RemoteSurface(mode, Modifier.weight(1f).fillMaxHeight(), model::command, accent) { PadMediaControls(state, model) }
                    Column(Modifier.width(260.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        DeviceTitle(state, open = ::openConnection)
                        ModeSwitch(mode) { mode = it }
                        ActionCluster(state, model, channels, Modifier.fillMaxWidth().padding(top = 12.dp), compact = true) { channels = !channels }
                    }
                }
            } else {
                DeviceTitle(state, Modifier.padding(top = if (compact) 6.dp else 16.dp), ::openConnection)
                ActiveAppHeading(state)
                Row(Modifier.fillMaxWidth().height(62.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    SoftButton("Mute", Symbols.Mute, Modifier.size(44.dp), plain = true) { model.command("volume_mute") }
                    ModeSwitch(mode) { mode = it }
                    SoftButton(if (channels) "Volume buttons" else "Channel buttons", if (channels) Symbols.Volume else Symbols.Channels,
                        Modifier.size(44.dp), plain = true) { channels = !channels }
                }
                Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp, bottom = 18.dp)) {
                    RemoteSurface(mode, Modifier.fillMaxSize(), model::command, accent) { PadMediaControls(state, model) }
                }
                ActionCluster(state, model, channels, Modifier.fillMaxWidth().padding(horizontal = 2.dp).padding(bottom = 12.dp)) { channels = !channels }
            }
            FavoritesDock(state, model, Modifier.fillMaxWidth()) { apps = true }
        }

        // The library expands from its place in the floating navigation, and returns there.
        AnimatedVisibility(apps, enter = fadeIn(Motion.settle()) + scaleIn(Motion.panel(), initialScale = .22f,
            transformOrigin = TransformOrigin(.72f, 1f)),
            exit = fadeOut(Motion.settle()) + scaleOut(Motion.panel(), targetScale = .22f, transformOrigin = TransformOrigin(.72f, 1f))) {
            Column(Modifier.fillMaxSize().background(Ink.Surface).statusBarsPadding()) {
                Box(Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                    SoftButton("Close", Symbols.Back, Modifier.align(Alignment.CenterStart).size(44.dp), plain = true) { apps = false }
                    Text("Apps", color = Ink.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.5).sp)
                    SoftButton("Refresh apps", Symbols.Refresh, Modifier.align(Alignment.CenterEnd).size(44.dp), plain = true, onClick = model::refreshApps)
                }
                AppsPanel(state, model, libraryScroll, ::openConnection)
            }
        }
        FloatingNavigation(apps, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp)) { apps = it }
        if (flying) LaunchFlight(launchMotion, flight.value, state.preview)

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
                    onPreview = { model.preview(); connect = false; apps = false })
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
}
