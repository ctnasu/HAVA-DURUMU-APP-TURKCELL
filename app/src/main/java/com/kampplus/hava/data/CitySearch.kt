package com.kampplus.hava.data

import com.kampplus.hava.model.City
import java.util.Locale

/**
 * İl listesinde arama.
 *
 * - Türkçe karakterler ve büyük/küçük harf önemsizdir: "izmir", "Izmir" ve "İZMİR"
 *   aynı sonucu verir; "sanliurfa" Şanlıurfa'yı bulur.
 * - Metin adın herhangi bir yerinde eşleşir; adı arananla **başlayanlar** öne alınır.
 * - Yalnızca rakam yazılırsa plaka kodu aranır: "34" İstanbul'u bulur.
 *
 * Boş arama listenin tamamını döndürür.
 */
fun searchCities(query: String, cities: List<City>): List<City> {
    val text = query.trim()
    if (text.isEmpty()) return cities
    if (text.all { it.isDigit() }) {
        val plate = text.toIntOrNull()
        return cities.filter { it.plate == plate }
    }
    val needle = text.foldTurkish()
    return cities
        .filter { it.name.foldTurkish().contains(needle) }
        // sortedBy kararlıdır: aynı gruptaki iller plaka sırasını korur.
        .sortedBy { if (it.name.foldTurkish().startsWith(needle)) 0 else 1 }
}

/**
 * Türkçe harfleri ASCII karşılıklarına çevirip küçültür.
 *
 * Neden `lowercase()` yetmez? "I" harfi Türkçe'de "ı"ya, "İ" ise "i"ye küçülür; İngilizce
 * klavyede "Izmir" yazan kullanıcı "İzmir"i bulamazdı. İki taraf da aynı ASCII biçime
 * getirilince bu sorun ortadan kalkar.
 */
internal fun String.foldTurkish(): String = buildString(length) {
    for (char in this@foldTurkish) {
        append(
            when (char) {
                'İ', 'I', 'ı', 'Î', 'î' -> 'i'
                'Ş', 'ş' -> 's'
                'Ğ', 'ğ' -> 'g'
                'Ü', 'ü', 'Û', 'û' -> 'u'
                'Ö', 'ö' -> 'o'
                'Ç', 'ç' -> 'c'
                'Â', 'â' -> 'a'
                else -> char.lowercaseChar()
            }
        )
    }
}.lowercase(Locale.ROOT)
