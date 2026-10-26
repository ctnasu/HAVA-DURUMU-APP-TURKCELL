package com.kampplus.hava.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Öğrenen modelin davranışı. Asıl soru: kullanıcı "iyiydi / kötüydü" dedikçe model gerçekten
 * kullanıcıya doğru kayıyor mu, ve tek bir tuhaf cevapla bozulmuyor mu?
 */
class ActivityModelTest {

    private fun conditions(temp: Double = 12.0, wind: Double = 0.0, rain: Double = 0.0, humidity: Double = 0.5, dark: Boolean = false) =
        Conditions(temp, wind, rain, humidity, dark)

    private val running = ActivityModel.initial(Activity.RUNNING)

    @Test
    fun `baslangic modeli aktivitenin ideal sicakligini bilir`() {
        assertEquals(12.0, checkNotNull(running.idealTemperatureC), 0.01)
        assertEquals(19.0, checkNotNull(ActivityModel.initial(Activity.CYCLING).idealTemperatureC), 0.01)
        assertEquals(24.0, checkNotNull(ActivityModel.initial(Activity.LAUNDRY).idealTemperatureC), 0.01)
    }

    @Test
    fun `ideal kosullar yuksek, kotu kosullar dusuk puan alir`() {
        val ideal = running.probability(conditions())
        val rainy = running.probability(conditions(rain = 1.0))
        val freezing = running.probability(conditions(temp = -10.0))
        val night = running.probability(conditions(dark = true))

        assertTrue("ideal: $ideal", ideal > 0.75)
        assertTrue("yağmur: $rainy", rainy < 0.1)
        assertTrue("dondurucu: $freezing", freezing < 0.1)
        assertTrue("gece: $night", night < ideal)
    }

    @Test
    fun `camasir icin nem ve yagmur belirleyicidir`() {
        val laundry = ActivityModel.initial(Activity.LAUNDRY)

        val dry = laundry.probability(conditions(temp = 24.0, wind = 10.0, humidity = 0.35))
        val humid = laundry.probability(conditions(temp = 24.0, wind = 10.0, humidity = 0.9))

        assertTrue("kuru $dry, nemli $humid", dry > 0.8 && humid < 0.5)
    }

    @Test
    fun `iyiydi demek o kosullarin puanini yukseltir ve idealı o yone kaydirir`() {
        val cold = conditions(temp = 4.0)
        val before = running.probability(cold)

        val learned = running.learn(cold, good = true, Activity.RUNNING)

        assertTrue(learned.probability(cold) > before)
        assertTrue("ideal 12'den düşmeli: ${learned.idealTemperatureC}", checkNotNull(learned.idealTemperatureC) < 12.0)
        assertEquals(1, learned.samples)
    }

    @Test
    fun `kotuydu demek o kosullarin puanini dusurur`() {
        val mild = conditions(temp = 12.0)

        val learned = running.learn(mild, good = false, Activity.RUNNING)

        assertTrue(learned.probability(mild) < running.probability(mild))
    }

    @Test
    fun `tutarli geri bildirim ideal sicakligi kalici olarak kullaniciya tasir`() {
        var model = running
        // Kullanıcı hep 5-6 derecede "iyiydi", 15 derecede "kötüydü" diyor.
        repeat(6) {
            model = model.learn(conditions(temp = 5.0), good = true, Activity.RUNNING)
            model = model.learn(conditions(temp = 15.0), good = false, Activity.RUNNING)
        }

        val ideal = checkNotNull(model.idealTemperatureC)
        assertTrue("ideal 8°'nin altına inmeli: $ideal", ideal < 8.0)
        assertTrue(model.probability(conditions(temp = 5.0)) > model.probability(conditions(temp = 15.0)))
    }

    @Test
    fun `tek bir tuhaf cevap modeli altust etmez`() {
        // Yağmurlu bir havada tek seferlik "iyiydi" denmesi, yağmuru sevmeyen bir modeli tamamen çevirmemeli.
        val learned = running.learn(conditions(rain = 1.0), good = true, Activity.RUNNING)

        assertTrue(learned.probability(conditions(rain = 1.0)) < learned.probability(conditions(rain = 0.0)))
    }

    @Test
    fun `ogrenilen model kaydedilip geri okunabilir`() {
        val learned = running.learn(conditions(temp = 4.0), good = true, Activity.RUNNING)

        val restored = checkNotNull(ActivityModel.parse(learned.serialize()))

        assertEquals(learned.samples, restored.samples)
        assertEquals(learned.weights, restored.weights)
    }

    @Test
    fun `bozuk kayit null dondurur`() {
        assertNull(ActivityModel.parse(""))
        assertNull(ActivityModel.parse("abc|1,2,3"))
        assertNull(ActivityModel.parse("3|1,2,3"))
        assertNotNull(ActivityModel.parse(running.serialize()))
    }

    @Test
    fun `ucurum olmayan modelde ideal sicaklik yoktur`() {
        val flat = ActivityModel(listOf(0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0))

        assertNull(flat.idealTemperatureC)
    }
}
