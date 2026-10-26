package com.kampplus.hava.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 81 ilin verisi elle yazıldığı için bozulması kolaydır; bu testler yazım hatalarını
 * (tekrarlanan plaka, yanlış işaretli koordinat) derleme sonrası hemen yakalar.
 */
class CityCatalogTest {

    private val cities = CityCatalog.cities

    @Test
    fun `tam olarak 81 il vardir ve plakalar 1 ile 81 arasindadir`() {
        assertEquals(81, cities.size)
        assertEquals((1..81).toList(), cities.map { it.plate })
    }

    @Test
    fun `il adlari benzersizdir`() {
        assertEquals(81, cities.map { it.name }.toSet().size)
    }

    @Test
    fun `tum koordinatlar turkiye sinirlari icindedir`() {
        // Kabaca Türkiye'yi kapsayan dikdörtgen; işareti ters veya basamağı kaymış değer yakalanır.
        cities.forEach {
            assertTrue("${it.name} enlemi: ${it.latitude}", it.latitude in 35.8..42.2)
            assertTrue("${it.name} boylamı: ${it.longitude}", it.longitude in 25.6..44.9)
        }
    }

    @Test
    fun `bilinen illerin plakalari dogrudur`() {
        val byPlate = cities.associateBy { it.plate }
        assertEquals("Ankara", byPlate.getValue(6).name)
        assertEquals("İstanbul", byPlate.getValue(34).name)
        assertEquals("İzmir", byPlate.getValue(35).name)
        assertEquals("Düzce", byPlate.getValue(81).name)
    }

    @Test
    fun `alt yazi plakayi iki haneli gosterir`() {
        assertEquals("Plaka 06 · Türkiye", cities.first { it.plate == 6 }.subtitle)
        assertEquals("Plaka 34 · Türkiye", cities.first { it.plate == 34 }.subtitle)
    }
}
