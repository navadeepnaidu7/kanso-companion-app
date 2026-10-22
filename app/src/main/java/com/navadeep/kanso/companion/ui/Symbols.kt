package com.navadeep.kanso.companion.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A small, consistent stroke family for the controls used most often. */
object Symbols {
    private fun stroke(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = block)
        }.build()

    val Plus by lazy { stroke("Plus") { moveTo(6f, 12f); lineTo(18f, 12f); moveTo(12f, 6f); lineTo(12f, 18f) } }
    val More by lazy { stroke("More") {
        moveTo(5f, 12f); lineTo(5.2f, 12f); moveTo(12f, 12f); lineTo(12.2f, 12f); moveTo(19f, 12f); lineTo(19.2f, 12f)
    } }
    val DirectionPad by lazy { stroke("DirectionPad") {
        moveTo(9f, 6f); lineTo(12f, 3f); lineTo(15f, 6f)
        moveTo(9f, 18f); lineTo(12f, 21f); lineTo(15f, 18f)
        moveTo(6f, 9f); lineTo(3f, 12f); lineTo(6f, 15f)
        moveTo(18f, 9f); lineTo(21f, 12f); lineTo(18f, 15f)
    } }
    val Trackpad by lazy { stroke("Trackpad") {
        moveTo(5f, 4f); lineTo(19f, 4f); lineTo(19f, 20f); lineTo(5f, 20f); close()
        moveTo(10f, 12f); lineTo(14f, 12f); moveTo(12f, 10f); lineTo(12f, 14f)
    } }
    val Remote by lazy { stroke("Remote") {
        moveTo(9f, 2f); lineTo(15f, 2f); quadTo(17f, 2f, 17f, 4f); lineTo(17f, 20f)
        quadTo(17f, 22f, 15f, 22f); lineTo(9f, 22f); quadTo(7f, 22f, 7f, 20f)
        lineTo(7f, 4f); quadTo(7f, 2f, 9f, 2f); close()
        moveTo(10f, 7f); lineTo(14f, 7f); moveTo(10f, 12f); lineTo(10f, 13f)
        moveTo(14f, 12f); lineTo(14f, 13f); moveTo(10f, 17f); lineTo(10f, 18f)
        moveTo(14f, 17f); lineTo(14f, 18f)
    } }
    val Refresh by lazy { stroke("Refresh") {
        moveTo(19f, 7f); curveTo(16f, 2f, 8f, 2f, 5f, 8f); curveTo(1f, 16f, 11f, 23f, 18f, 17f)
        moveTo(19f, 3f); lineTo(19f, 8f); lineTo(14f, 8f)
    } }
    val Minus by lazy { stroke("Minus") { moveTo(6f, 12f); lineTo(18f, 12f) } }
    val Channels by lazy { stroke("Channels") {
        moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 17f); lineTo(4f, 17f); close()
        moveTo(9f, 21f); lineTo(15f, 21f); moveTo(12f, 17f); lineTo(12f, 21f)
    } }

    val Launch by lazy { stroke("Launch") {
        moveTo(6f, 18f); lineTo(18f, 6f); moveTo(7f, 6f); lineTo(18f, 6f); lineTo(18f, 17f)
    } }
    val Up by lazy { stroke("Up") { moveTo(7f, 14f); lineTo(12f, 9f); lineTo(17f, 14f) } }
    val Down by lazy { stroke("Down") { moveTo(7f, 10f); lineTo(12f, 15f); lineTo(17f, 10f) } }
    val Left by lazy { stroke("Left") { moveTo(14f, 7f); lineTo(9f, 12f); lineTo(14f, 17f) } }
    val Right by lazy { stroke("Right") { moveTo(10f, 7f); lineTo(15f, 12f); lineTo(10f, 17f) } }
    val VolumeDown by lazy { stroke("VolumeDown") {
        moveTo(3f, 9f); lineTo(6f, 9f); lineTo(11f, 5f); lineTo(11f, 19f); lineTo(6f, 15f); lineTo(3f, 15f); close()
        moveTo(16f, 12f); lineTo(22f, 12f)
    } }
    val Mute by lazy { stroke("Mute") {
        moveTo(3f, 9f); lineTo(6f, 9f); lineTo(11f, 5f); lineTo(11f, 19f); lineTo(6f, 15f); lineTo(3f, 15f); close()
        moveTo(16f, 9f); lineTo(22f, 15f); moveTo(22f, 9f); lineTo(16f, 15f)
    } }
    val Volume by lazy { stroke("Volume") {
        moveTo(3f, 9f); lineTo(6f, 9f); lineTo(11f, 5f); lineTo(11f, 19f); lineTo(6f, 15f); lineTo(3f, 15f); close()
        moveTo(16f, 8f); curveTo(19f, 10f, 19f, 14f, 16f, 16f)
    } }
    val VolumeUp by lazy { stroke("VolumeUp") {
        moveTo(3f, 9f); lineTo(6f, 9f); lineTo(11f, 5f); lineTo(11f, 19f); lineTo(6f, 15f); lineTo(3f, 15f); close()
        moveTo(16f, 12f); lineTo(22f, 12f); moveTo(19f, 9f); lineTo(19f, 15f)
    } }
    val Forward by lazy { stroke("Forward") {
        moveTo(3f, 6f); lineTo(11f, 12f); lineTo(3f, 18f); close()
        moveTo(13f, 6f); lineTo(21f, 12f); lineTo(13f, 18f); close()
    } }
    val Rewind by lazy { stroke("Rewind") {
        moveTo(21f, 6f); lineTo(13f, 12f); lineTo(21f, 18f); close()
        moveTo(11f, 6f); lineTo(3f, 12f); lineTo(11f, 18f); close()
    } }
    val Next by lazy { stroke("Next") {
        moveTo(5f, 5f); lineTo(16f, 12f); lineTo(5f, 19f); close(); moveTo(20f, 5f); lineTo(20f, 19f)
    } }
    val Previous by lazy { stroke("Previous") {
        moveTo(19f, 5f); lineTo(8f, 12f); lineTo(19f, 19f); close(); moveTo(4f, 5f); lineTo(4f, 19f)
    } }
    val Back by lazy {
        ImageVector.Builder("Back", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(14.5f, 5.5f); lineTo(8f, 12f); lineTo(14.5f, 18.5f)
            }
        }.build()
    }
    val Home by lazy {
        ImageVector.Builder("Home", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 1.65f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3.5f, 10.5f); lineTo(12f, 3.8f); lineTo(20.5f, 10.5f)
                moveTo(5.5f, 9f); lineTo(5.5f, 19.5f); lineTo(9.7f, 19.5f)
                lineTo(9.7f, 13.8f); lineTo(14.3f, 13.8f); lineTo(14.3f, 19.5f)
                lineTo(18.5f, 19.5f); lineTo(18.5f, 9f)
            }
        }.build()
    }
    val PlayPause by lazy {
        ImageVector.Builder("PlayPause", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(3f, 5.7f); lineTo(12f, 12f); lineTo(3f, 18.3f); close()
            }
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round) {
                moveTo(16f, 6.5f); lineTo(16f, 17.5f); moveTo(21f, 6.5f); lineTo(21f, 17.5f)
            }
        }.build()
    }
    val Apps by lazy {
        ImageVector.Builder("Apps", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 1.6f, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 3f); lineTo(9f, 3f); lineTo(9f, 9f); lineTo(3f, 9f); close()
                moveTo(15f, 3f); lineTo(21f, 3f); lineTo(21f, 9f); lineTo(15f, 9f); close()
                moveTo(3f, 15f); lineTo(9f, 15f); lineTo(9f, 21f); lineTo(3f, 21f); close()
                moveTo(15f, 15f); lineTo(21f, 15f); lineTo(21f, 21f); lineTo(15f, 21f); close()
            }
        }.build()
    }
}
