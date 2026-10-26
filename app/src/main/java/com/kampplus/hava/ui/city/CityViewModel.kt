package com.kampplus.hava.ui.city

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonSyntaxException
import com.kampplus.hava.data.CityCatalog
import com.kampplus.hava.data.OpenMeteoWeatherRepository
import com.kampplus.hava.data.WeatherRepository
import com.kampplus.hava.data.searchCities
import com.kampplus.hava.model.City
import com.kampplus.hava.model.CitySnapshot
import java.io.IOException
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * İl listesi ekranının beyni.
 *
 * Görev dağılımı:
 * - Ekran (Composable) yalnızca çizer.
 * - ViewModel state'i tutar ve kararları verir (arama, kartların havasını yükleme).
 * - Repository hava verisini getirir.
 *
 * Şehirler koda gömülü olduğu için arama **anında** ve ağsız çalışır; ağ yalnızca
 * kartlardaki mini gökyüzleri için gerekir.
 *
 * ViewModel ekran döndüğünde (Activity yeniden kurulurken) yaşamaya devam eder;
 * bu yüzden arama metni ve yüklenen havalar kaybolmaz.
 *
 * @param cities Listelenecek iller. Testlerde küçük bir liste verilebilir.
 * @param weatherRepository Varsayılanı gerçek servis. Testlerde sahte bir repository verilir.
 */
class CityViewModel(
    private val cities: List<City> = CityCatalog.cities,
    private val weatherRepository: WeatherRepository = OpenMeteoWeatherRepository()
) : ViewModel() {

    // State ViewModel'de MutableStateFlow ile tutulur.
    // Değiştirilebilir alan private'tır; dışarıya yalnızca okunabilir hâli açılır.
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow(cities)

    /** Arama metnine uyan iller; metin boşsa hepsi. */
    val results: StateFlow<List<City>> = _results.asStateFlow()

    private val _snapshots = MutableStateFlow<Map<Int, CitySnapshot>>(emptyMap())

    /**
     * Kartlardaki mini gökyüzleri için her ilin anlık durumu (anahtar: plaka kodu).
     * Listeden ayrı yüklenir: iller hemen görünür, gökyüzleri gelince canlanır.
     */
    val snapshots: StateFlow<Map<Int, CitySnapshot>> = _snapshots.asStateFlow()

    /** Şu an yüklenmekte olan iller; aynı il için ikinci bir istek atılmaz. */
    private val loading = mutableSetOf<Int>()

    init {
        loadSnapshots(cities)
    }

    /** Kullanıcı arama kutusuna her harf yazdığında çağrılır. */
    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        _results.value = searchCities(newQuery, cities)
        // Açılışta bağlantı yoksa yüklenemeyen havalar, kullanıcı yazarken yeniden denenir.
        loadSnapshots(_results.value)
    }

    /**
     * Henüz gökyüzü bilinmeyen illerin anlık durumunu getirir.
     *
     * İstekler [CHUNK_SIZE]'lık gruplar hâlinde atılır: 81 koordinatı tek adrese
     * sığdırmak yerine üç kısa istek, hem adres uzunluğunu hem de bir hatanın
     * etkisini sınırlar. Başarısız olursa sessiz kalınır: kart yine görünür,
     * yalnızca gökyüzü nötr olur.
     */
    private fun loadSnapshots(candidates: List<City>) {
        val missing = candidates.filter { it.plate !in _snapshots.value && it.plate !in loading }
        missing.chunked(CHUNK_SIZE).forEach { chunk ->
            loading += chunk.map { it.plate }
            viewModelScope.launch {
                try {
                    val loaded = weatherRepository.getSnapshots(chunk)
                    _snapshots.update { it + loaded }
                } catch (e: IOException) {
                    Log.w(TAG, "Kart havaları alınamadı: bağlantı hatası", e)
                } catch (e: HttpException) {
                    Log.w(TAG, "Kart havaları alınamadı: HTTP ${e.code()}", e)
                } catch (e: JsonSyntaxException) {
                    Log.w(TAG, "Kart havaları alınamadı: yanıt çözümlenemedi", e)
                } catch (e: DateTimeParseException) {
                    Log.w(TAG, "Kart havaları alınamadı: zaman çözümlenemedi", e)
                } finally {
                    loading -= chunk.map { it.plate }.toSet()
                }
            }
        }
    }

    private companion object {
        const val TAG = "CityViewModel"
        const val CHUNK_SIZE = 30
    }
}
