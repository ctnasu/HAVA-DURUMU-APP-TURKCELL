package com.kampplus.hava.data

import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Hava tahmini servisinin menüsü.
 *
 * Retrofit bu arayüzün gövdesini kendisi yazar: biz yalnızca hangi adrese, hangi
 * parametrelerle gidileceğini tarif ederiz. Tahmin `api.open-meteo.com` adresinden gelir.
 *
 * Örnek istek:
 * `GET https://api.open-meteo.com/v1/forecast?latitude=39.92&longitude=32.85&current=...&timezone=auto`
 */
interface ForecastApi {

    /**
     * Tek bir konumun anlık durumunu, saatlik ve günlük tahminini getirir.
     *
     * @param current Anlık olarak istenen değişkenler, virgülle ayrılır.
     * @param hourly Saatlik istenen değişkenler.
     * @param daily Günlük istenen değişkenler.
     * @param forecastDays Kaç günlük tahmin istendiği.
     * @param timezone "auto" dersek sunucu zamanları şehrin yerel saatine göre gönderir.
     */
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("hourly") hourly: String = HOURLY_FIELDS,
        @Query("daily") daily: String = DAILY_FIELDS,
        @Query("forecast_days") forecastDays: Int = FORECAST_DAYS,
        @Query("timezone") timezone: String = "auto"
    ): ForecastResponse

    /**
     * Birden çok konumun anlık durumunu **tek istekte** getirir.
     *
     * Open-Meteo virgülle ayrılmış koordinat listesi kabul eder: 10 kart için 10 istek
     * yerine 1 istek. Yanıt, birden fazla konum varsa dizi, tek konum varsa nesnedir;
     * bu yüzden ham [JsonElement] alıp [parseSnapshots] ile çözüyoruz.
     */
    @GET("v1/forecast")
    suspend fun getSnapshots(
        @Query("latitude") latitudes: String,
        @Query("longitude") longitudes: String,
        @Query("current") current: String = "temperature_2m,weather_code,is_day",
        @Query("daily") daily: String = SNAPSHOT_DAILY_FIELDS,
        @Query("forecast_days") forecastDays: Int = 2,
        @Query("timezone") timezone: String = "auto"
    ): JsonElement

    companion object {
        const val CURRENT_FIELDS =
            "temperature_2m,weather_code,apparent_temperature,relative_humidity_2m,wind_speed_10m,is_day"
        const val HOURLY_FIELDS =
            "temperature_2m,weather_code,precipitation_probability," +
                "apparent_temperature,wind_speed_10m,relative_humidity_2m,is_day"

        /** Kart özeti için: gün doğumu/batımı + bugünün ve yarının kısa özeti. */
        const val SNAPSHOT_DAILY_FIELDS =
            "sunrise,sunset,weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
        const val DAILY_FIELDS =
            "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset"
        const val FORECAST_DAYS = 7
    }
}
