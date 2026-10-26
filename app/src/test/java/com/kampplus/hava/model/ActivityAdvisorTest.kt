package com.kampplus.hava.model

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Saatlik tahminden "en iyi pencere"nin seçilmesi. */
class ActivityAdvisorTest {

    private val start = LocalDateTime.of(2026, 10, 25, 21, 0)

    /** Saat saat tahmin: [temp] ve [rain] fonksiyonları saat sırasını alır. */
    private fun forecast(
        count: Int = 48,
        temp: (Int) -> Double = { 12.0 },
        rain: (Int) -> Int = { 0 },
        humidity: Int = 50,
        wind: Double = 5.0
    ) = List(count) { i ->
        val time = start.plusHours(i.toLong())
        HourlyForecast(
            time = time,
            temperatureC = temp(i),
            weatherCode = 0,
            precipitationProbability = rain(i),
            apparentTemperatureC = temp(i),
            windSpeedKmh = wind,
            humidityPercent = humidity,
            isDay = time.hour in 7..18
        )
    }

    private val running = ActivityModel.initial(Activity.RUNNING)

    @Test
    fun `en iyi pencere gunduz saatlerinden secilir`() {
        val advice = adviseActivity(Activity.RUNNING, running, forecast())

        val best = checkNotNull(advice.best)
        // Gece saatleri cezalıdır; pencere gündüz içinde olmalı. Başlangıç 21:00 olduğu için yarın sabah.
        assertTrue("pencere gündüz olmalı: ${best.start}", best.start.hour in 7..17)
        assertEquals(best.start.plusHours(2), best.end)
    }

    @Test
    fun `pencere uzunlugu aktiviteye gore degisir`() {
        val laundry = adviseActivity(Activity.LAUNDRY, ActivityModel.initial(Activity.LAUNDRY), forecast(temp = { 22.0 }))

        val best = checkNotNull(laundry.best)
        assertEquals(best.start.plusHours(4), best.end)
    }

    @Test
    fun `yagmurlu saatler atlanir`() {
        // Yarın sabah (index 10-14 arası ~07:00-11:00) yağmurlu; öğleden sonra açık.
        val advice = adviseActivity(Activity.RUNNING, running, forecast(rain = { if (it in 8..16) 90 else 0 }))

        val best = checkNotNull(advice.best)
        assertTrue("yağmurlu saatlere denk gelmemeli: ${best.start}", best.startIndex !in 7..16)
    }

    @Test
    fun `araba yikamada sonraki saatlerdeki yagmur da hesaba katilir`() {
        // Öğlen 13:00'ten sonra yağmur başlıyor (index 16'dan itibaren). Yıkama 12 saat ilerisine bakar:
        // yağmurdan hemen önceki saatler de uygun değildir.
        val rain: (Int) -> Int = { if (it >= 16) 95 else 0 }
        val advice = adviseActivity(Activity.CAR_WASH, ActivityModel.initial(Activity.CAR_WASH), forecast(rain = rain))

        val beforeRain = advice.hours[12].probability
        val clear = adviseActivity(Activity.CAR_WASH, ActivityModel.initial(Activity.CAR_WASH), forecast()).hours[12].probability
        assertTrue("yağmur öncesi $beforeRain, açık $clear", beforeRain < clear * 0.5)
    }

    @Test
    fun `veri pencereyi kapsamiyorsa en iyi pencere yoktur`() {
        val advice = adviseActivity(Activity.LAUNDRY, ActivityModel.initial(Activity.LAUNDRY), forecast(count = 3))

        assertNull(advice.best)
        assertEquals(3, advice.hours.size)
    }

    @Test
    fun `ogrenilen tercih pencereyi degistirir`() {
        // Sabah 3°, öğleden sonra 17°. Koşu için ideal 12° olduğundan başlangıçta öğleden sonra daha yakındır.
        val temps: (Int) -> Double = { i -> if (LocalDateTime.of(2026, 10, 25, 21, 0).plusHours(i.toLong()).hour < 12) 3.0 else 17.0 }
        val hours = forecast(temp = temps)
        val before = checkNotNull(adviseActivity(Activity.RUNNING, running, hours).best)
        assertTrue("başta öğleden sonra: ${before.start}", before.start.hour >= 12)

        // Kullanıcı soğukta koşmayı sevdiğini öğretiyor.
        var learned = running
        repeat(8) {
            learned = learned.learn(Conditions(3.0, 5.0, 0.0, 0.5, false), good = true, Activity.RUNNING)
            learned = learned.learn(Conditions(17.0, 5.0, 0.0, 0.5, false), good = false, Activity.RUNNING)
        }
        val after = checkNotNull(adviseActivity(Activity.RUNNING, learned, hours).best)

        assertTrue("öğrendikten sonra sabah: ${after.start}", after.start.hour < 12)
    }

    @Test
    fun `eksik alanlar icin makul varsayilanlar kullanilir`() {
        val bare = listOf(HourlyForecast(start, temperatureC = 12.0, weatherCode = 0))

        val conditions = conditionsAt(bare, 0, lookaheadHours = 3)

        assertEquals(12.0, conditions.temperatureC, 0.0)
        assertEquals(0.0, conditions.windKmh, 0.0)
        assertEquals(0.5, conditions.humidity, 0.0)
        assertEquals(0.0, conditions.rainProbability, 0.0)
        assertEquals(false, conditions.isDark)
    }

    @Test
    fun `uygunluk etiketi puana gore secilir`() {
        assertEquals(Fit.GREAT, Fit.of(0.9))
        assertEquals(Fit.GOOD, Fit.of(0.6))
        assertEquals(Fit.POOR, Fit.of(0.35))
        assertEquals(Fit.BAD, Fit.of(0.1))
        assertNotNull(Fit.GREAT.label)
    }
}
