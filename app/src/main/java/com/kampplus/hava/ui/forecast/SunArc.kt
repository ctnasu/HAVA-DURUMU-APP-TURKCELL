package com.kampplus.hava.ui.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.cos
import kotlin.math.sin

/**
 * Güneşin gün içindeki yolu: gün doğumundan batımına uzanan bir yay ve
 * güneşin şu an bu yayın neresinde olduğu.
 */
@Composable
fun SunArc(now: LocalDateTime, sunrise: LocalDateTime, sunset: LocalDateTime, modifier: Modifier = Modifier) {
    val progress = sunProgress(now, sunrise, sunset)
    val isUp = progress in 0.0001f..0.9999f

    Canvas(modifier = modifier.height(96.dp)) {
        val pad = 12.dp.toPx()
        val horizon = size.height - 6.dp.toPx()
        val radiusX = size.width / 2 - pad
        val radiusY = horizon - pad
        val center = Offset(size.width / 2, horizon)
        val arcTopLeft = Offset(pad, pad)
        val arcSize = Size(radiusX * 2, radiusY * 2)

        drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, horizon), Offset(size.width, horizon), 1.dp.toPx())
        // Günün tamamı kesikli çizgi...
        drawArc(
            color = Color.White.copy(alpha = 0.35f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
        )
        // ...geçen kısmı ise dolu ve sıcak renkte.
        drawArc(
            brush = Brush.horizontalGradient(listOf(Color(0xFFFFB25B), Color(0xFFFFE08A))),
            startAngle = 180f,
            sweepAngle = 180f * progress,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        val angle = Math.toRadians(180.0 + 180.0 * progress)
        val sun = Offset(
            x = center.x + radiusX * cos(angle).toFloat(),
            y = center.y + radiusY * sin(angle).toFloat()
        )
        val sunColor = if (isUp) Color(0xFFFFD36B) else Color.White.copy(alpha = 0.5f)
        drawCircle(
            brush = Brush.radialGradient(listOf(sunColor.copy(alpha = 0.5f), Color.Transparent), sun, 18.dp.toPx()),
            radius = 18.dp.toPx(),
            center = sun
        )
        drawCircle(sunColor, radius = 7.dp.toPx(), center = sun)
    }
}

/** 0 = gün doğumu, 1 = gün batımı; gece ise uca sabitlenir. */
fun sunProgress(now: LocalDateTime, sunrise: LocalDateTime, sunset: LocalDateTime): Float {
    val total = Duration.between(sunrise, sunset).toMinutes().coerceAtLeast(1)
    val elapsed = Duration.between(sunrise, now).toMinutes()
    return (elapsed.toFloat() / total).coerceIn(0f, 1f)
}
