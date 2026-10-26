package com.kampplus.hava.ui.city

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kampplus.hava.model.City
import com.kampplus.hava.model.CitySnapshot
import com.kampplus.hava.model.WeatherCondition
import com.kampplus.hava.ui.forecast.sky.SkyBackground
import com.kampplus.hava.ui.forecast.sky.SkyState
import com.kampplus.hava.ui.theme.HavaTheme
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Listedeki tek bir şehir: o şehrin gökyüzüne açılan küçük bir pencere.
 *
 * Arka plan, şehrin **şu anki** havası ve yerel saatiyle çizilir; Tokyo'nun kartında
 * gece ve yıldızlar, New York'unkinde öğle güneşi görünür. Hava henüz yüklenmediyse
 * nötr bir gökyüzü çizilir ve veri gelince yumuşakça gerçeğine döner.
 *
 * @param snapshot Şehrin anlık durumu; yüklenene kadar `null`.
 */
@Composable
fun CityCard(city: City, snapshot: CitySnapshot?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sky = snapshot?.let(SkyState::from) ?: SkyState.Default
    val condition = snapshot?.let { WeatherCondition.fromWeatherCode(it.weatherCode) }
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CARD_HEIGHT)
            .clip(shape)
            .clickable(onClick = onClick)
            // Ekran okuyucu kartı tek parça okur: "İstanbul, 16 derece, Kapalı".
            .semantics(mergeDescendants = true) {
                contentDescription = listOfNotNull(
                    city.name,
                    snapshot?.let { "${it.temperatureC.roundToInt()} derece" },
                    condition?.label
                ).joinToString(", ")
            }
    ) {
        // Parçacık yoğunluğu düşük: aynı anda birkaç kart canlandırılıyor.
        SkyBackground(state = sky, modifier = Modifier.fillMaxSize(), particleDensity = 0.45f)
        // Soldan sağa hafif karartma: açık renkli gündüz gökyüzünde de yazı okunur.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent)))
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = city.name,
                    style = MaterialTheme.typography.titleLarge.merge(ShadowStyle),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = city.subtitle,
                    style = MaterialTheme.typography.bodySmall.merge(ShadowStyle),
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (snapshot != null && condition != null) {
                    Text(
                        text = "${liveLocalTime(snapshot)} · ${condition.label}",
                        style = MaterialTheme.typography.labelLarge.merge(ShadowStyle),
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }
            Text(
                text = snapshot?.let { "${it.temperatureC.roundToInt()}°" }.orEmpty(),
                fontSize = 54.sp,
                fontWeight = FontWeight.Thin,
                color = Color.White,
                style = ShadowStyle.copy(fontFeatureSettings = "tnum")
            )
        }
    }
}

/**
 * Şehrin yerel saati, dakikada bir güncellenir. UTC farkı bilinmiyorsa verinin
 * alındığı andaki saat gösterilir.
 */
@Composable
private fun liveLocalTime(snapshot: CitySnapshot): String {
    val offset = snapshot.utcOffsetSeconds ?: return snapshot.localTime.format(TIME_FORMAT)
    val zone = ZoneOffset.ofTotalSeconds(offset)
    val time by produceState(initialValue = LocalDateTime.now(zone), offset) {
        while (true) {
            // Bir sonraki dakikanın başına kadar bekle; saniye saniye uyanmaya gerek yok.
            delay((60 - LocalDateTime.now(zone).second) * 1000L)
            value = LocalDateTime.now(zone)
        }
    }
    return time.format(TIME_FORMAT)
}

private val CARD_HEIGHT = 118.dp
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val ShadowStyle = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.3f), Offset(0f, 2f), blurRadius = 8f))

@Preview
@Composable
private fun CityCardPreview() {
    HavaTheme {
        CityCard(
            city = City(6, "Ankara", 39.93, 32.86),
            snapshot = CitySnapshot(
                temperatureC = 22.0,
                weatherCode = 1,
                isDay = true,
                localTime = LocalDateTime.of(2026, 10, 25, 14, 0)
            ),
            onClick = {}
        )
    }
}
