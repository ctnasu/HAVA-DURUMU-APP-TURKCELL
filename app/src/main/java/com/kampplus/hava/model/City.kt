package com.kampplus.hava.model

/**
 * Türkiye'nin bir ili.
 *
 * `data class`: Kotlin bu sınıf için `equals`, `hashCode`, `toString` ve `copy`
 * fonksiyonlarını kendisi yazar. Veri taşıyan sınıflarda tercih edilir.
 *
 * @param plate Plaka kodu (1–81). İller için benzersizdir; listede ve navigasyonda kimlik olarak kullanılır.
 * @param latitude Enlem. Hava durumu isteği bu koordinatla yapılır.
 * @param longitude Boylam.
 */
data class City(val plate: Int, val name: String, val latitude: Double, val longitude: Double) {

    /** Kartta ve tahmin ekranında gösterilen alt yazı: "Plaka 06 · Türkiye". */
    val subtitle: String
        get() = "Plaka ${plate.toString().padStart(2, '0')} · Türkiye"
}
