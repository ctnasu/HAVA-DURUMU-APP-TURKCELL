package com.kampplus.hava.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** İl aramasının Türkçe karakter, büyük/küçük harf ve plaka kuralları. */
class CitySearchTest {

    private fun names(query: String) = searchCities(query, CityCatalog.cities).map { it.name }

    @Test
    fun `bos arama tum illeri dondurur`() {
        assertEquals(81, names("").size)
        assertEquals(81, names("   ").size)
    }

    @Test
    fun `buyuk kucuk harf ve noktali i farki onemsizdir`() {
        // İngilizce klavyede "I" yazan kullanıcı da İzmir'i ve İstanbul'u bulmalı.
        assertEquals(listOf("İzmir"), names("Izmir"))
        assertEquals(listOf("İzmir"), names("izmir"))
        assertEquals(listOf("İzmir"), names("İZMİR"))
        assertEquals(listOf("İstanbul"), names("ISTANBUL"))
    }

    @Test
    fun `turkce karakterler yerine ascii yazilabilir`() {
        assertEquals(listOf("Şanlıurfa"), names("sanliurfa"))
        assertEquals(listOf("Muğla"), names("mugla"))
        assertEquals(listOf("Hakkâri"), names("hakkari"))
        assertEquals(listOf("Iğdır"), names("igdir"))
    }

    @Test
    fun `adi arananla baslayanlar one gelir`() {
        val result = names("an")

        // "Ankara" ve "Antalya" ile başlar; "Adana", "Çankırı" gibi içinde geçenler sonra gelir.
        assertEquals(listOf("Ankara", "Antalya"), result.take(2))
        assertEquals(true, "Çankırı" in result)
    }

    @Test
    fun `yalnizca rakam yazilirsa plaka aranir`() {
        assertEquals(listOf("İstanbul"), names("34"))
        assertEquals(listOf("Adana"), names("1"))
        assertEquals(emptyList<String>(), names("99"))
    }

    @Test
    fun `eslesme yoksa bos liste doner`() {
        assertEquals(emptyList<String>(), names("xyz"))
    }
}
