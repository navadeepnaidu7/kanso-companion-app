package com.navadeep.kanso.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.navadeep.kanso.companion.data.CompanionViewModel
import com.navadeep.kanso.companion.ui.CompanionScreen
import com.navadeep.kanso.companion.ui.KansoTheme

class MainActivity : ComponentActivity() {
    private val model by viewModels<CompanionViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent { KansoTheme { CompanionScreen(model) } }
    }
    override fun onStart() { super.onStart(); model.resume() }
    override fun onStop() { model.suspendConnection(); super.onStop() }
}
