package com.kampplus.hava.ui.forecast.sky

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.kampplus.hava.model.Forecast
import com.kampplus.hava.model.WeatherCondition
import com.kampplus.hava.ui.forecast.sunProgress
import java.time.Duration
import java.time.LocalDateTime

/** Gökyüzünde ne çizileceği: hava durumunun görsel grubu. */
enum class SkyScene { CLEAR, CLOUDY, RAIN, STORM, SNOW, FOG }

/** Günün hangi bölümündeyiz? Gökyüzünün renk paleti buna göre seçilir. */
enum class DayPhase { DAWN, DAY, DUSK, NIGHT }

/**
 * Arka planı çizmek için gereken her şey. Saf veri olduğu için unit test ile
 * doğrulanabilir; Compose'a bağımlı değildir (renkler hariç).
 *
 * @param sunProgress Güneşin gündüz yayındaki yeri: 0 doğuş, 1 batış.
 */
data class SkyState(val scene: SkyScene, val phase: DayPhase, val sunProgress: Float = 0.5f) {

    /** Üstten alta gökyüzü gradyanı. Hava kötüleştikçe renkler griye ve koyuya kayar. */
    val topColor: Color get() = tint(phaseTop(phase))
    val bottomColor: Color get() = tint(phaseBottom(phase))

    private fun tint(base: Color): Color {
        val (target, amount) = when (scene) {
            SkyScene.CLEAR -> return base
            SkyScene.CLOUDY -> Color(0xFF5D6B7D) to 0.45f
            SkyScene.RAIN -> Color(0xFF3B4757) to 0.6f
            SkyScene.STORM -> Color(0xFF1C1F27) to 0.72f
            SkyScene.SNOW -> Color(0xFF7D8DA3) to 0.5f
            SkyScene.FOG -> Color(0xFF7F8792) to 0.6f
        }
        // Gece zaten koyu; griye çekmeyi yarıya indiriyoruz ki gökyüzü bulanıklaşmasın.
        return lerp(base, target, if (phase == DayPhase.NIGHT) amount * 0.5f else amount)
    }

    companion object {
        /** Veri gelmeden (yükleniyor / hata) gösterilen nötr gökyüzü. */
        val Default = SkyState(SkyScene.CLOUDY, DayPhase.DAY)

        /** Gün doğumu/batımına bu kadar yakınsak şafak/alacakaranlık renkleri kullanılır. */
        private val TWILIGHT: Duration = Duration.ofMinutes(45)

        /** Şu anki gökyüzü. */
        fun from(forecast: Forecast): SkyState = with(forecast.current) {
            at(forecast, observedAt, weatherCode, isDay)
        }

        /**
         * Tahmindeki herhangi bir saatin gökyüzü: zaman makinesi bunu kullanır.
         * O günün gün doğumu/batımı tahminde yoksa kaba bir gündüz varsayımına düşülür.
         */
        fun at(forecast: Forecast, time: LocalDateTime, weatherCode: Int, isDay: Boolean = time.hour in 7..18): SkyState {
            val day = forecast.daily.firstOrNull { it.date == time.toLocalDate() }
            return of(time, weatherCode, day?.sunrise, day?.sunset, isDay)
        }

        fun of(time: LocalDateTime, weatherCode: Int, sunrise: LocalDateTime?, sunset: LocalDateTime?, isDay: Boolean) = SkyState(
            scene = sceneOf(WeatherCondition.fromWeatherCode(weatherCode)),
            phase = phaseOf(time, sunrise, sunset, isDay),
            sunProgress = if (sunrise != null && sunset != null) sunProgress(time, sunrise, sunset) else 0.5f
        )

        fun sceneOf(condition: WeatherCondition): SkyScene = when (condition) {
            WeatherCondition.CLEAR, WeatherCondition.MAINLY_CLEAR -> SkyScene.CLEAR
            WeatherCondition.PARTLY_CLOUDY, WeatherCondition.OVERCAST, WeatherCondition.UNKNOWN -> SkyScene.CLOUDY
            WeatherCondition.FOG -> SkyScene.FOG
            WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.RAIN_SHOWERS -> SkyScene.RAIN
            WeatherCondition.THUNDERSTORM -> SkyScene.STORM
            WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWERS -> SkyScene.SNOW
        }

        /**
         * Gün doğumu/batımı saatleri biliniyorsa onlara göre, bilinmiyorsa servisin
         * `is_day` bilgisine göre günün bölümünü bulur.
         */
        fun phaseOf(now: LocalDateTime, sunrise: LocalDateTime?, sunset: LocalDateTime?, isDay: Boolean): DayPhase {
            if (sunrise == null || sunset == null) return if (isDay) DayPhase.DAY else DayPhase.NIGHT
            return when {
                Duration.between(sunrise, now).abs() <= TWILIGHT -> DayPhase.DAWN
                Duration.between(sunset, now).abs() <= TWILIGHT -> DayPhase.DUSK
                now.isAfter(sunrise) && now.isBefore(sunset) -> DayPhase.DAY
                else -> DayPhase.NIGHT
            }
        }

        private fun phaseTop(phase: DayPhase) = when (phase) {
            DayPhase.DAWN -> Color(0xFF3A4A8A)
            DayPhase.DAY -> Color(0xFF2A66CF)
            DayPhase.DUSK -> Color(0xFF29296A)
            DayPhase.NIGHT -> Color(0xFF060A1C)
        }

        private fun phaseBottom(phase: DayPhase) = when (phase) {
            DayPhase.DAWN -> Color(0xFFE99A7A)
            DayPhase.DAY -> Color(0xFF6AAAE8)
            DayPhase.DUSK -> Color(0xFFDD6B55)
            DayPhase.NIGHT -> Color(0xFF1B2A4E)
        }
    }
}
