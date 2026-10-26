package com.kampplus.hava.ui.city

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kampplus.hava.R
import com.kampplus.hava.data.CityCatalog
import com.kampplus.hava.data.searchCities
import com.kampplus.hava.model.City
import com.kampplus.hava.model.CitySnapshot
import com.kampplus.hava.model.LovedOne
import com.kampplus.hava.model.LovedOneAdvice
import com.kampplus.hava.model.Severity
import com.kampplus.hava.model.adviseLovedOne
import com.kampplus.hava.ui.component.glassTextFieldColors
import com.kampplus.hava.ui.forecast.sky.SkyBackground
import com.kampplus.hava.ui.forecast.sky.SkyState
import com.kampplus.hava.ui.theme.HavaTheme
import kotlin.math.roundToInt

/** "Sevdiklerim" başlığı ve sağında "Ekle" düğmesi. */
@Composable
fun LovedOnesHeader(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color(0xFFFF8FA3), modifier = Modifier.size(18.dp))
        Text(
            text = stringResource(R.string.loved_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )
        TextButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
            Text(stringResource(R.string.loved_add), color = Color.White, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/** Henüz kimse eklenmemişken gösterilen, dokununca ekleme penceresini açan davet kartı. */
@Composable
fun LovedOnesEmptyCard(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CARD_SHAPE)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onAdd)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "💌  ${stringResource(R.string.loved_empty_title)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.loved_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f)
        )
    }
}

/**
 * Bir sevdiğimizin kartı: yaşadığı ilin canlı gökyüzü, şu anki sıcaklık ve yarına dair bir cümle.
 *
 * @param snapshot İlin anlık durumu; yüklenene kadar `null` ve kart "yükleniyor" der.
 */
@Composable
fun LovedOneCard(person: LovedOne, city: City, snapshot: CitySnapshot?, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val advice = adviseLovedOne(person, city.name, snapshot?.tomorrow)
    val sky = snapshot?.let(SkyState::from) ?: SkyState.Default

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 124.dp)
            .clip(CARD_SHAPE)
            .semantics(mergeDescendants = true) {
                contentDescription = "${person.label}, ${city.name}. ${advice?.message.orEmpty()}"
            }
    ) {
        SkyBackground(state = sky, modifier = Modifier.matchParentSize(), particleDensity = 0.45f)
        // Yazı, açık renkli gündüz gökyüzünde de okunsun diye soldan karartma.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.38f), Color.Black.copy(alpha = 0.12f))))
        )
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = advice?.emoji ?: "⏳", fontSize = 30.sp, modifier = Modifier.padding(top = 2.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${person.label} · ${city.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = advice?.message ?: stringResource(R.string.loved_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.95f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.loved_remove),
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                snapshot?.let {
                    Text(
                        text = "${it.temperatureC.roundToInt()}°",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Thin,
                        color = Color.White,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }
        }
        // Uyarının önemi, kartın sol kenarında ince bir renk çizgisiyle belirtilir.
        advice?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(4.dp)
                    .height(44.dp)
                    .background(accentColor(it), RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
            )
        }
    }
}

private fun accentColor(advice: LovedOneAdvice): Color = when (advice.severity) {
    Severity.ALERT -> Color(0xFFFFB25B)
    Severity.INFO -> Color(0xFF9AD4FF)
    Severity.NICE -> Color(0xFF6BE3A0)
}

/**
 * "Sevdiğini ekle" penceresi: kim olduğu (hazır seçenekler ya da serbest metin) ve yaşadığı il.
 *
 * İl, arama kutusuyla süzülen listeden seçilir; ana ekrandaki arama ile aynı kurallar geçerlidir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddLovedOneDialog(onDismiss: () -> Unit, onConfirm: (label: String, plate: Int) -> Unit) {
    var label by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var plate by rememberSaveable { mutableStateOf<Int?>(null) }
    val matches = remember(query) { searchCities(query, CityCatalog.cities) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF17223F),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text(stringResource(R.string.loved_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(
                    value = label,
                    onValueChange = { label = it.take(MAX_LABEL) },
                    placeholder = { Text(stringResource(R.string.loved_dialog_name_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = glassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LovedOne.relatives.forEach { relative ->
                        FilterChip(
                            selected = label == relative,
                            onClick = { label = relative },
                            label = { Text(relative) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha = 0.06f),
                                labelColor = Color.White.copy(alpha = 0.85f),
                                selectedContainerColor = Color.White.copy(alpha = 0.28f),
                                selectedLabelColor = Color.White
                            ),
                            border = null
                        )
                    }
                }
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.loved_dialog_city_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = glassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.height(168.dp)) {
                    items(items = matches, key = { it.plate }) { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { plate = city.plate }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = city.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (plate == city.plate) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (plate == city.plate) Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF6BE3A0))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = label.isNotBlank() && plate != null, onClick = { onConfirm(label, checkNotNull(plate)) }) {
                Text(stringResource(R.string.loved_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.loved_dialog_cancel), color = Color.White.copy(alpha = 0.8f)) }
        }
    )
}

private val CARD_SHAPE = RoundedCornerShape(28.dp)
private const val MAX_LABEL = 24

@Preview
@Composable
private fun LovedOneCardPreview() {
    HavaTheme {
        LovedOneCard(
            person = LovedOne(1, "Annem", 58),
            city = City(58, "Sivas", 39.75, 37.02),
            snapshot = null,
            onRemove = {}
        )
    }
}
