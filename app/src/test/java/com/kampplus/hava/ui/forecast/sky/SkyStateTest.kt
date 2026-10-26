package com.kampplus.hava.ui.forecast.sky

import com.kampplus.hava.model.WeatherCondition
import com.kampplus.hava.ui.forecast.sunProgress
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Gökyüzü arka planının kararlarını doğrular: hangi saatte hangi palet,
 * hangi havada hangi sahne. Çizimin kendisi değil, çizime giden mantık test edilir.
 */
class SkyStateTest {

    private val sunrise = LocalDateTime.of(2026, 10, 25, 7, 0)
    private val sunset = LocalDateTime.of(2026, 10, 25, 18, 0)

    private fun phaseAt(hour: Int, minute: Int = 0) =
        SkyState.phaseOf(sunrise.withHour(hour).withMinute(minute), sunrise, sunset, isDay = true)

    @Test
    fun `gun dogumuna yakin saat safak sayilir`() {
        assertEquals(DayPhase.DAWN, phaseAt(6, 30))
        assertEquals(DayPhase.DAWN, phaseAt(7, 40))
    }

    @Test
    fun `gun ortasi gunduz sayilir`() {
        assertEquals(DayPhase.DAY, phaseAt(13))
    }

    @Test
    fun `gun batimina yakin saat alacakaranlik sayilir`() {
        assertEquals(DayPhase.DUSK, phaseAt(18, 20))
    }

    @Test
    fun `gun batimindan cok sonra gece sayilir`() {
        assertEquals(DayPhase.NIGHT, phaseAt(22))
        assertEquals(DayPhase.NIGHT, phaseAt(3))
    }

    @Test
    fun `gunes saatleri yoksa is_day bilgisine dusulur`() {
        val now = sunrise.withHour(13)
        assertEquals(DayPhase.DAY, SkyState.phaseOf(now, null, null, isDay = true))
        assertEquals(DayPhase.NIGHT, SkyState.phaseOf(now, null, null, isDay = false))
    }

    @Test
    fun `hava durumu dogru sahneye eslenir`() {
        assertEquals(SkyScene.CLEAR, SkyState.sceneOf(WeatherCondition.MAINLY_CLEAR))
        assertEquals(SkyScene.RAIN, SkyState.sceneOf(WeatherCondition.DRIZZLE))
        assertEquals(SkyScene.STORM, SkyState.sceneOf(WeatherCondition.THUNDERSTORM))
        assertEquals(SkyScene.SNOW, SkyState.sceneOf(WeatherCondition.SNOW_SHOWERS))
    }

    @Test
    fun `gunes ilerlemesi gun icinde 0 ile 1 arasindadir`() {
        assertEquals(0f, sunProgress(sunrise.withHour(5), sunrise, sunset), 0.001f)
        assertEquals(0.5f, sunProgress(sunrise.withHour(12).withMinute(30), sunrise, sunset), 0.001f)
        assertEquals(1f, sunProgress(sunrise.withHour(23), sunrise, sunset), 0.001f)
    }
}
