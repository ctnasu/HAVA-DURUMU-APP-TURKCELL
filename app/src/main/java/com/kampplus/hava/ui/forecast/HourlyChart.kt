package com.kampplus.hava.ui.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kampplus.hava.model.HourlyForecast
import com.kampplus.hava.model.WeatherCondition
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Saatlik sıcaklık eğrisi + yağış olasılığı çubukları.
 *
 * Tamamı tek bir Canvas'ta çizilir: 24 ayrı kart yerine tek çizim, hem daha hızlı hem
 * de saatler arasında kesintisiz bir eğri çizmeye imkân verir.
 *
 * Etkileşim: dokununca o saat seçilir; basılı tutup kaydırınca seçim parmağı takip
 * eder ve her saat değişiminde hafif bir titreşim verilir.
 *
 * Seçim state'i dışarıda tutulur ([selected] / [onSelect]): zaman makinesinde
 * gökyüzü ve başlık da aynı seçimi izler.
 */
@Composable
fun HourlyChart(hours: List<HourlyForecast>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (hours.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val haptics = LocalHapticFeedback.current
    // pointerInput bloğu bir kez kurulur; en güncel değerleri okuyabilmesi için.
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val styles = ChartStyles(
        time = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.7f)),
        temperature = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.SemiBold),
        precipitation = MaterialTheme.typography.labelSmall.copy(color = PrecipitationColor)
    )

    val slotPx = with(LocalDensity.current) { SLOT_WIDTH.toPx() }
    fun select(x: Float) {
        val index = (x / slotPx).toInt().coerceIn(hours.indices)
        if (index != currentSelected) {
            currentOnSelect(index)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val selectedHour = hours[selected.coerceIn(hours.indices)]
    val description = "${timeLabel(hours, selected)}, ${selectedHour.temperatureC.roundToInt()} derece, " +
        WeatherCondition.fromWeatherCode(selectedHour.weatherCode).label

    Box(modifier = modifier.horizontalScroll(rememberScrollState())) {
        Canvas(
            modifier = Modifier
                .width(SLOT_WIDTH * hours.size)
                // Yağış yoksa çubuk alanı ayrılmaz; grafik altta boşluk bırakmaz.
                .height(if (hours.any { (it.precipitationProbability ?: 0) > 0 }) CHART_HEIGHT else CHART_HEIGHT_COMPACT)
                .pointerInput(hours) { detectTapGestures { select(it.x) } }
                .pointerInput(hours) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { select(it.x) },
                        onDrag = { change, _ -> select(change.position.x) }
                    )
                }
                .semantics { contentDescription = "Saatlik tahmin grafiği. Seçili: $description" }
        ) {
            drawChart(hours, selected, textMeasurer, styles)
        }
    }
}

private class ChartStyles(val time: TextStyle, val temperature: TextStyle, val precipitation: TextStyle)

private fun DrawScope.drawChart(hours: List<HourlyForecast>, selected: Int, textMeasurer: TextMeasurer, styles: ChartStyles) {
    val slot = SLOT_WIDTH.toPx()
    val curveTop = 84.dp.toPx()
    val curveBottom = 126.dp.toPx()
    val barBottom = size.height - 4.dp.toPx()
    val barMax = 26.dp.toPx()

    val minT = hours.minOf { it.temperatureC }
    val maxT = hours.maxOf { it.temperatureC }
    // En az 6°'lik ölçek: 1°'lik bir oynama ekranın tamamını kaplayıp abartılı görünmesin.
    val range = (maxT - minT).coerceAtLeast(MIN_RANGE)
    val base = minT - (range - (maxT - minT)) / 2
    val points = hours.mapIndexed { index, hour ->
        Offset(
            x = slot * (index + 0.5f),
            y = curveBottom - ((hour.temperatureC - base) / range).toFloat() * (curveBottom - curveTop)
        )
    }

    // Seçili saatin arka planı.
    drawRoundRect(
        color = Color.White.copy(alpha = 0.12f),
        topLeft = Offset(slot * selected + 3.dp.toPx(), 0f),
        size = Size(slot - 6.dp.toPx(), size.height),
        cornerRadius = CornerRadius(16.dp.toPx())
    )

    // Yumuşak eğri: her iki nokta arasında yatay teğetli kübik Bezier. Tepe ve
    // çukurlarda gerçek değerin üstüne/altına taşmaz (overshoot yok).
    val curve = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val previous = points[i - 1]
            val point = points[i]
            val dx = (point.x - previous.x) / 2
            cubicTo(previous.x + dx, previous.y, point.x - dx, point.y, point.x, point.y)
        }
    }
    val fillBottom = curveBottom + 18.dp.toPx()
    val fill = Path().apply {
        addPath(curve)
        lineTo(points.last().x, fillBottom)
        lineTo(points.first().x, fillBottom)
        close()
    }
    drawPath(
        path = fill,
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
            startY = curveTop,
            endY = fillBottom
        )
    )
    drawPath(
        path = curve,
        brush = if (hours.size > 1) {
            Brush.horizontalGradient(
                colors = hours.map { temperatureColor(it.temperatureC) },
                startX = points.first().x,
                endX = points.last().x
            )
        } else {
            Brush.linearGradient(listOf(Color.White, Color.White))
        },
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )

    hours.forEachIndexed { index, hour ->
        val point = points[index]
        val isSelected = index == selected

        drawCentered(textMeasurer, timeLabel(hours, index), point.x, 6.dp.toPx(), styles.time.boldIf(isSelected))
        drawCentered(
            textMeasurer,
            WeatherCondition.fromWeatherCode(hour.weatherCode).emoji,
            point.x,
            28.dp.toPx(),
            EMOJI_STYLE
        )
        drawCentered(
            textMeasurer,
            "${hour.temperatureC.roundToInt()}°",
            point.x,
            point.y - 26.dp.toPx(),
            styles.temperature
        )

        if (isSelected) {
            drawCircle(temperatureColor(hour.temperatureC), radius = 7.dp.toPx(), center = point)
            drawCircle(Color.White, radius = 4.dp.toPx(), center = point)
        } else {
            drawCircle(Color.White.copy(alpha = 0.85f), radius = 2.5.dp.toPx(), center = point)
        }

        val probability = hour.precipitationProbability ?: 0
        if (probability > 0) {
            val barHeight = (barMax * probability / 100f).coerceAtLeast(2.dp.toPx())
            val barWidth = 10.dp.toPx()
            drawRoundRect(
                color = PrecipitationColor.copy(alpha = 0.35f + 0.6f * probability / 100f),
                topLeft = Offset(point.x - barWidth / 2, barBottom - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            drawCentered(textMeasurer, "%$probability", point.x, barBottom - barMax - 18.dp.toPx(), styles.precipitation)
        }
    }
}

private fun DrawScope.drawCentered(measurer: TextMeasurer, text: String, centerX: Float, top: Float, style: TextStyle) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(centerX - layout.size.width / 2f, top))
}

private fun TextStyle.boldIf(condition: Boolean) = if (condition) copy(color = Color.White, fontWeight = FontWeight.Bold) else this

/** İlk saat "Şimdi"; gece yarısı saat yerine günün kısa adı ("Pzt") yazılır. */
private fun timeLabel(hours: List<HourlyForecast>, index: Int): String {
    val time = hours[index].time
    return when {
        index == 0 -> "Şimdi"
        time.hour == 0 -> time.format(SHORT_DAY_FORMAT).replaceFirstChar(Char::titlecase)
        else -> time.format(HOUR_FORMAT)
    }
}

private val SLOT_WIDTH = 58.dp
private val CHART_HEIGHT = 196.dp
private val CHART_HEIGHT_COMPACT = 150.dp
private const val MIN_RANGE = 6.0
private val EMOJI_STYLE = TextStyle(fontSize = 22.sp)
private val HOUR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val SHORT_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("tr"))
