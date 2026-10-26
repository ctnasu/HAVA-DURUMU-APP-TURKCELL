package com.kampplus.hava.ui.city

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.hava.R
import com.kampplus.hava.data.CityCatalog
import com.kampplus.hava.model.City
import com.kampplus.hava.model.CitySnapshot
import com.kampplus.hava.model.LovedOne
import com.kampplus.hava.ui.component.EmptyView
import com.kampplus.hava.ui.component.glassTextFieldColors
import com.kampplus.hava.ui.theme.HavaTheme

/**
 * İl listesi ekranının "bağlayıcı" katmanı.
 *
 * Tek işi ViewModel'i edinip state'i toplamak ve [CityScreen]'e aktarmaktır.
 * Bu ayrım sayesinde [CityScreen] ViewModel tanımaz: önizlemede ve testte
 * elle state verilerek çalıştırılabilir.
 *
 * `collectAsStateWithLifecycle`: StateFlow'u Compose state'ine
 * çevirir ve uygulama arka plandayken dinlemeyi durdurur.
 */
@Composable
fun CityRoute(
    onCityClick: (City) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CityViewModel = viewModel(),
    lovedOnesViewModel: LovedOnesViewModel = viewModel()
) {
    val lovedOnes by lovedOnesViewModel.lovedOnes.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val cities by viewModel.results.collectAsStateWithLifecycle()
    val snapshots by viewModel.snapshots.collectAsStateWithLifecycle()

    CityScreen(
        query = query,
        cities = cities,
        snapshots = snapshots,
        onQueryChange = viewModel::onQueryChange,
        onCityClick = onCityClick,
        lovedOnes = lovedOnes,
        onAddLovedOne = lovedOnesViewModel::add,
        onRemoveLovedOne = lovedOnesViewModel::remove,
        modifier = modifier
    )
}

/**
 * İl listesi ekranı: koyu bir gece zemini üstünde, her il kendi gökyüzüyle.
 *
 * Kutu boşken 81 ilin tamamı, yazınca eşleşenler gösterilir; eşleşme yoksa açıklayıcı
 * bir boş durum çıkar. State tutmaz, yalnızca aldığı değerleri çizer ve olayları
 * yukarı bildirir.
 */
@Composable
fun CityScreen(
    query: String,
    cities: List<City>,
    snapshots: Map<Int, CitySnapshot>,
    onQueryChange: (String) -> Unit,
    onCityClick: (City) -> Unit,
    modifier: Modifier = Modifier,
    lovedOnes: List<LovedOne> = emptyList(),
    onAddLovedOne: (label: String, plate: Int) -> Unit = { _, _ -> },
    onRemoveLovedOne: (Long) -> Unit = {}
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    if (showAddDialog) {
        AddLovedOneDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { label, plate ->
                onAddLovedOne(label, plate)
                showAddDialog = false
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF060A1A), Color(0xFF17223F))))
    ) {
        Scaffold(containerColor = Color.Transparent, contentColor = Color.White) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Header(query = query, resultCount = cities.size)
                SearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (cities.isEmpty()) {
                        EmptyView(
                            title = "İl bulunamadı",
                            message = "Yazımı kontrol et ya da plaka kodunu dene (ör. 34)."
                        )
                    } else {
                        CityList(
                            cities = cities,
                            snapshots = snapshots,
                            onCityClick = onCityClick,
                            // Arama sürerken sevdikler gizlenir: sonuçlar ekranın başında görünsün.
                            lovedOnes = if (query.isBlank()) lovedOnes else null,
                            onAddLovedOne = { showAddDialog = true },
                            onRemoveLovedOne = onRemoveLovedOne
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(query: String, resultCount: Int) {
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp)) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (query.isBlank()) {
                stringResource(R.string.city_featured_title)
            } else {
                stringResource(R.string.city_results_count, resultCount)
            },
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

/**
 * Cam görünümlü arama kutusu. Liste her tuşta anında süzülür; klavyedeki "Ara" tuşu
 * yalnızca klavyeyi kapatır.
 */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val keyboardController = LocalSoftwareKeyboardController.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            // Temizle düğmesi yalnızca kutu doluyken görünür.
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        colors = glassTextFieldColors(),
        // Klavyedeki Enter tuşu "Ara" olur.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
    )
}

/**
 * Kaydırılabilir il listesi.
 *
 * `LazyColumn` tembeldir: 81 il olsa da yalnızca ekranda görünen kartlar (ve her
 * birinin canlı gökyüzü) çizilir; kaybolanlar bırakılır.
 *
 * `key = { it.plate }`: liste değiştiğinde Compose hangi kartın hangisi olduğunu bilir.
 * `animateItem()`: arama sonucu değişince kartlar yerlerine kayarak/solarak geçer.
 */
@Composable
private fun CityList(
    cities: List<City>,
    snapshots: Map<Int, CitySnapshot>,
    onCityClick: (City) -> Unit,
    lovedOnes: List<LovedOne>?,
    onAddLovedOne: () -> Unit,
    onRemoveLovedOne: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // `null`: arama sürüyor, sevdikler bölümü hiç gösterilmez.
        if (lovedOnes != null) {
            item(key = "loved-header") { LovedOnesHeader(onAdd = onAddLovedOne, modifier = Modifier.animateItem()) }
            if (lovedOnes.isEmpty()) {
                item(key = "loved-empty") { LovedOnesEmptyCard(onAdd = onAddLovedOne, modifier = Modifier.animateItem()) }
            } else {
                items(items = lovedOnes, key = { "loved-${it.id}" }) { person ->
                    // Plaka kodu kataloğun dışındaysa (bozuk kayıt) kart hiç çizilmez.
                    CityCatalog.cities.firstOrNull { it.plate == person.plate }?.let { city ->
                        LovedOneCard(
                            person = person,
                            city = city,
                            snapshot = snapshots[city.plate],
                            onRemove = { onRemoveLovedOne(person.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
            item(key = "all-header") {
                Text(
                    text = stringResource(R.string.all_cities_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(start = 4.dp, top = 8.dp)
                        .animateItem()
                )
            }
        }
        items(items = cities, key = { it.plate }) { city ->
            CityCard(
                city = city,
                snapshot = snapshots[city.plate],
                onClick = { onCityClick(city) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Preview
@Composable
private fun CityScreenPreview() {
    HavaTheme {
        CityScreen(
            query = "",
            cities = CityCatalog.cities.take(5),
            snapshots = emptyMap(),
            onQueryChange = {},
            onCityClick = {}
        )
    }
}

@Preview
@Composable
private fun CityScreenEmptyPreview() {
    HavaTheme {
        CityScreen(
            query = "xyz",
            cities = emptyList(),
            snapshots = emptyMap(),
            onQueryChange = {},
            onCityClick = {}
        )
    }
}
