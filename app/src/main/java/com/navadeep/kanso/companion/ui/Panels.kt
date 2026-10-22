package com.navadeep.kanso.companion.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.navadeep.kanso.companion.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Only used in the explicitly selected preview; connected TVs supply their own icons. */
@Composable
internal fun PreviewIcon(id: String) {
    val background = when (id) {
        "youtube" -> Color(0xFF272429); "netflix" -> Color(0xFF191A20)
        "prime" -> Color(0xFF17486C); "spotify" -> Color(0xFF173B2C)
        "plex" -> Color(0xFF27272C); "disney" -> Color(0xFF18264F)
        "apple" -> Color(0xFF272B31); "vlc" -> Color(0xFF433024)
        else -> Color(0xFF2C3440)
    }
    Box(Modifier.fillMaxSize().background(background), contentAlignment = Alignment.Center) {
        when (id) {
            "netflix" -> Text("N", color = Color(0xFFE54450), fontWeight = FontWeight.Black, fontSize = 43.sp)
            "prime" -> Text("prime", color = Color(0xFFC8E8FF), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            "disney" -> Text("Disney+", color = Color(0xFFE4ECFF), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            "apple" -> Text("tv", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Medium)
            "settings" -> Icon(Icons.Outlined.Settings, null, Modifier.size(37.dp), tint = Color(0xFFCED8E8))
            "vlc" -> Canvas(Modifier.size(40.dp)) {
                val path = Path().apply { moveTo(size.width / 2, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
                drawPath(path, Color(0xFFF5A558))
                drawLine(Color.White, Offset(size.width * .25f, size.height * .68f), Offset(size.width * .75f, size.height * .68f), size.height * .13f)
            }
            "plex" -> Canvas(Modifier.size(35.dp, 42.dp)) {
                val p = Path().apply { moveTo(size.width * .18f, 0f); lineTo(size.width * .64f, 0f); lineTo(size.width, size.height / 2); lineTo(size.width * .64f, size.height); lineTo(size.width * .18f, size.height); lineTo(size.width * .55f, size.height / 2); close() }
                drawPath(p, Color(0xFFE8B550))
            }
            "spotify" -> Canvas(Modifier.size(43.dp)) {
                drawCircle(Color(0xFF75D39B))
                repeat(3) { row ->
                    val y = size.height * (.34f + row * .17f)
                    val p = Path().apply {
                        moveTo(size.width * .23f, y)
                        quadraticTo(size.width * .52f, y - size.height * .13f, size.width * .8f, y + size.height * .025f)
                    }
                    drawPath(p, Color(0xFF173B2C), style = Stroke(size.width * .065f, cap = StrokeCap.Round))
                }
            }
            else -> Box(Modifier.size(46.dp, 33.dp).clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE85F64)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.PlayArrow, null, Modifier.size(26.dp), tint = Color.White)
            }
        }
    }
}

@Composable
fun ConnectionPanel(
    state: CompanionState,
    networkAllowed: Boolean,
    model: CompanionViewModel,
    requestPermission: () -> Unit,
    onPreview: () -> Unit,
) {
    var address by rememberSaveable { mutableStateOf("") }
    var manual by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable(state.selectedDevice?.host) { mutableStateOf("") }
    var addressError by remember { mutableStateOf(false) }
    val device = state.selectedDevice
    Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
        .padding(start = 28.dp, end = 28.dp, bottom = 26.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        when {
            !networkAllowed -> {
                Text("Allow nearby connections to find your TV.", color = Ink.Muted, style = MaterialTheme.typography.bodyMedium)
                ActionButton("Allow", onClick = requestPermission)
            }
            device != null -> {
                Text(device.name, color = Ink.Muted, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6); model.clearError() },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "TV pairing code" },
                    label = { Text("Code on your TV") }, shape = RoundedCornerShape(18.dp),
                    singleLine = true, enabled = !state.busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (code.length == 6) model.pair(code) }))
                ActionButton(if (state.busy) "Pairing" else "Pair", enabled = code.length == 6 && !state.busy) { model.pair(code) }
                TextButton(onClick = model::unselect, enabled = !state.busy) { Text("Choose another TV") }
            }
            else -> {
                if (state.connected) {
                    DeviceRow(state.tvName, connected = true) { }
                    TextButton(onClick = model::forget) { Text("Forget TV") }
                } else {
                    if (state.devices.isEmpty()) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Ink.Muted, strokeWidth = 1.5.dp)
                            Spacer(Modifier.width(14.dp))
                            Text("Looking for your TV", color = Ink.Muted, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else state.devices.forEach { found -> DeviceRow(found.name) { model.select(found) } }
                    if (manual) {
                        OutlinedTextField(address, { address = it; addressError = false },
                            modifier = Modifier.fillMaxWidth(), label = { Text("TV address") },
                            placeholder = { Text("192.168.1.20") }, singleLine = true,
                            shape = RoundedCornerShape(18.dp), isError = addressError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                parseAddress(address)?.let(model::select) ?: run { addressError = true }
                            }))
                        ActionButton("Continue", enabled = address.isNotBlank()) {
                            parseAddress(address)?.let(model::select) ?: run { addressError = true }
                        }
                    } else TextButton(onClick = { manual = true }) { Text("Enter TV address") }
                    Text("Open Kanso Receiver on your TV to pair.", color = Ink.Muted, style = MaterialTheme.typography.bodyMedium)
                }
                if (!state.connected) TextButton(onClick = onPreview) { Text("Try preview") }
            }
        }
        if (state.error != null) Text(state.error, color = Color(0xFFE3A2A6), style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { contentDescription = state.error })
    }
}

private fun parseAddress(value: String): TvDevice? = runCatching {
    val uri = java.net.URI("tls://${value.trim()}")
    val host = requireNotNull(uri.host)
    require(uri.rawUserInfo == null && (uri.path.isNullOrEmpty()) && uri.query == null && uri.fragment == null)
    val port = if (uri.port == -1) 9876 else uri.port
    require(port in 1..65535)
    TvDevice("TV", host, port)
}.getOrNull()

@Composable
private fun DeviceRow(name: String, connected: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Ink.Raised)
        .clickable(role = Role.Button, onClick = onClick).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Tv, null, tint = Ink.Accent)
        Spacer(Modifier.width(14.dp))
        Text(name, color = Ink.Text, modifier = Modifier.weight(1f))
        Icon(if (connected) Icons.Outlined.Check else Icons.Outlined.ChevronRight, null,
            Modifier.size(20.dp), tint = if (connected) Ink.Connected else Ink.Muted)
    }
}

@Composable
fun ActionButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Ink.Accent, contentColor = Ink.Background),
        contentPadding = PaddingValues(horizontal = 26.dp, vertical = 14.dp)) { Text(text) }
}
