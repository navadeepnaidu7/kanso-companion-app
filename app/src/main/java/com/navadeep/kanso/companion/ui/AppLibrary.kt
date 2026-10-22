package com.navadeep.kanso.companion.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.navadeep.kanso.companion.data.*

@Composable
fun FavoritesDock(state: CompanionState, model: CompanionViewModel, modifier: Modifier = Modifier, openApps: () -> Unit) {
    val apps = remember(state.apps, state.dockIds) { state.dockIds.mapNotNull { id -> state.apps.find { it.id == id } } }
    Row(modifier.heightIn(min = 56.dp).padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        apps.forEach { app ->
            AppTile(app, state, model, size = 46.dp, label = false, modifier = Modifier.width(70.dp))
        }
    }
}

@Composable
fun AppsPanel(state: CompanionState, model: CompanionViewModel, gridState: LazyGridState, connect: () -> Unit) {
    Box(Modifier.fillMaxWidth().fillMaxHeight()) {
    when {
        state.loadingApps && state.apps.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(24.dp), color = Ink.Accent, strokeWidth = 2.dp)
        }
        !state.connected && !state.preview && state.apps.isEmpty() -> Column(Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Outlined.Tv, null, Modifier.size(36.dp), tint = Ink.Muted)
            Spacer(Modifier.height(24.dp))
            ActionButton("Connect your TV", onClick = connect)
        }
        state.apps.isEmpty() -> Column(Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(if (state.error != null) "Couldn't load apps" else "No apps found", color = Ink.Muted)
            Spacer(Modifier.height(18.dp))
            ActionButton("Try again", onClick = model::refreshApps)
        }
        else -> AppLibrary(state, model, gridState, connect)
    }
    }
}

@Composable
private fun AppLibrary(state: CompanionState, model: CompanionViewModel, gridState: LazyGridState, connect: () -> Unit) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(searchOpen) { if (searchOpen) searchFocus.requestFocus() }
    val dock = remember(state.apps, state.dockIds) { state.dockIds.mapNotNull { id -> state.apps.find { it.id == id } } }
    val filtered = remember(state.apps, query) { state.apps.filter { it.name.contains(query.trim(), ignoreCase = true) } }
    Column(Modifier.fillMaxWidth().fillMaxHeight().imePadding()) {
        Row(Modifier.fillMaxWidth().height(44.dp).padding(start = 22.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (searchOpen) {
                BasicTextField(query, { query = it }, modifier = Modifier.weight(1f).focusRequester(searchFocus).semantics { contentDescription = "Search apps" },
                    singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink.Text),
                    cursorBrush = SolidColor(Ink.Text), decorationBox = { field ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Search, null, Modifier.size(19.dp), tint = Ink.Muted)
                            Spacer(Modifier.width(10.dp))
                            Box { if (query.isEmpty()) Text("Search apps", color = Ink.Muted, style = MaterialTheme.typography.bodyLarge); field() }
                        }
                    })
                TextButton(onClick = { keyboard?.hide(); focusManager.clearFocus(); query = ""; searchOpen = false }) { Text("Done", color = Ink.Text) }
            } else {
                Text(if (!state.connected && !state.preview) "Saved apps" else if (state.preview) "Preview" else "", color = Ink.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                if (!state.connected && !state.preview) TextButton(onClick = connect) { Text("Connect", color = Ink.Accent) }
                SoftButton("Search apps", Icons.Outlined.Search, Modifier.size(44.dp), plain = true) { searchOpen = true }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (filtered.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No apps found", color = Ink.Muted, style = MaterialTheme.typography.bodyMedium)
            }
            LazyVerticalGrid(columns = GridCells.Adaptive(155.dp), state = gridState,
                modifier = Modifier.fillMaxSize().semantics { contentDescription = "App library" },
                contentPadding = PaddingValues(start = 23.dp, end = 23.dp, top = 8.dp, bottom = 110.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                items(filtered, key = { it.id }, contentType = { "app" }) { app ->
                    AppTile(app, state, model, size = 58.dp, wide = true,
                        modifier = Modifier.animateItem(placementSpec = Motion.settle()))
                }
            }
            // Edge veils keep partial rows legible without a hard clipping line.
            val atTop by remember { derivedStateOf { !gridState.canScrollBackward } }
            if (!atTop) Box(Modifier.fillMaxWidth().height(12.dp).background(Brush.verticalGradient(listOf(Ink.Surface, Color.Transparent))))
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(22.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Ink.Surface))))
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AppTile(app: TvApp, state: CompanionState, model: CompanionViewModel, size: Dp,
    modifier: Modifier = Modifier, label: Boolean = true, wide: Boolean = false) {
    val source = remember { MutableInteractionSource() }
    val launchMotion = LocalLaunchMotion.current
    var artworkBounds by remember { mutableStateOf(Rect.Zero) }
    val pressed = source.collectIsPressedAsState()
    val opening = state.launchingId == app.id
    val active = state.activeAppId == app.id
    val scale = animateFloatAsState(if (pressed.value || opening) { if (wide) .965f else .93f } else 1f, Motion.press(), label = "app press")
    val haptics = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }
    var replacing by remember { mutableStateOf(false) }
    val pinnedIndex = state.dockIds.indexOf(app.id)
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        Column(Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .widthIn(min = 48.dp).heightIn(min = 48.dp)
            .clip(RoundedCornerShape(if (wide) 0.dp else 17.dp))
            .combinedClickable(interactionSource = source, indication = null, role = Role.Button,
                onLongClickLabel = "Edit ${app.name} in dock", onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress); menu = true; replacing = false
                }, onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    launchMotion?.select(app, artworkBounds)
                    model.launch(app)
                }).semantics(mergeDescendants = true) {
                    contentDescription = "Open ${app.name} on TV"
                    if (opening) stateDescription = "Opening"
                    else if (active) stateDescription = "On TV"
                    customActions = if (pinnedIndex >= 0) listOf(
                        CustomAccessibilityAction("Remove from dock") { model.unpin(app); true },
                        CustomAccessibilityAction("Move left") { model.movePin(app, -1); true },
                        CustomAccessibilityAction("Move right") { model.movePin(app, 1); true },
                    ) else emptyList()
                }.padding(if (wide) 0.dp else 2.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Box(Modifier.onGloballyPositioned { artworkBounds = it.boundsInRoot() }.then(if (wide) Modifier.clip(RoundedCornerShape(22.dp))
                .border(.8.dp, if (active) Ink.Accent.copy(alpha = .7f) else Color.White.copy(alpha = .065f), RoundedCornerShape(22.dp)) else Modifier), contentAlignment = Alignment.Center) {
                if (wide) AppBannerView(app, state.preview, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                else AppArtworkView(app, state.preview, size)
                if (opening) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .45f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Ink.Text, strokeWidth = 2.dp)
                }
                if (pressed.value) Box(Modifier.matchParentSize().background(Color.White.copy(alpha = .06f)))
            }
            if (wide) {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center) {
                    if (active) {
                        Box(Modifier.size(4.dp).background(Ink.Accent, CircleShape))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(app.name, color = Ink.Text.copy(alpha = .9f), fontSize = 13.sp, fontWeight = FontWeight.Normal,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            } else if (label) {
                Spacer(Modifier.height(9.dp))
                Text(app.name, color = Ink.Text.copy(alpha = .86f), fontSize = 11.sp, lineHeight = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            }
        }
        DropdownMenu(menu, onDismissRequest = { menu = false },
            shape = RoundedCornerShape(21.dp), containerColor = Ink.Raised,
            tonalElevation = 0.dp, shadowElevation = 18.dp) {
            if (replacing) {
                state.dockIds.forEach { id -> state.apps.find { it.id == id }?.let { target ->
                    DropdownMenuItem(text = { Text(target.name, color = Ink.Text) },
                        leadingIcon = { AppArtworkView(target, state.preview, 28.dp) },
                        onClick = { model.pin(app, replace = id); menu = false })
                } }
            } else if (pinnedIndex >= 0) {
                if (pinnedIndex > 0) DropdownMenuItem(text = { Text("Move left") }, leadingIcon = { Icon(Icons.Outlined.ArrowBack, null) },
                    onClick = { model.movePin(app, -1); menu = false })
                if (pinnedIndex < state.dockIds.lastIndex) DropdownMenuItem(text = { Text("Move right") }, leadingIcon = { Icon(Icons.Outlined.ArrowForward, null) },
                    onClick = { model.movePin(app, 1); menu = false })
                DropdownMenuItem(text = { Text("Remove from dock") }, leadingIcon = { Icon(Icons.Outlined.RemoveCircleOutline, null) },
                    onClick = { model.unpin(app); menu = false })
            } else DropdownMenuItem(text = { Text(if (state.dockIds.size < AppArrangement.DockSize) "Add to dock" else "Replace in dock") },
                leadingIcon = { Icon(Icons.Outlined.AddCircleOutline, null) }, onClick = {
                    if (state.dockIds.size < AppArrangement.DockSize) { model.pin(app); menu = false } else replacing = true
                })
        }
    }
}

@Composable
internal fun AppArtworkView(app: TvApp, preview: Boolean, size: Dp) {
    val shape = RoundedCornerShape(size * .245f)
    Box(Modifier.size(size).shadow(4.dp, shape, ambientColor = Color.Black.copy(alpha = .18f), spotColor = Color.Black.copy(alpha = .18f))
        .clip(shape).background(Color(0xFF303137)), contentAlignment = Alignment.Center) {
        if (app.artwork != null) Image(app.artwork, null, Modifier.fillMaxSize())
        else if (preview) PreviewIcon(app.id)
        else Text(app.name.take(1), fontSize = 24.sp, color = Ink.Text)
    }
}

@Composable
internal fun AppBannerView(app: TvApp, preview: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.linearGradient(listOf(Color(app.accent).copy(alpha = .28f), Color(0xFF16171B)))),
        contentAlignment = Alignment.Center) {
        if (app.banner != null) Image(app.banner, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else {
            // Keep a missing TV banner intentional: large original artwork on a color-matched field.
            AppArtworkView(app, preview, 64.dp)
        }
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .035f), Color.Transparent, Color.Black.copy(alpha = .1f)))))
    }
}
