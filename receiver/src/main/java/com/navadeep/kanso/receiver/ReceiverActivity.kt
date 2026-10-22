package com.navadeep.kanso.receiver

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

class ReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFB8CCF0))) {
                val state by RemoteService.state.collectAsStateWithLifecycle()
                var networkAllowed by remember { mutableStateOf(Build.VERSION.SDK_INT < 37 ||
                    ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_LOCAL_NETWORK") == PackageManager.PERMISSION_GRANTED) }
                val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { networkAllowed = it }
                val lifecycle = LocalLifecycleOwner.current.lifecycle
                LaunchedEffect(state.running, lifecycle) {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        if (state.running) {
                            try {
                                while (true) { RemoteService.instance?.showPairingCode(); delay(120000) }
                            } finally { RemoteService.instance?.hidePairingCode() }
                        }
                    }
                }
                Column(Modifier.fillMaxSize().background(Color(0xFF080A0F)).padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("Kanso", color = Color(0xFFF0F1F4), fontWeight = FontWeight.Light, fontSize = 36.sp)
                    Spacer(Modifier.height(28.dp))
                    when {
                        !networkAllowed -> Button(onClick = { permission.launch("android.permission.ACCESS_LOCAL_NETWORK") }) { Text("Allow local connections") }
                        !state.running -> {
                            Text(state.error ?: "Enable Kanso remote to connect your phone.", color = Color(0xFF919AA9))
                            Spacer(Modifier.height(20.dp))
                            Button(onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Open accessibility settings") }
                        }
                        state.code.isNotEmpty() -> {
                            Text(state.code.chunked(3).joinToString("  "), color = Color(0xFFF0F1F4), fontSize = 64.sp, fontWeight = FontWeight.Light, letterSpacing = 6.sp)
                            Spacer(Modifier.height(16.dp))
                            Text("Enter this code in Kanso on your phone.", color = Color(0xFF919AA9))
                        }
                        else -> Text("Paired", fontSize = 32.sp, color = Color(0xFFA7CFBC))
                    }
                    if (state.running) {
                        Spacer(Modifier.height(24.dp))
                        TextButton(onClick = { RemoteService.instance?.forgetPhones() }) { Text("Forget phones") }
                    }
                }
            }
        }
    }
    override fun onResume() { super.onResume(); RemoteService.instance?.showPairingCode() }
    override fun onStop() { RemoteService.instance?.hidePairingCode(); super.onStop() }
}
