package com.kampplus.hava.testing

import com.kampplus.hava.data.WeatherRepository
import com.kampplus.hava.model.City
import com.kampplus.hava.model.CitySnapshot
import com.kampplus.hava.model.CurrentWeather
import com.kampplus.hava.model.Forecast
import java.time.LocalDateTime

/**
 * Testlerde gerçek repository yerine kullanılan sahteler.
 *
 * Repository'yi arayüz olarak tanımlamamızın sebebi tam olarak budur: ViewModel
 * testleri internete çıkmadan, istenen her sonucu (başarı / boş / hata)
 * kolayca üretebilir.
 */

/** @param error Doluysa [getForecast] bu hatayı fırlatır. */
class FakeWeatherRepository(
    var result: Forecast = sampleForecast(),
    var error: Throwable? = null
) : WeatherRepository {

    var callCount: Int = 0
        private set

    override suspend fun getForecast(latitude: Double, longitude: Double): Forecast {
        callCount++
        error?.let { throw it }
        return result
    }

    /** Kaç kez toplu anlık durum istendiği; gereksiz istek atılmadığını doğrulamak için. */
    var snapshotCallCount: Int = 0
        private set

    /** Her şehir için aynı hazır anlık durumu döner. */
    override suspend fun getSnapshots(cities: List<City>): Map<Int, CitySnapshot> {
        snapshotCallCount++
        error?.let { throw it }
        return cities.associate { it.plate to sampleSnapshot() }
    }
}

/** Testlerde tekrar tekrar yazmamak için hazır bir il. */
fun sampleCity(plate: Int = 6, name: String = "Ankara") = City(plate, name, latitude = 39.93, longitude = 32.86)

/** Testlerde tekrar tekrar yazmamak için hazır bir tahmin. */
fun sampleForecast() = Forecast(
    current = CurrentWeather(
        temperatureC = 21.0,
        weatherCode = 0,
        observedAt = LocalDateTime.parse("2026-09-24T11:30")
    ),
    hourly = emptyList(),
    daily = emptyList()
)

/** Testlerde tekrar tekrar yazmamak için hazır bir anlık durum. */
fun sampleSnapshot() = CitySnapshot(
    temperatureC = 18.0,
    weatherCode = 61,
    isDay = true,
    localTime = LocalDateTime.parse("2026-09-24T11:30")
)

/** Testlerde Android olmadan çalışan, bellekteki sahte depo. */
class InMemoryStorage(initial: Map<String, String> = emptyMap()) : com.kampplus.hava.data.KeyValueStorage {
    val values = initial.toMutableMap()

    override fun read(key: String): String? = values[key]

    override fun write(key: String, value: String) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
