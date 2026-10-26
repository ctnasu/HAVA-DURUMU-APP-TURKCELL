package com.kampplus.hava.data

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Toplu anlık durum yanıtının çözümlenmesi.
 *
 * Open-Meteo birden fazla konum için **dizi**, tek konum için **nesne** döndürür;
 * iki biçimin de doğru okunması gerekir.
 */
class SnapshotParsingTest {

    private val single = """
        {"current": {"time": "2026-10-25T20:45", "temperature_2m": 16.2, "weather_code": 3, "is_day": 0},
         "daily": {"time": ["2026-10-25", "2026-10-26"], "sunrise": ["2026-10-25T07:26", "2026-10-26T07:27"],
                   "sunset": ["2026-10-25T18:09", "2026-10-26T18:07"], "weather_code": [3, 73],
                   "temperature_2m_max": [18.0, 2.4], "temperature_2m_min": [9.0, -3.1],
                   "precipitation_probability_max": [10, 80]}}
    """.trimIndent()

    @Test
    fun `tek konum nesne olarak gelir`() {
        val snapshots = parseSnapshots(JsonParser.parseString(single))

        assertEquals(1, snapshots.size)
        val snapshot = snapshots.single()!!
        assertEquals(16.2, snapshot.temperatureC, 0.0)
        assertEquals(false, snapshot.isDay)
        assertEquals(18, snapshot.sunset!!.hour)
    }

    @Test
    fun `birden cok konum dizi olarak gelir ve sira korunur`() {
        val snapshots = parseSnapshots(JsonParser.parseString("[$single, {\"latitude\": 1.0}]"))

        assertEquals(2, snapshots.size)
        assertEquals(3, snapshots[0]!!.weatherCode)
        // current bloğu olmayan konum null olur; kart gökyüzüsüz çizilir.
        assertNull(snapshots[1])
    }

    @Test
    fun `yarinin ozeti gunluk sutunlarin ikinci elemanindan okunur`() {
        val tomorrow = parseSnapshots(JsonParser.parseString(single)).single()!!.tomorrow!!

        assertEquals(73, tomorrow.weatherCode)
        assertEquals(2.4, tomorrow.maxTemperatureC, 0.01)
        assertEquals(-3.1, tomorrow.minTemperatureC, 0.01)
        assertEquals(80, tomorrow.precipitationProbability)
    }

    @Test
    fun `gunluk blok yoksa yarin null olur ama anlik durum okunur`() {
        val json = """{"current": {"time": "2026-10-25T20:45", "temperature_2m": 16.2, "weather_code": 3}}"""

        val snapshot = parseSnapshots(JsonParser.parseString(json)).single()!!

        assertEquals(null, snapshot.tomorrow)
        assertEquals(16.2, snapshot.temperatureC, 0.0)
    }
}
