package com.kampplus.hava.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Bir şehrin anlık durumu + önümüzdeki saatler ve günler.
 *
 * Tahmin ekranının veri modeli: bir şehre dokunulduğunda gösterilen bilgiler.
 * Zamanlar `timezone=auto` sayesinde şehrin **yerel** saatindedir.
 */
data class Forecast(
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>
)

/**
 * Şu anki hava. Son üç alan servis tarafından her zaman gönderilmez,
 * bu yüzden nullable'dır ve ekranda yalnızca varsa gösterilir.
 */
data class CurrentWeather(
    val temperatureC: Double,
    val weatherCode: Int,
    val observedAt: LocalDateTime,
    val apparentTemperatureC: Double? = null,
    val humidityPercent: Int? = null,
    val windSpeedKmh: Double? = null,
    val isDay: Boolean = true
)

/**
 * Tek bir saatlik tahmin satırı.
 *
 * Son dört alan servis tarafından her zaman gönderilmeyebilir; aktivite penceresi
 * hesabı eksik alanlar için makul varsayılanlara düşer.
 */
data class HourlyForecast(
    val time: LocalDateTime,
    val temperatureC: Double,
    val weatherCode: Int,
    val precipitationProbability: Int? = null,
    val apparentTemperatureC: Double? = null,
    val windSpeedKmh: Double? = null,
    val humidityPercent: Int? = null,
    val isDay: Boolean = true
)

/**
 * Tek bir günlük tahmin satırı: o günün en düşük ve en yüksek sıcaklığı.
 *
 * [sunrise] ve [sunset] şehrin yerel saatindedir; kutup bölgelerinde gelmeyebilir.
 */
data class DailyForecast(
    val date: LocalDate,
    val minTemperatureC: Double,
    val maxTemperatureC: Double,
    val weatherCode: Int,
    val precipitationProbability: Int? = null,
    val sunrise: LocalDateTime? = null,
    val sunset: LocalDateTime? = null
)

/**
 * Bir şehrin "şu an" özeti: liste kartındaki mini gökyüzünü çizmeye yetecek kadar veri.
 *
 * [localTime] verinin alındığı andaki yerel saattir. Kartta canlı saat göstermek için
 * [utcOffsetSeconds] kullanılır: saat, dakikada bir telefonun saatinden hesaplanır.
 */
data class CitySnapshot(
    val temperatureC: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val localTime: LocalDateTime,
    val sunrise: LocalDateTime? = null,
    val sunset: LocalDateTime? = null,
    val utcOffsetSeconds: Int? = null
)
