package com.kampplus.hava.ui.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * Sıcaklık → renk skalası: buz mavisinden kızıla.
 *
 * Grafikteki eğri ve haftalık aralık çubukları aynı skalayı kullanır; böylece
 * "turuncu = sıcak" bilgisi ekranın her yerinde tutarlıdır.
 */
private val STOPS = listOf(
    -10.0 to Color(0xFF8AB6FF),
    0.0 to Color(0xFF8FDCFF),
    10.0 to Color(0xFF7FE3C0),
    20.0 to Color(0xFFFFD36B),
    30.0 to Color(0xFFFF9447),
    40.0 to Color(0xFFFF5252)
)

/** Yağış olasılığı her yerde aynı açık maviyle gösterilir. */
val PrecipitationColor = Color(0xFF9AD4FF)

fun temperatureColor(celsius: Double): Color {
    if (celsius <= STOPS.first().first) return STOPS.first().second
    if (celsius >= STOPS.last().first) return STOPS.last().second
    val upper = STOPS.indexOfFirst { it.first >= celsius }
    val (t0, c0) = STOPS[upper - 1]
    val (t1, c1) = STOPS[upper]
    return lerp(c0, c1, ((celsius - t0) / (t1 - t0)).toFloat())
}

/**
 * Bir günün en düşük–en yüksek aralığını, haftanın tamamına göre ölçekli gösteren çubuk.
 *
 * Bütün satırlar aynı [weekMin]..[weekMax] ölçeğini kullandığı için çubuklar alt alta
 * okununca haftanın gidişatı (ısınma, soğuma) tek bakışta görülür.
 *
 * @param current Yalnızca bugün için verilir; şu anki sıcaklık çubukta bir nokta olur.
 */
@Composable
fun TemperatureRangeBar(
    min: Double,
    max: Double,
    weekMin: Double,
    weekMax: Double,
    modifier: Modifier = Modifier,
    current: Double? = null
) {
    val span = (weekMax - weekMin).coerceAtLeast(1.0)
    fun fraction(value: Double) = ((value - weekMin) / span).toFloat().coerceIn(0f, 1f)

    Canvas(modifier = modifier.height(6.dp)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(color = Color.White.copy(alpha = 0.18f), cornerRadius = radius)

        val start = fraction(min) * size.width
        val end = (fraction(max) * size.width).coerceAtLeast(start + size.height)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(temperatureColor(min), temperatureColor(max)),
                startX = start,
                endX = end
            ),
            topLeft = Offset(start, 0f),
            size = Size(end - start, size.height),
            cornerRadius = radius
        )
        current?.let {
            val center = Offset(fraction(it) * size.width, size.height / 2)
            drawCircle(Color.Black.copy(alpha = 0.35f), radius = size.height * 0.95f, center = center)
            drawCircle(Color.White, radius = size.height * 0.7f, center = center)
        }
    }
}
