package com.kampplus.hava.ui.forecast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
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
import com.kampplus.hava.model.Activity
import com.kampplus.hava.model.ActivityAdvice
import com.kampplus.hava.model.ActivityModel
import com.kampplus.hava.model.ActivityWindow
import com.kampplus.hava.model.Conditions
import com.kampplus.hava.model.Fit
import com.kampplus.hava.model.HourScore
import com.kampplus.hava.model.HourlyForecast
import com.kampplus.hava.model.adviseActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * "En iyi zaman" bölümü: her aktivite için önümüzdeki 48 saatin en uygun penceresi.
 *
 * Bir satıra dokununca satır açılır: saat saat uygunluk şeridi, seçili saatin koşulları ve
 * "İyiydi / Kötüydü" düğmeleri görünür. Her cevap o aktivitenin modelini günceller
 * (bkz. [ActivityModel.learn]); satır, öğrenilen ideal sıcaklığı da gösterir ki öğrenme
 * gözle görülsün.
 *
 * @param now Şehrin yerel saati; pencerelerin "Bugün / Yarın" etiketi bunun üzerinden hesaplanır.
 * @param hours Şu andan itibaren saat saat tahmin.
 */
@Composable
fun ActivityWindows(
    now: LocalDateTime,
    hours: List<HourlyForecast>,
    models: Map<Activity, ActivityModel>,
    onFeedback: (Activity, Conditions, Boolean) -> Unit,
    onReset: (Activity) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    Column(modifier = modifier) {
        Activity.entries.forEachIndexed { index, activity ->
            if (index > 0) HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
            val model = models[activity] ?: ActivityModel.initial(activity)
            // Model değişince (geri bildirimle) puanlar yeniden hesaplanır.
            val advice = remember(activity, model, hours) { adviseActivity(activity, model, hours) }
            ActivityRow(
                advice = advice,
                model = model,
                today = now.toLocalDate(),
                expanded = expanded == activity.name,
                onToggle = { expanded = if (expanded == activity.name) null else activity.name },
                onFeedback = { conditions, good -> onFeedback(activity, conditions, good) },
                onReset = { onReset(activity) }
            )
        }
    }
}

@Composable
private fun ActivityRow(
    advice: ActivityAdvice,
    model: ActivityModel,
    today: LocalDate,
    expanded: Boolean,
    onToggle: () -> Unit,
    onFeedback: (Conditions, Boolean) -> Unit,
    onReset: () -> Unit
) {
    val activity = advice.activity
    val best = advice.best
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = activity.emoji, fontSize = 26.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = activity.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = best?.let { windowText(it, today) } ?: "Önümüzdeki saatler için yeterli veri yok",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            best?.let { FitPill(it) }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Kapat" else "Aç",
                modifier = Modifier.rotate(chevron),
                tint = Color.White.copy(alpha = 0.7f)
            )
        }
        AnimatedVisibility(visible = expanded && best != null) {
            if (best != null) ActivityDetails(advice, best, model, today, onFeedback, onReset)
        }
    }
}

/** "Yarın 07:00–09:00". */
private fun windowText(window: ActivityWindow, today: LocalDate): String =
    "${dayName(window.start.toLocalDate(), today)} ${window.start.format(TIME)}–${window.end.format(TIME)}"

private fun dayName(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Bugün"
    today.plusDays(1) -> "Yarın"
    else -> date.format(DAY_NAME).replaceFirstChar(Char::titlecase)
}

@Composable
private fun FitPill(window: ActivityWindow) {
    val color = fitColor(window.score)
    Text(
        text = window.fit.label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** Uygunluk puanının rengi; şerit çubukları ve etiket aynı eşikleri kullanır. */
private fun fitColor(score: Double): Color = when (Fit.of(score)) {
    Fit.GREAT -> GREEN
    Fit.GOOD -> LIME
    Fit.POOR -> ORANGE
    Fit.BAD -> RED
}

@Composable
private fun ActivityDetails(
    advice: ActivityAdvice,
    best: ActivityWindow,
    model: ActivityModel,
    today: LocalDate,
    onFeedback: (Conditions, Boolean) -> Unit,
    onReset: () -> Unit
) {
    // -1: kullanıcı henüz bir saat seçmedi; en iyi pencerenin başı gösterilir.
    var picked by remember { mutableIntStateOf(-1) }
    val selected = (if (picked >= 0) picked else best.startIndex).coerceIn(advice.hours.indices)
    val score = advice.hours[selected]
    // Son cevap, seçim değişince silinir: yeni bir saat için yeni bir cevap beklenir.
    var answered by remember(selected) { mutableStateOf<Boolean?>(null) }

    Column(modifier = Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HourStrip(
            scores = advice.hours,
            selected = selected,
            bestRange = best.startIndex until best.startIndex + advice.activity.windowHours,
            onSelect = { picked = it },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "${dayName(
                    score.time.toLocalDate(),
                    today
                )} ${score.time.format(TIME)} · %${(score.probability * 100).roundToInt()} uygun",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = conditionsText(score.conditions),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
        FeedbackRow(
            answered = answered,
            onAnswer = { good ->
                answered = good
                onFeedback(score.conditions, good)
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LearningStatus(advice.activity, model, onReset, modifier = Modifier.padding(horizontal = 16.dp))
    }
}

/** 12.43 → "12,4" (Türkçe ondalık virgülü). */
private fun oneDecimal(value: Double): String = String.format(TURKISH, "%.1f", value)

/** "12° · rüzgâr 8 km/sa · yağış %0 · nem %55". */
private fun conditionsText(c: Conditions) = buildString {
    append("${c.temperatureC.roundToInt()}° · rüzgâr ${c.windKmh.roundToInt()} km/sa · ")
    append("yağış %${(c.rainProbability * 100).roundToInt()} · nem %${(c.humidity * 100).roundToInt()}")
    if (c.isDark) append(" · gece")
}

@Composable
private fun FeedbackRow(answered: Boolean?, onAnswer: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Bu koşullarda nasıldı? Cevabınla modeli eğit:",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.65f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeedbackButton("👍 İyiydi", selected = answered == true) { onAnswer(true) }
            FeedbackButton("👎 Kötüydü", selected = answered == false) { onAnswer(false) }
        }
        AnimatedVisibility(visible = answered != null) {
            Text(
                text = "Öğrendim! Bu tür koşulları artık ${if (answered == true) "daha uygun" else "daha az uygun"} sayacağım.",
                style = MaterialTheme.typography.bodySmall,
                color = GREEN
            )
        }
    }
}

@Composable
private fun FeedbackButton(text: String, selected: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color.White.copy(alpha = if (selected) 0.32f else 0.14f),
            contentColor = Color.White
        )
    ) { Text(text) }
}

/** Modelin ne kadar öğrendiğini gösterir: ideal sıcaklığın başlangıçtan nereye kaydığı. */
@Composable
private fun LearningStatus(activity: Activity, model: ActivityModel, onReset: () -> Unit, modifier: Modifier = Modifier) {
    // Tek ondalık: birkaç geri bildirimle ideal sıcaklık küsurat kadar kayar; tam sayıya
    // yuvarlarsak öğrenme görünmez olurdu.
    val initial = ActivityModel.initial(activity).idealTemperatureC?.let(::oneDecimal)
    val current = model.idealTemperatureC?.let(::oneDecimal)
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (model.samples == 0) {
                "Henüz öğrenmedim. Geri bildirim verdikçe sana göre ayarlanırım."
            } else {
                "Senin ideal sıcaklığın ≈ $current° (başlangıç: $initial°) · ${model.samples} geri bildirim"
            },
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.weight(1f)
        )
        if (model.samples > 0) {
            TextButton(onClick = onReset) { Text("Sıfırla", color = Color.White.copy(alpha = 0.8f)) }
        }
    }
}

/**
 * Saat saat uygunluk şeridi: çubuk ne kadar uzunsa o saat o kadar uygun, renk kırmızıdan yeşile.
 * En iyi pencere hafif bir zeminle işaretlenir. Dokunarak ya da basılı tutup kaydırarak saat seçilir.
 */
@Composable
private fun HourStrip(scores: List<HourScore>, selected: Int, bestRange: IntRange, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val haptics = LocalHapticFeedback.current
    // pointerInput bloğu bir kez kurulur; en güncel değerleri okuyabilmesi için.
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val slotPx = with(LocalDensity.current) { SLOT_WIDTH.toPx() }
    fun select(x: Float) {
        val index = (x / slotPx).toInt().coerceIn(scores.indices)
        if (index != currentSelected) {
            currentOnSelect(index)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Box(modifier = modifier.horizontalScroll(rememberScrollState())) {
        Canvas(
            modifier = Modifier
                .width(SLOT_WIDTH * scores.size)
                .height(STRIP_HEIGHT)
                .pointerInput(scores.size) { detectTapGestures { select(it.x) } }
                .pointerInput(scores.size) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { select(it.x) },
                        onDrag = { change, _ -> select(change.position.x) }
                    )
                }
                .semantics { contentDescription = "Saat saat uygunluk şeridi. Bir saate dokunarak seç." }
        ) {
            drawStrip(scores, selected, bestRange, textMeasurer)
        }
    }
}

private fun DrawScope.drawStrip(scores: List<HourScore>, selected: Int, bestRange: IntRange, measurer: TextMeasurer) {
    val slot = SLOT_WIDTH.toPx()
    val barWidth = 10.dp.toPx()
    val baseline = 50.dp.toPx()
    val maxBar = 40.dp.toPx()
    val minBar = 4.dp.toPx()

    // En iyi pencerenin zemini.
    drawRoundRect(
        color = Color.White.copy(alpha = 0.12f),
        topLeft = Offset(bestRange.first * slot + 2.dp.toPx(), 0f),
        size = Size(bestRange.count() * slot - 4.dp.toPx(), size.height),
        cornerRadius = CornerRadius(12.dp.toPx())
    )

    scores.forEachIndexed { index, score ->
        val centerX = slot * (index + 0.5f)
        val height = minBar + (maxBar - minBar) * score.probability.toFloat()
        val color = lerp(RED, GREEN, score.probability.toFloat())
        val isSelected = index == selected

        if (score.time.hour == 0) {
            drawLine(
                Color.White.copy(alpha = 0.25f),
                Offset(slot * index, 6.dp.toPx()),
                Offset(slot * index, size.height - 6.dp.toPx()),
                1.dp.toPx()
            )
        }
        drawRoundRect(
            color = color.copy(alpha = if (isSelected) 1f else 0.75f),
            topLeft = Offset(centerX - barWidth / 2, baseline - height),
            size = Size(barWidth, height),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
        if (isSelected) {
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(centerX - barWidth / 2 - 2.dp.toPx(), baseline - height - 2.dp.toPx()),
                size = Size(barWidth + 4.dp.toPx(), height + 4.dp.toPx()),
                cornerRadius = CornerRadius(5.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        // Her 3 saatte bir saat etiketi; gece yarısında günün kısa adı.
        val label = when {
            score.time.hour == 0 -> score.time.format(SHORT_DAY).replaceFirstChar(Char::titlecase)
            score.time.hour % 3 == 0 -> score.time.format(HOUR_ONLY)
            else -> null
        }
        label?.let {
            val layout = measurer.measure(it, if (isSelected) LABEL_STYLE_SELECTED else LABEL_STYLE)
            drawText(layout, topLeft = Offset(centerX - layout.size.width / 2f, baseline + 6.dp.toPx()))
        }
    }
}

private val SLOT_WIDTH = 22.dp
private val STRIP_HEIGHT = 76.dp
private val TURKISH: Locale = Locale.forLanguageTag("tr")
private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val HOUR_ONLY: DateTimeFormatter = DateTimeFormatter.ofPattern("HH")
private val DAY_NAME: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE", TURKISH)
private val SHORT_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", TURKISH)
private val LABEL_STYLE = TextStyle(fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
private val LABEL_STYLE_SELECTED = TextStyle(fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
private val GREEN = Color(0xFF6BE3A0)
private val LIME = Color(0xFFD7E36B)
private val ORANGE = Color(0xFFFFB25B)
private val RED = Color(0xFFFF7A7A)
