package com.kampplus.hava.model

import java.time.LocalDateTime

/** Bir saatin puanı ve o puanı veren koşullar. */
data class HourScore(val time: LocalDateTime, val probability: Double, val conditions: Conditions)

/**
 * Önerilen pencere: [start] dahil, [end] hariç.
 *
 * @param startIndex Pencerenin saat listesindeki başlangıç sırası.
 * @param score Penceredeki saatlerin ortalama olasılığı.
 */
data class ActivityWindow(val start: LocalDateTime, val end: LocalDateTime, val startIndex: Int, val score: Double) {
    val fit: Fit get() = Fit.of(score)
}

/** Puanın insan dilindeki karşılığı. Sıra, en yüksek eşikten en düşüğe doğrudur. */
enum class Fit(val label: String, val minScore: Double) {
    GREAT("Çok uygun", 0.75),
    GOOD("Uygun", 0.5),
    POOR("Pek değil", 0.3),
    BAD("Önerilmez", 0.0);

    companion object {
        fun of(score: Double) = entries.first { score >= it.minScore }
    }
}

/**
 * Bir aktivite için önümüzdeki saatlerin puanları ve en iyi pencere.
 *
 * @param hours Her saatin puanı; sıra, verilen saatlik tahmin listesiyle aynıdır.
 * @param best En yüksek ortalama puanlı pencere; veri pencereyi kapsamıyorsa `null`.
 */
data class ActivityAdvice(val activity: Activity, val hours: List<HourScore>, val best: ActivityWindow?)

/**
 * Saatlik tahmini, kullanıcının modeliyle puanlayıp en iyi pencereyi bulur.
 *
 * @param upcoming Şu andan itibaren, saat saat ilerleyen tahmin.
 */
fun adviseActivity(activity: Activity, model: ActivityModel, upcoming: List<HourlyForecast>): ActivityAdvice {
    val hours = upcoming.indices.map { index ->
        val conditions = conditionsAt(upcoming, index, activity.lookaheadHours)
        HourScore(upcoming[index].time, model.probability(conditions), conditions)
    }
    val length = activity.windowHours
    // Pencereler üst üste biner; ilk en iyi pencere seçilir (eşitlikte daha erken olan kazanır).
    val bestStart = (0..hours.size - length).maxByOrNull { start ->
        hours.subList(start, start + length).map { it.probability }.average()
    }
    val best = bestStart?.let { start ->
        ActivityWindow(
            start = hours[start].time,
            end = hours[start].time.plusHours(length.toLong()),
            startIndex = start,
            score = hours.subList(start, start + length).map { it.probability }.average()
        )
    }
    return ActivityAdvice(activity, hours, best)
}

/**
 * Bir saatin koşullarını çıkarır. Eksik alanlar için makul varsayılanlar kullanılır:
 * hissedilen sıcaklık yoksa sıcaklık, rüzgâr yoksa durgun hava, nem yoksa %50.
 *
 * Yağmur ihtimali, o saatten itibaren [lookaheadHours] saat içindeki **en yüksek** değerdir.
 */
fun conditionsAt(hours: List<HourlyForecast>, index: Int, lookaheadHours: Int): Conditions {
    val hour = hours[index]
    val horizon = hours.subList(index, minOf(hours.size, index + lookaheadHours + 1))
    val rain = horizon.maxOf { it.precipitationProbability ?: 0 } / PERCENT
    return Conditions(
        temperatureC = hour.apparentTemperatureC ?: hour.temperatureC,
        windKmh = hour.windSpeedKmh ?: 0.0,
        rainProbability = rain,
        humidity = (hour.humidityPercent ?: DEFAULT_HUMIDITY_PERCENT) / PERCENT,
        isDark = !hour.isDay
    )
}

private const val PERCENT = 100.0
private const val DEFAULT_HUMIDITY_PERCENT = 50
