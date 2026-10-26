package com.kampplus.hava.model

/**
 * Hava durumuna göre en iyi zamanı aranan aktiviteler.
 *
 * Her aktivitenin bir **başlangıç bilgisi** vardır (koşu için ideal sıcaklık gibi);
 * model bu bilgiyle başlar ve kullanıcı geri bildirim verdikçe kullanıcıya göre
 * şekillenir (bkz. [ActivityModel]).
 *
 * @param windowHours Önerilen pencerenin uzunluğu: çamaşır kurumak koşudan uzun sürer.
 * @param lookaheadHours Yağmur, bu saatten sonraki kaç saat içinde de aranır: araba yıkandıktan
 *   sonra gelen yağmur yıkamayı boşa çıkarır.
 */
enum class Activity(
    val label: String,
    val emoji: String,
    val windowHours: Int,
    val lookaheadHours: Int,
    private val prior: Prior
) {
    RUNNING("Koşu", "🏃", windowHours = 2, lookaheadHours = 0, Prior(12.0, 8.0, wind = -1.5, rain = -5.0, humidity = -1.0, dark = -1.5)),
    CYCLING(
        "Bisiklet",
        "🚴",
        windowHours = 2,
        lookaheadHours = 0,
        Prior(19.0, 8.0, wind = -2.5, rain = -5.0, humidity = -0.8, dark = -3.0)
    ),
    WALKING("Yürüyüş", "🚶", windowHours = 2, lookaheadHours = 0, Prior(18.0, 9.0, wind = -1.0, rain = -4.5, humidity = -0.5, dark = -2.5)),
    LAUNDRY(
        "Çamaşır asma",
        "🧺",
        windowHours = 4,
        lookaheadHours = 4,
        Prior(24.0, 10.0, wind = 0.7, rain = -6.0, humidity = -3.5, dark = -6.0)
    ),
    CAR_WASH(
        "Araba yıkama",
        "🚗",
        windowHours = 3,
        lookaheadHours = 12,
        Prior(18.0, 14.0, wind = -0.8, rain = -6.0, humidity = -0.5, dark = -5.0)
    );

    /** Model henüz hiç öğrenmemişken kullanılan ağırlıklar. */
    val priorWeights: List<Double> get() = prior.toWeights()

    /**
     * Başlangıç bilgisi.
     *
     * Sıcaklık, ideal değere göre **çan eğrisi** gibi etki eder: ideal değerde puan en yüksek,
     * uzaklaştıkça düşer. Bu, ağırlık olarak ikinci dereceden bir terimdir (bkz. [toWeights]).
     *
     * @param idealC İdeal sıcaklık (°C).
     * @param toleranceC Sıcaklık ideal değerden bu kadar uzaklaşınca puan belirgin düşer.
     * @param wind Rüzgârın etkisi (negatif = rüzgâr kötü). 20 km/sa için bu değer kadar.
     * @param rain Yağış ihtimalinin etkisi; ihtimal %100 ise bu değer kadar.
     * @param humidity Nemin etkisi; nem %100 ise bu değer kadar.
     * @param dark Gece olmasının etkisi.
     */
    private class Prior(
        val idealC: Double,
        val toleranceC: Double,
        val wind: Double,
        val rain: Double,
        val humidity: Double,
        val dark: Double
    ) {
        /**
         * Çan eğrisini `a·(t − t0)²` biçiminde yazıp açarsak `a·t² − 2a·t0·t + a·t0²` çıkar.
         * Sabit terim ağırlıklardaki `bias`'a eklenir ki ideal sıcaklıkta puan tam [IDEAL_LOGIT] olsun.
         */
        fun toWeights(): List<Double> {
            val t0 = (idealC - TEMPERATURE_CENTER) / TEMPERATURE_SCALE
            val a = -(TEMPERATURE_SCALE * TEMPERATURE_SCALE) / (toleranceC * toleranceC)
            return listOf(IDEAL_LOGIT + a * t0 * t0, -2 * a * t0, a, wind, rain, humidity, dark)
        }
    }

    private companion object {
        /** İdeal koşullarda (olasılık ≈ %92) başlanan değer. */
        const val IDEAL_LOGIT = 2.5
    }
}
