package com.kampplus.hava.model

import java.util.Locale

/**
 * Havasını takip ettiğimiz bir sevdiğimiz.
 *
 * @param id Benzersiz kimlik (eklenme zamanı); listeden silmek için.
 * @param label Kullanıcının yazdığı ad: "Annem", "Kardeşim" ya da "Ayşe".
 * @param plate Yaşadığı ilin plaka kodu ([City.plate]).
 */
data class LovedOne(val id: Long, val label: String, val plate: Int) {

    /**
     * Cümlede kullanılan hâli: "Annem" → "Annen'in", "Ayşe" → "Ayşe'nin".
     * Uygulama kullanıcıya "senin annen" diye hitap ettiği için akrabalık adları
     * "ben" biçiminden ("Annem") "sen" biçimine ("Annen") çevrilir.
     */
    val genitiveForYou: String get() = TurkishSuffix.genitive(toSecondPerson(label))

    companion object {
        private val TURKISH: Locale = Locale.forLanguageTag("tr")

        /** Kullanıcıya hızlı seçenek olarak sunulan akrabalık adları. */
        val relatives = listOf("Annem", "Babam", "Kardeşim", "Ablam", "Abim", "Eşim", "Dedem", "Anneannem", "Babaannem", "Arkadaşım")

        private val firstPersonRelatives = (
            relatives +
                listOf("Ağabeyim", "Teyzem", "Dayım", "Halam", "Amcam", "Kuzenim", "Sevgilim", "Yengem", "Oğlum", "Kızım")
            )
            .map { it.lowercase(TURKISH) }
            .toSet()

        /**
         * "Annem" → "Annen". Yalnızca bilinen akrabalık adlarına uygulanır: "Selim" gibi
         * bir kişi adının son harfini "n" yapmak adı bozardı. Cümle başında kullanıldığı
         * için ilk harf Türkçe kurallara göre büyütülür ("eşim" → "Eşin", "ismail" → "İsmail").
         */
        fun toSecondPerson(label: String): String {
            val name = label.trim()
            val converted = if (name.lowercase(TURKISH) in firstPersonRelatives) name.dropLast(1) + "n" else name
            return converted.replaceFirstChar { it.uppercase(TURKISH) }
        }
    }
}
