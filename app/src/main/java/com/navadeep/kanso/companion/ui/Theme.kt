package com.navadeep.kanso.companion.ui

import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Ink {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF1C1C1C)
    val Raised = Color(0xFF30302F)
    val Text = Color(0xFFF5F5F7)
    val Muted = Color(0xFF9A9AA2)
    val Accent = Color(0xFFF5F5F7)
    val Connected = Color(0xFF95B9A4)
    val ControlTop = Color(0xFF30302E)
    val ControlBottom = Color(0xFF212120)
    val PadTop = Color(0xFF20201F)
    val PadBottom = Color(0xFF171717)
}

/** Mirrors the launcher: firm presses, soft panel travel, no theatrical bounce. */
object Motion {
    fun <T> press() = spring<T>(dampingRatio = 1f, stiffness = 1600f)
    fun <T> settle() = spring<T>(dampingRatio = 1f, stiffness = 440f)
    fun <T> panel() = spring<T>(dampingRatio = 1f, stiffness = 260f)
}

@Composable
fun KansoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ink.Accent, background = Ink.Background, surface = Ink.Surface,
            onBackground = Ink.Text, onSurface = Ink.Text, onSurfaceVariant = Ink.Muted,
            surfaceContainerHigh = Ink.Raised,
        ),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = (-.8).sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 25.sp, letterSpacing = (-.65).sp),
            titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
            labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp),
            labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp),
        ), content = content,
    )
}
