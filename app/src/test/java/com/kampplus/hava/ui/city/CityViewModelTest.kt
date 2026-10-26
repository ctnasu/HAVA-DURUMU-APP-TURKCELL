package com.kampplus.hava.ui.city

import com.kampplus.hava.data.CityCatalog
import com.kampplus.hava.testing.FakeWeatherRepository
import com.kampplus.hava.testing.MainDispatcherRule
import com.kampplus.hava.testing.sampleCity
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * İl listesi ekranının davranışını doğrular.
 *
 * Temel kurallar:
 * - Kutu boşken tüm iller görünür; yazdıkça liste **anında** süzülür (ağ gerekmez).
 * - Kartların havası ayrı ve gruplar hâlinde yüklenir; yüklenemezse liste yine çalışır.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val weatherRepository = FakeWeatherRepository()

    private val smallList = listOf(
        sampleCity(6, "Ankara"),
        sampleCity(7, "Antalya"),
        sampleCity(34, "İstanbul"),
        sampleCity(35, "İzmir")
    )

    private fun createViewModel(cities: List<com.kampplus.hava.model.City> = smallList) = CityViewModel(cities, weatherRepository)

    @Test
    fun `baslangicta tum iller listelenir`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(smallList, viewModel.results.value)
        assertEquals("", viewModel.query.value)
    }

    @Test
    fun `yazdikca liste aninda suzulur`() = runTest {
        val viewModel = createViewModel()

        // advanceUntilIdle çağrılmıyor: arama beklemesizdir, ağa bağlı değildir.
        viewModel.onQueryChange("Ank")

        assertEquals(listOf("Ankara"), viewModel.results.value.map { it.name })
    }

    @Test
    fun `arama kelimenin ortasinda da eslesir`() = runTest {
        val viewModel = createViewModel()

        viewModel.onQueryChange("an")

        // "an" hem Ankara hem Antalya hem de İstanbul'da (istANbul) geçer; baştakiler öne gelir.
        assertEquals(listOf("Ankara", "Antalya", "İstanbul"), viewModel.results.value.map { it.name })
    }

    @Test
    fun `eslesme yoksa liste bos doner`() = runTest {
        val viewModel = createViewModel()

        viewModel.onQueryChange("xyz")

        // Ekran bu durumda boş durum görünümünü gösterir.
        assertTrue(viewModel.results.value.isEmpty())
    }

    @Test
    fun `arama temizlenince tum liste geri gelir`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("Ankara")

        viewModel.onQueryChange("")

        assertEquals(smallList, viewModel.results.value)
    }

    @Test
    fun `81 ilin havasi uc grup halinde yuklenir`() = runTest {
        val viewModel = CityViewModel(CityCatalog.cities, weatherRepository)
        advanceUntilIdle()

        // 30 + 30 + 21: tek dev istek yerine üç kısa istek.
        assertEquals(3, weatherRepository.snapshotCallCount)
        assertEquals(81, viewModel.snapshots.value.size)
    }

    @Test
    fun `yuklenmis iller icin tekrar istek atilmaz`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val calls = weatherRepository.snapshotCallCount

        viewModel.onQueryChange("Ankara")
        viewModel.onQueryChange("")
        advanceUntilIdle()

        assertEquals(calls, weatherRepository.snapshotCallCount)
    }

    @Test
    fun `hava yuklenemezse liste calismaya devam eder`() = runTest {
        weatherRepository.error = IOException("no network")

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Kartlar nötr gökyüzüyle görünür, arama sorunsuz çalışır.
        assertTrue(viewModel.snapshots.value.isEmpty())
        viewModel.onQueryChange("izm")
        assertEquals(listOf("İzmir"), viewModel.results.value.map { it.name })
    }

    @Test
    fun `baglanti gelince yazarken yuklenemeyen havalar yeniden denenir`() = runTest {
        weatherRepository.error = IOException("no network")
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.snapshots.value.isEmpty())

        // Bağlantı geri geldi; kullanıcı yazmaya başlayınca eksik havalar tekrar istenir.
        weatherRepository.error = null
        viewModel.onQueryChange("ank")
        advanceUntilIdle()

        assertEquals(setOf(6), viewModel.snapshots.value.keys)
    }
}
