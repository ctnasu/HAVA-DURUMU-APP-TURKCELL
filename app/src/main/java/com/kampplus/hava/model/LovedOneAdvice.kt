package com.kampplus.hava.model

/** Bir uyarının ne kadar dikkat gerektirdiği; kartın vurgusu buna göre değişir. */
enum class Severity { ALERT, INFO, NICE }

/**
 * "Sevdiklerim" kartındaki cümle.
 *
 * @param message Örneğin "Annen'in olduğu Sivas'ta yarın kar var, kalın giyinmesini söyle."
 * @param emoji Cümlenin yanındaki simge.
 */
data class LovedOneAdvice(val message: String, val emoji: String, val severity: Severity)

/**
 * Sevdiğimizin yaşadığı ilde **yarın** hava nasıl olacak ve ona ne söylemeliyiz?
 *
 * Kurallar önem sırasıyla denenir; ilk eşleşen kazanır. Böylece hem karlı hem soğuk bir
 * günde "kar" uyarısı çıkar, "soğuk" uyarısı değil.
 *
 * @return Yarının verisi henüz yüklenmediyse `null`.
 */
fun adviseLovedOne(person: LovedOne, cityName: String, tomorrow: DaySummary?): LovedOneAdvice? {
    tomorrow ?: return null
    val intro = "${person.genitiveForYou} olduğu ${TurkishSuffix.locative(cityName)} yarın"
    val condition = WeatherCondition.fromWeatherCode(tomorrow.weatherCode)
    val rain = tomorrow.precipitationProbability ?: 0
    val max = Math.round(tomorrow.maxTemperatureC).toInt()

    return when {
        condition == WeatherCondition.THUNDERSTORM ->
            LovedOneAdvice("$intro gök gürültülü fırtına var, mümkünse evden çıkmasın.", "⛈️", Severity.ALERT)
        condition == WeatherCondition.SNOW || condition == WeatherCondition.SNOW_SHOWERS ->
            LovedOneAdvice("$intro kar var, kalın giyinmesini söyle.", "❄️", Severity.ALERT)
        condition in RAINY || rain >= RAIN_ALERT_PERCENT ->
            LovedOneAdvice("$intro yağmur bekleniyor, şemsiye almasını hatırlat.", "☔", Severity.ALERT)
        tomorrow.minTemperatureC <= 0 ->
            LovedOneAdvice("$intro gece don var, akşam sıkı giyinmesini söyle.", "🥶", Severity.ALERT)
        tomorrow.maxTemperatureC <= COLD_MAX_C ->
            LovedOneAdvice("$intro hava çok soğuk (en çok $max°), kalın giyinmesini söyle.", "🧣", Severity.ALERT)
        tomorrow.maxTemperatureC >= HOT_MAX_C ->
            LovedOneAdvice("$intro hava çok sıcak ($max°), bol su içmesini ve öğle güneşinden kaçmasını söyle.", "🥵", Severity.ALERT)
        condition == WeatherCondition.FOG ->
            LovedOneAdvice("$intro sis var, yola çıkacaksa dikkatli olmasını söyle.", "🌫️", Severity.INFO)
        else ->
            LovedOneAdvice("$intro hava güzel, en çok $max°. Ona iyi günler dile.", "☀️", Severity.NICE)
    }
}

private val RAINY = setOf(WeatherCondition.RAIN, WeatherCondition.RAIN_SHOWERS, WeatherCondition.DRIZZLE)
private const val RAIN_ALERT_PERCENT = 60
private const val COLD_MAX_C = 8.0
private const val HOT_MAX_C = 35.0
