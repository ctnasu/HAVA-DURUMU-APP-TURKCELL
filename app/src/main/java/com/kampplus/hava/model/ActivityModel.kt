package com.kampplus.hava.model

import kotlin.math.exp

/** Ortalama sıcaklık ve ölçek: sıcaklık bu değerlerle ~[-2, 2] aralığına getirilir (özellik ölçekleme). */
internal const val TEMPERATURE_CENTER = 15.0
internal const val TEMPERATURE_SCALE = 10.0

/**
 * Bir aktivite için bir saatin hava koşulları. Modelin girdisidir.
 *
 * @param rainProbability 0..1; aktivitenin [Activity.lookaheadHours] değerine kadar ilerideki saatler dahil en yükseği.
 * @param humidity 0..1.
 */
data class Conditions(
    val temperatureC: Double,
    val windKmh: Double,
    val rainProbability: Double,
    val humidity: Double,
    val isDark: Boolean
) {
    /**
     * Modelin okuduğu sayı vektörü: `[1, t, t², rüzgâr, yağmur, nem, gece]`.
     * `t` ölçeklenmiş sıcaklıktır; `t²` sayesinde model "ne çok soğuk ne çok sıcak" gibi
     * bir **ideal sıcaklık** öğrenebilir (düz bir çizgi bunu ifade edemez).
     */
    fun features(): List<Double> {
        val t = (temperatureC - TEMPERATURE_CENTER) / TEMPERATURE_SCALE
        return listOf(1.0, t, t * t, windKmh / WIND_SCALE, rainProbability, humidity, if (isDark) 1.0 else 0.0)
    }

    private companion object {
        const val WIND_SCALE = 20.0
    }
}

/**
 * Bir aktivite için "bu koşullar bana uygun mu?" sorusunu cevaplayan, kullanıcıya göre öğrenen model.
 *
 * Teknik olarak **lojistik regresyon**: `P(uygun) = sigmoid(w · x)`. Ağırlıklar [Activity.priorWeights]
 * ile başlar; kullanıcı "iyiydi / kötüydü" dedikçe [learn] ile **çevrimiçi** (tek örnekle) güncellenir.
 *
 * Model değişmezdir (immutable): her öğrenme yeni bir model üretir; böylece state akışları
 * ve testler basit kalır.
 *
 * @param samples Şu ana kadar alınan geri bildirim sayısı.
 */
class ActivityModel(val weights: List<Double>, val samples: Int = 0) {

    init {
        require(weights.size == FEATURE_COUNT) { "Ağırlık sayısı $FEATURE_COUNT olmalı: ${weights.size}" }
    }

    /** Bu koşulların kullanıcıya uygun olma olasılığı (0..1). */
    fun probability(conditions: Conditions): Double = sigmoid(conditions.features().zip(weights) { x, w -> x * w }.sum())

    /**
     * Tek bir geri bildirimle bir gradyan adımı atar.
     *
     * Log-kayıp için gradyan `(y − p)·x`'tir: model çok yanıldıkça (örneğin p=0.1 iken kullanıcı
     * "iyiydi" derse) büyük, doğru bildiyse küçük adım atar. Ek olarak ağırlıklar her adımda
     * başlangıç değerlerine **hafifçe çekilir** (L2 düzenlileştirme): tek bir tuhaf cevap
     * modeli bozamaz, ama tutarlı geri bildirimler ağırlıkları kalıcı olarak kaydırır.
     */
    fun learn(conditions: Conditions, good: Boolean, activity: Activity): ActivityModel {
        val x = conditions.features()
        val error = (if (good) 1.0 else 0.0) - probability(conditions)
        val prior = activity.priorWeights
        val updated = weights.indices.map { i ->
            weights[i] + LEARNING_RATE * (error * x[i] - PRIOR_PULL * (weights[i] - prior[i]))
        }
        return ActivityModel(updated, samples + 1)
    }

    /**
     * Öğrenilen ideal sıcaklık (°C): ikinci derece eğrinin tepe noktası. Eğri tepe yapmıyorsa
     * (ikinci derece terim ≥ 0) anlamlı bir ideal yoktur ve `null` döner.
     */
    val idealTemperatureC: Double?
        get() {
            val (_, linear, quadratic) = weights
            if (quadratic >= 0) return null
            val t = -linear / (2 * quadratic)
            return t * TEMPERATURE_SCALE + TEMPERATURE_CENTER
        }

    /** Kalıcı depolama için `"örnekSayısı|w0,w1,..."` biçimi. */
    fun serialize(): String = "$samples|${weights.joinToString(",")}"

    companion object {
        const val FEATURE_COUNT = 7
        private const val LEARNING_RATE = 1.2
        private const val PRIOR_PULL = 0.03

        /** Hiç öğrenmemiş, yalnızca başlangıç bilgisiyle kurulu model. */
        fun initial(activity: Activity) = ActivityModel(activity.priorWeights)

        /** [serialize] çıktısını geri okur; bozuk veride `null` döner. */
        fun parse(text: String): ActivityModel? {
            val (samples, values) = text.split("|").takeIf { it.size == 2 } ?: return null
            val weights = values.split(",").map { it.toDoubleOrNull() ?: return null }
            return runCatching { ActivityModel(weights, samples.toIntOrNull() ?: return null) }.getOrNull()
        }

        private fun sigmoid(x: Double) = 1.0 / (1.0 + exp(-x))
    }
}
