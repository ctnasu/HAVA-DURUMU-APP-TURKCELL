package com.kampplus.hava.data

import com.kampplus.hava.model.City
import com.kampplus.hava.model.CitySnapshot
import com.kampplus.hava.model.Forecast
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hava tahmini verisine erişimin tek kapısı.
 *
 * Arayüz + gerçek uygulama deseni: tahmin ekranı yalnızca
 * bu arayüzü tanır; Retrofit'i veya JSON'u bilmez.
 */
interface WeatherRepository {
    /** Verilen koordinat için anlık durum + saatlik + günlük tahmini getirir. */
    suspend fun getForecast(latitude: Double, longitude: Double): Forecast

    /** Şehirlerin anlık özetini getirir; anahtar plaka kodudur ([City.plate]). */
    suspend fun getSnapshots(cities: List<City>): Map<Int, CitySnapshot>
}

/** Gerçek uygulama: tahmini Open-Meteo forecast servisinden alır. */
class OpenMeteoWeatherRepository(
    private val api: ForecastApi = ApiClient.forecastApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeatherRepository {

    override suspend fun getForecast(latitude: Double, longitude: Double): Forecast = withContext(ioDispatcher) {
        // Ağ yanıtı sütun biçimindedir; toForecast() onu ekranın beklediği
        // satır biçimine çevirir (bkz. ForecastResponse.kt).
        api.getForecast(latitude = latitude, longitude = longitude).toForecast()
    }

    override suspend fun getSnapshots(cities: List<City>): Map<Int, CitySnapshot> = withContext(ioDispatcher) {
        if (cities.isEmpty()) return@withContext emptyMap()
        val json = api.getSnapshots(
            latitudes = cities.joinToString(",") { it.latitude.toString() },
            longitudes = cities.joinToString(",") { it.longitude.toString() }
        )
        // Yanıt, istekteki sırayla gelir; şehirlerle index üzerinden eşleştirilir.
        cities.zip(parseSnapshots(json))
            .mapNotNull { (city, snapshot) -> snapshot?.let { city.plate to it } }
            .toMap()
    }
}
