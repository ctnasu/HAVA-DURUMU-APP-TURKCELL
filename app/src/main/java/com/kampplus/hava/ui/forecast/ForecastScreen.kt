package com.kampplus.hava.ui.forecast

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.hava.R
import com.kampplus.hava.model.Activity
import com.kampplus.hava.model.ActivityModel
import com.kampplus.hava.model.Conditions
import com.kampplus.hava.model.CurrentWeather
import com.kampplus.hava.model.DailyForecast
import com.kampplus.hava.model.Forecast
import com.kampplus.hava.model.HourlyForecast
import com.kampplus.hava.model.WeatherCondition
import com.kampplus.hava.ui.LoadState
import com.kampplus.hava.ui.component.ErrorView
import com.kampplus.hava.ui.forecast.sky.SkyBackground
import com.kampplus.hava.ui.forecast.sky.SkyState
import com.kampplus.hava.ui.theme.HavaTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Tahmin ekranının bağlayıcı katmanı; ViewModel'i edinir ve state'i aktarır.
 * (Bkz. [com.kampplus.hava.ui.city.CityRoute] — aynı desen.)
 */
@Composable
fun ForecastRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ForecastViewModel = viewModel(),
    activityViewModel: ActivityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activityModels by activityViewModel.models.collectAsStateWithLifecycle()

    ForecastScreen(
        cityName = viewModel.cityName,
        subtitle = viewModel.subtitle,
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::onRetry,
        activityModels = activityModels,
        onActivityFeedback = activityViewModel::onFeedback,
        onActivityReset = activityViewModel::onReset,
        modifier = modifier
    )
}

/**
 * Bir şehrin hava tahmini, canlı bir gökyüzünün üstünde.
 *
 * Arka plan şehrin **yerel** saatine ve havasına göre çizilir (bkz. [SkyState]).
 * İçerik yarı saydam "cam" kartlarda durur; yazılar her zaman beyazdır.
 *
 * **Zaman makinesi:** saatlik grafikte bir saat seçilince gökyüzü ve büyük sıcaklık
 * o saate gider. Seçili saat bu yüzden grafikte değil, burada tutulur: hem arka plan
 * hem başlık hem grafik aynı değeri okur.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    cityName: String,
    subtitle: String,
    uiState: LoadState<Forecast>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    activityModels: Map<Activity, ActivityModel> = emptyMap(),
    onActivityFeedback: (Activity, Conditions, Boolean) -> Unit = { _, _, _ -> },
    onActivityReset: (Activity) -> Unit = {}
) {
    val forecast = (uiState as? LoadState.Success)?.data
    val upcoming = remember(forecast) { forecast?.let { upcomingHours(it, HOURLY_COUNT) }.orEmpty() }
    // Aktivite pencereleri "yarın sabah" gibi daha uzak saatlere de bakar.
    val activityHours = remember(forecast) { forecast?.let { upcomingHours(it, ACTIVITY_HOURS) }.orEmpty() }
    // 0 = şimdi. Yeni veri gelince zaman makinesi "şimdi"ye döner.
    var selectedHour by remember(forecast) { mutableIntStateOf(0) }
    val sky = when {
        forecast == null -> SkyState.Default
        selectedHour == 0 || selectedHour !in upcoming.indices -> SkyState.from(forecast)
        else -> upcoming[selectedHour].let { SkyState.at(forecast, it.time, it.weatherCode) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SkyBackground(state = sky, modifier = Modifier.fillMaxSize())
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                when (uiState) {
                    LoadState.Loading -> CircularProgressIndicator(color = Color.White)
                    LoadState.Empty -> ErrorView(message = "Bu şehir için tahmin bulunamadı.")
                    is LoadState.Error -> ErrorView(message = uiState.message, onRetry = onRetry)
                    is LoadState.Success -> ForecastContent(
                        cityName = cityName.ifEmpty { stringResource(R.string.detail_title) },
                        subtitle = subtitle,
                        forecast = uiState.data,
                        upcoming = upcoming,
                        activityHours = activityHours,
                        activityModels = activityModels,
                        selectedHour = selectedHour,
                        onSelectHour = { selectedHour = it },
                        onActivityFeedback = onActivityFeedback,
                        onActivityReset = onActivityReset
                    )
                }
            }
        }
    }
}

/**
 * Şu andan itibaren [count] saat. Zamanlar şehrin yerel saatindedir; telefonun saatini
 * değil, servisin bildirdiği "şimdi"yi kullanırız. (Tokyo'ya bakarken İstanbul saati yanlış olur.)
 */
private fun upcomingHours(forecast: Forecast, count: Int): List<HourlyForecast> {
    val hour = forecast.current.observedAt.truncatedTo(ChronoUnit.HOURS)
    return forecast.hourly.filter { !it.time.isBefore(hour) }.take(count)
}

@Composable
private fun ForecastContent(
    cityName: String,
    subtitle: String,
    forecast: Forecast,
    upcoming: List<HourlyForecast>,
    activityHours: List<HourlyForecast>,
    activityModels: Map<Activity, ActivityModel>,
    selectedHour: Int,
    onSelectHour: (Int) -> Unit,
    onActivityFeedback: (Activity, Conditions, Boolean) -> Unit,
    onActivityReset: (Activity) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = forecast.current.observedAt
    val today = forecast.daily.firstOrNull { it.date == now.toLocalDate() }
    // Zaman makinesinde seçili saat; 0 ise "şimdi" gösterilir.
    val travel = upcoming.getOrNull(selectedHour)?.takeIf { selectedHour > 0 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .fadingTopEdge()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Hero(
            cityName = cityName,
            subtitle = subtitle,
            temperatureC = travel?.temperatureC ?: forecast.current.temperatureC,
            weatherCode = travel?.weatherCode ?: forecast.current.weatherCode,
            day = travel?.let { hour -> forecast.daily.firstOrNull { it.date == hour.time.toLocalDate() } } ?: today,
            now = now,
            travelTime = travel?.time,
            onReturnToNow = { onSelectHour(0) },
            modifier = Modifier.staggeredEntrance(0)
        )
        GlassCard(modifier = Modifier.staggeredEntrance(1)) {
            MetricsRow(forecast.current)
        }
        if (upcoming.isNotEmpty()) {
            GlassCard(
                title = stringResource(R.string.detail_hourly),
                hint = stringResource(R.string.detail_time_machine_hint),
                modifier = Modifier.staggeredEntrance(2)
            ) {
                HourlyChart(
                    hours = upcoming,
                    selected = selectedHour,
                    onSelect = onSelectHour,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
        if (activityHours.size > Activity.entries.maxOf { it.windowHours }) {
            GlassCard(
                title = stringResource(R.string.detail_activities),
                hint = stringResource(R.string.detail_activities_hint),
                modifier = Modifier.staggeredEntrance(3)
            ) {
                ActivityWindows(
                    now = now,
                    hours = activityHours,
                    models = activityModels,
                    onFeedback = onActivityFeedback,
                    onReset = onActivityReset
                )
            }
        }
        if (forecast.daily.isNotEmpty()) {
            GlassCard(title = stringResource(R.string.detail_daily), modifier = Modifier.staggeredEntrance(4)) {
                DailyList(days = forecast.daily, currentTemperature = forecast.current.temperatureC)
            }
        }
        val sunrise = today?.sunrise
        val sunset = today?.sunset
        if (sunrise != null && sunset != null) {
            GlassCard(title = stringResource(R.string.detail_sun), modifier = Modifier.staggeredEntrance(5)) {
                SunSection(now = now, sunrise = sunrise, sunset = sunset)
            }
        }
    }
}

/**
 * Şehir adı, saat çipi, büyük sıcaklık, durum ve günün en yüksek/en düşük değeri.
 *
 * Zaman makinesinde [travelTime] doludur: değerler o saate aittir ve çip
 * "şimdiye dön" düğmesine dönüşür. Çip her iki durumda da aynı yüksekliktedir;
 * kaydırma sırasında altındaki grafik yer değiştirip parmaktan kaçmaz.
 */
@Composable
private fun Hero(
    cityName: String,
    subtitle: String,
    temperatureC: Double,
    weatherCode: Int,
    day: DailyForecast?,
    now: LocalDateTime,
    travelTime: LocalDateTime?,
    onReturnToNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Açılışta sıcaklık sıfırdan "sayarak" yükselir; sonraki değişimlerde (zaman
    // makinesi) yaya benzer hızlı bir geçiş yapar ki parmağı takip edebilsin.
    val shown = remember { Animatable(0f) }
    var firstRun by remember { mutableStateOf(true) }
    LaunchedEffect(temperatureC) {
        val spec: AnimationSpec<Float> = if (firstRun) {
            tween(durationMillis = 1100, easing = FastOutSlowInEasing)
        } else {
            spring(stiffness = Spring.StiffnessMediumLow)
        }
        firstRun = false
        shown.animateTo(temperatureC.toFloat(), spec)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = cityName,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }
        TimeChip(now = now, travelTime = travelTime, onReturnToNow = onReturnToNow, modifier = Modifier.padding(top = 10.dp))
        Text(
            text = "${shown.value.roundToInt()}°",
            fontSize = 104.sp,
            fontWeight = FontWeight.Thin,
            // Sayılar sabit genişlikte: animasyon sırasında yazı sağa sola kaymaz.
            style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
            modifier = Modifier.padding(start = 24.dp)
        )
        AnimatedContent(
            targetState = WeatherCondition.fromWeatherCode(weatherCode).label,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
            label = "conditionLabel"
        ) { label ->
            Text(text = label, style = MaterialTheme.typography.titleLarge)
        }
        day?.let {
            Text(
                text = stringResource(R.string.detail_high_low, degrees(it.maxTemperatureC), degrees(it.minTemperatureC)),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * Şimdi: şehrin yerel saati ("Şimdi · 20:49").
 * Zaman yolculuğunda: gidilen saat + geri dönüş ("⏱ Yarın 16:00 · Şimdiye dön").
 */
@Composable
private fun TimeChip(now: LocalDateTime, travelTime: LocalDateTime?, onReturnToNow: () -> Unit, modifier: Modifier = Modifier) {
    val traveling = travelTime != null
    val background by animateColorAsState(
        if (traveling) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.1f),
        label = "chipBackground"
    )
    val text = if (travelTime == null) {
        stringResource(R.string.detail_now_at, now.format(HOUR_FORMAT))
    } else {
        "⏱ ${relativeDay(travelTime.toLocalDate(), now.toLocalDate())} ${travelTime.format(HOUR_FORMAT)} · " +
            stringResource(R.string.detail_back_to_now)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(enabled = traveling, onClick = onReturnToNow)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

/** "Bugün", "Yarın" ya da gün adı ("Salı"). */
@Composable
private fun relativeDay(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.today)
    today.plusDays(1) -> stringResource(R.string.tomorrow)
    else -> date.format(DAY_FORMAT).replaceFirstChar(Char::titlecase)
}

@Composable
private fun MetricsRow(current: CurrentWeather) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        // Bu üç alan servis tarafından gönderilmeyebilir; yalnızca varsa çizilir.
        current.apparentTemperatureC?.let {
            Metric(stringResource(R.string.detail_feels_like), degrees(it))
        }
        current.humidityPercent?.let {
            Metric(stringResource(R.string.detail_humidity), "%$it")
        }
        current.windSpeedKmh?.let {
            Metric(stringResource(R.string.detail_wind), "${it.roundToInt()} km/sa")
        }
    }
}

/**
 * Haftalık liste. Bütün aralık çubukları haftanın en düşük ve en yüksek değerine
 * göre ölçeklenir; bu yüzden ölçek bir kez, liste düzeyinde hesaplanır.
 */
@Composable
private fun DailyList(days: List<DailyForecast>, currentTemperature: Double) {
    val weekMin = days.minOf { it.minTemperatureC }
    val weekMax = days.maxOf { it.maxTemperatureC }
    days.forEachIndexed { index, day ->
        if (index > 0) HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
        DailyRow(
            day = day,
            isToday = index == 0,
            weekMin = weekMin,
            weekMax = weekMax,
            currentTemperature = currentTemperature.takeIf { index == 0 }
        )
    }
}

/** Gün adı · emoji + yağış · en düşük · aralık çubuğu · en yüksek. */
@Composable
private fun DailyRow(day: DailyForecast, isToday: Boolean, weekMin: Double, weekMax: Double, currentTemperature: Double?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isToday) stringResource(R.string.today) else day.date.format(DAY_FORMAT).replaceFirstChar(Char::titlecase),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.width(92.dp)
        )
        Column(modifier = Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = WeatherCondition.fromWeatherCode(day.weatherCode).emoji, style = MaterialTheme.typography.titleLarge)
            percent(day.precipitationProbability)?.let {
                Text(text = it, style = MaterialTheme.typography.labelSmall, color = PrecipitationColor)
            }
        }
        Text(
            text = degrees(day.minTemperatureC),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp)
        )
        TemperatureRangeBar(
            min = day.minTemperatureC,
            max = day.maxTemperatureC,
            weekMin = weekMin,
            weekMax = weekMax,
            current = currentTemperature,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        )
        Text(
            text = degrees(day.maxTemperatureC),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
private fun SunSection(now: LocalDateTime, sunrise: LocalDateTime, sunset: LocalDateTime) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SunArc(now = now, sunrise = sunrise, sunset = sunset, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric(stringResource(R.string.detail_sunrise), sunrise.format(HOUR_FORMAT))
            Metric(stringResource(R.string.detail_sunset), sunset.format(HOUR_FORMAT))
        }
    }
}

/**
 * Gökyüzünün üstünde duran yarı saydam kart. Koyu ve hafif saydam zemin, açık
 * renkli gökyüzünde de beyaz yazının okunmasını sağlar.
 */
@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    hint: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0x2E0B1530))
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
            .padding(vertical = 14.dp)
    ) {
        title?.let {
            Text(
                text = it.uppercase(TURKISH),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.2.sp,
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = if (hint == null) 8.dp else 2.dp)
            )
        }
        hint?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
        content()
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
    }
}

/**
 * Bölümler ekrana sırayla, aşağıdan süzülerek gelir. Değerler `graphicsLayer`
 * içinde okunduğu için animasyon recomposition tetiklemez.
 */
@Composable
private fun Modifier.staggeredEntrance(index: Int): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * STAGGER_DELAY_MS)
        progress.animateTo(1f, tween(durationMillis = 550, easing = FastOutSlowInEasing))
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 32.dp.toPx()
    }
}

/**
 * Kaydırılan içerik üst çubuğun altında sert bir çizgiyle kesilmez, yumuşakça kaybolur.
 * İçerik ayrı bir katmanda çizilir, üst kenarı `DstIn` ile saydamlaştırılır.
 */
private fun Modifier.fadingTopEdge(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = 24.dp.toPx()),
            blendMode = BlendMode.DstIn
        )
    }

/** 18.4 → "18°". Kullanıcıya ondalık göstermeye gerek yok. */
private fun degrees(celsius: Double) = "${celsius.roundToInt()}°"

/** Yağış olasılığı yalnızca sıfırdan büyükse anlamlıdır. */
private fun percent(value: Int?) = value?.takeIf { it > 0 }?.let { "%$it" }

private const val HOURLY_COUNT = 24
private const val ACTIVITY_HOURS = 48
private const val STAGGER_DELAY_MS = 70L
private val TURKISH: Locale = Locale.forLanguageTag("tr")
private val HOUR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE", TURKISH)

@Preview
@Composable
private fun ForecastScreenPreview() {
    val now = LocalDateTime.of(2026, 1, 15, 14, 0)
    HavaTheme {
        ForecastScreen(
            cityName = "Ankara",
            subtitle = "Ankara, Türkiye",
            uiState = LoadState.Success(
                Forecast(
                    current = CurrentWeather(
                        temperatureC = 21.0,
                        weatherCode = 61,
                        observedAt = now,
                        apparentTemperatureC = 20.0,
                        humidityPercent = 45,
                        windSpeedKmh = 12.0
                    ),
                    hourly = List(24) {
                        HourlyForecast(
                            time = now.plusHours(it.toLong()),
                            temperatureC = 18.0 + 4 * kotlin.math.sin(it / 3.0),
                            weatherCode = if (it % 5 == 0) 61 else 2,
                            precipitationProbability = (it * 7) % 60
                        )
                    },
                    daily = List(7) {
                        DailyForecast(
                            date = LocalDate.of(2026, 1, 15).plusDays(it.toLong()),
                            minTemperatureC = 7.0 + it,
                            maxTemperatureC = 16.0 + it % 3 * 2,
                            weatherCode = 61,
                            precipitationProbability = 40,
                            sunrise = now.withHour(7).withMinute(12),
                            sunset = now.withHour(17).withMinute(48)
                        )
                    }
                )
            ),
            onBack = {},
            onRetry = {}
        )
    }
}
