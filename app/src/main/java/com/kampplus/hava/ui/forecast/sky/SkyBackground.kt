package com.kampplus.hava.ui.forecast.sky

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Hava durumuna ve günün saatine göre canlanan gökyüzü.
 *
 * Gökyüzü katmanlardan oluşur (yıldız, güneş, ay, bulut, yağmur, kar, sis, şimşek).
 * Her katmanın bir görünürlük değeri vardır ve [state] değişince bu değerler
 * **animasyonla** değişir. Böylece zaman makinesinde saat kaydırılırken yağmur
 * birden belirmez, yavaşça başlar; güneş gökyüzünde yürür.
 *
 * Performans notu: Zaman ve görünürlükler `State` içinde tutulur ve **yalnızca çizim
 * aşamasında** okunur. Böylece her karede recomposition olmaz; Compose sadece
 * Canvas'ı yeniden çizer. Parçacıklar bir kez üretilir ve konumları zamandan
 * hesaplanır (nesne oluşturma yok, çöp toplayıcıya yük yok).
 *
 * @param particleDensity Parçacık yoğunluğu (0..1). Küçük kartlarda düşük tutulur.
 */
@Composable
fun SkyBackground(state: SkyState, modifier: Modifier = Modifier, particleDensity: Float = 1f) {
    val colorSpec = tween<Color>(durationMillis = 700)
    val top by animateColorAsState(state.topColor, colorSpec, label = "skyTop")
    val bottom by animateColorAsState(state.bottomColor, colorSpec, label = "skyBottom")

    val night = state.phase == DayPhase.NIGHT
    val twilight = state.phase == DayPhase.DAWN || state.phase == DayPhase.DUSK
    val clearish = state.scene == SkyScene.CLEAR || state.scene == SkyScene.CLOUDY
    val stars by layer(if (night && clearish) 1f else 0f)
    val sun by layer(
        when {
            night -> 0f
            state.scene == SkyScene.CLEAR -> 1f
            state.scene == SkyScene.CLOUDY -> 0.35f
            else -> 0f
        }
    )
    val moon by layer(if (night && clearish) (if (state.scene == SkyScene.CLEAR) 1f else 0.4f) else 0f)
    val glow by layer(if (twilight) 1f else 0f)
    val clouds by layer(if (state.scene == SkyScene.CLEAR) 0f else 1f)
    val rain by layer(if (state.scene == SkyScene.RAIN || state.scene == SkyScene.STORM) 1f else 0f)
    val storm by layer(if (state.scene == SkyScene.STORM) 1f else 0f)
    val snow by layer(if (state.scene == SkyScene.SNOW) 1f else 0f)
    val fog by layer(if (state.scene == SkyScene.FOG) 1f else 0f)
    val sunPosition by animateFloatAsState(state.sunProgress, tween(durationMillis = 700), label = "sunPosition")

    val time = rememberFrameSeconds()
    val particles = remember { Particles() }

    Canvas(modifier = modifier) {
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
        val t = time.value

        drawStars(particles.stars, t, stars, particleDensity)
        drawHorizonGlow(glow)
        drawSun(t, sunPosition, sun)
        drawMoon(moon)
        drawClouds(particles.clouds, t, clouds, dark = storm > 0.5f, night = night)
        // Fırtınada yağmur yoğunlaşır: 120 damladan tüm damlalara.
        val drops = (120 + (particles.drops.size - 120) * storm) * particleDensity
        drawRain(particles.drops, t, rain, count = drops.toInt())
        drawLightning(t, storm)
        drawSnow(particles.flakes, t, snow, particleDensity)
        drawFog(t, fog)
    }
}

/** Bir katmanın görünürlüğü; hedef değişince yumuşakça yeni değere geçer. */
@Composable
private fun layer(target: Float, spec: AnimationSpec<Float> = tween(durationMillis = 700)): State<Float> =
    animateFloatAsState(target, spec, label = "skyLayer")

/** Ekranın yenilenme hızında ilerleyen saniye sayacı. */
@Composable
private fun rememberFrameSeconds(): State<Float> {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { seconds.floatValue = (it - start) / 1_000_000_000f }
        }
    }
    return seconds
}

/**
 * Bir parçacık; tüm değerler 0..1 aralığında, ekran boyutundan bağımsız.
 * Gerçek konum çizim anında `size` ile çarpılarak bulunur.
 */
private class Particle(val x: Float, val y: Float, val speed: Float, val size: Float, val phase: Float)

private class Particles(seed: Int = 7) {
    private val random = Random(seed)
    private fun make(count: Int, speed: ClosedFloatingPointRange<Float>) = List(count) {
        Particle(
            x = random.nextFloat(),
            y = random.nextFloat(),
            speed = speed.start + random.nextFloat() * (speed.endInclusive - speed.start),
            size = random.nextFloat(),
            phase = random.nextFloat() * 6.28f
        )
    }

    val drops = make(170, 0.9f..1.5f)
    val flakes = make(90, 0.05f..0.12f)
    val stars = make(70, 0.6f..1.8f)
    val clouds = make(5, 0.008f..0.02f)
}

/** Görünürlüğü sıfıra yakın katmanlar hiç çizilmez. */
private const val INVISIBLE = 0.01f

private fun DrawScope.drawStars(stars: List<Particle>, t: Float, alpha: Float, density: Float) {
    if (alpha < INVISIBLE) return
    for (i in 0 until (stars.size * density).toInt()) {
        val star = stars[i]
        // Yıldızlar yalnızca gökyüzünün üst yarısında, kendi hızlarında parıldar.
        val twinkle = 0.25f + 0.75f * abs(sin(t * star.speed + star.phase))
        drawCircle(
            color = Color.White.copy(alpha = twinkle * 0.9f * alpha),
            radius = 1f + star.size * 2f,
            center = Offset(star.x * size.width, star.y * size.height * 0.55f)
        )
    }
}

/**
 * Güneş gün boyunca soldan sağa bir yay çizerek yürür: doğuşta solda ve alçakta,
 * öğlende tepede, batışta sağda.
 */
private fun DrawScope.drawSun(t: Float, progress: Float, alpha: Float) {
    if (alpha < INVISIBLE) return
    val unit = bodyUnit()
    val center = Offset(
        x = size.width * (0.12f + 0.76f * progress),
        y = size.height * (0.34f - 0.22f * sin(PI * progress).toFloat())
    )
    // Yavaşça "nefes alan" bir parlama.
    val pulse = 1f + 0.04f * sin(t * 0.8f)
    val glowRadius = unit * 0.75f * pulse
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x66FFF3C4), Color(0x22FFE08A), Color.Transparent),
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center,
        alpha = alpha
    )
    drawCircle(Color(0xFFFFF4D6), radius = unit * 0.07f, center = center, alpha = alpha)
}

/**
 * Güneş ve ay gibi gök cisimlerinin ölçü birimi. Tam ekranda ekran genişliğidir; alçak
 * ve geniş bir kartta ise yüksekliğe bağlanır, böylece cisimler kartı taşırmaz.
 */
private fun DrawScope.bodyUnit() = minOf(size.width, size.height * 1.2f)

private fun DrawScope.drawMoon(alpha: Float) {
    if (alpha < INVISIBLE) return
    val center = Offset(size.width * 0.8f, size.height * 0.16f)
    val radius = bodyUnit() * 0.055f
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0x33DDE6FF), Color.Transparent), center, radius * 6),
        radius = radius * 6,
        center = center,
        alpha = alpha
    )
    drawCircle(Color(0xFFF1F0E6), radius = radius, center = center, alpha = alpha)
    // Ayın bir kenarına hafif gölge: düz bir daire yerine hacimli görünür.
    drawCircle(
        Color(0x33000000),
        radius = radius * 0.9f,
        center = center + Offset(radius * 0.45f, -radius * 0.2f),
        alpha = alpha
    )
}

private fun DrawScope.drawHorizonGlow(alpha: Float) {
    if (alpha < INVISIBLE) return
    val center = Offset(size.width * 0.5f, size.height * 0.62f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x55FFC98A), Color(0x22FF9A6A), Color.Transparent),
            center = center,
            radius = size.width
        ),
        radius = size.width,
        center = center,
        alpha = alpha
    )
}

/**
 * Her bulut, birbirine binen yumuşak kenarlı "puf"lardan oluşur ve yavaşça sağa kayar.
 * Puflar düz daire değil radyal gradyandır; kenarları gökyüzüne erir, sert çizgi oluşmaz.
 */
private fun DrawScope.drawClouds(clouds: List<Particle>, t: Float, visibility: Float, dark: Boolean, night: Boolean) {
    if (visibility < INVISIBLE) return
    val alpha = when {
        dark -> 0.10f
        night -> 0.08f
        else -> 0.16f
    } * visibility
    val color = (if (dark) Color(0xFFA0A8B8) else Color.White).copy(alpha = alpha)
    clouds.forEach { cloud ->
        // 1.6 genişliğinde bir döngü: bulut ekranın solundan girip sağından tamamen çıkar.
        val x = ((cloud.x + t * cloud.speed) % 1.6f - 0.3f) * size.width
        val y = (0.02f + cloud.y * 0.3f) * size.height
        val r = size.width * (0.16f + cloud.size * 0.1f)
        drawPuff(color, Offset(x, y), r)
        drawPuff(color, Offset(x - r * 0.9f, y + r * 0.25f), r * 0.8f)
        drawPuff(color, Offset(x + r * 0.95f, y + r * 0.2f), r * 0.85f)
    }
}

private fun DrawScope.drawPuff(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(0f to color, 0.55f to color.copy(alpha = color.alpha * 0.7f), 1f to Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawRain(drops: List<Particle>, t: Float, alpha: Float, count: Int) {
    if (alpha < INVISIBLE) return
    val slant = size.width * 0.04f
    for (i in 0 until count.coerceAtMost(drops.size)) {
        val drop = drops[i]
        val length = size.height * (0.025f + drop.size * 0.03f)
        val progress = (drop.y + t * drop.speed * 0.6f) % 1f
        val y = progress * (size.height + length) - length
        val x = drop.x * (size.width + slant) - slant * progress
        drawLine(
            color = Color.White.copy(alpha = (0.18f + drop.size * 0.3f) * alpha),
            start = Offset(x, y),
            end = Offset(x - slant * 0.08f, y + length),
            strokeWidth = 1.5f + drop.size * 1.5f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSnow(flakes: List<Particle>, t: Float, alpha: Float, density: Float) {
    if (alpha < INVISIBLE) return
    for (i in 0 until (flakes.size * density).toInt()) {
        val flake = flakes[i]
        val y = ((flake.y + t * flake.speed) % 1f) * size.height
        // Kar taneleri düşerken sağa sola salınır.
        val x = flake.x * size.width + sin(t * 0.9f + flake.phase) * size.width * 0.03f
        drawCircle(
            color = Color.White.copy(alpha = (0.5f + flake.size * 0.5f) * alpha),
            radius = 2f + flake.size * 4f,
            center = Offset(x, y)
        )
    }
}

/** Yaklaşık 7 saniyede bir, iki kısa parlamadan oluşan şimşek. */
private fun DrawScope.drawLightning(t: Float, alpha: Float) {
    if (alpha < INVISIBLE) return
    val cycle = t % 7f
    val flash = when {
        cycle < 0.07f -> 0.45f
        cycle in 0.16f..0.22f -> 0.28f
        else -> return
    }
    drawRect(Color.White.copy(alpha = flash * alpha))
}

private fun DrawScope.drawFog(t: Float, alpha: Float) {
    if (alpha < INVISIBLE) return
    repeat(4) { band ->
        val y = size.height * (0.15f + band * 0.2f)
        val drift = sin(t * 0.15f + band) * size.width * 0.15f
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color(0x30FFFFFF), Color.Transparent),
                startX = drift - size.width * 0.2f,
                endX = drift + size.width * 1.2f
            ),
            topLeft = Offset(0f, y),
            size = Size(size.width, size.height * 0.12f),
            alpha = alpha
        )
    }
}
