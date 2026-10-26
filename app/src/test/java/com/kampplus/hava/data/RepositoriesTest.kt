package com.kampplus.hava.data

import com.kampplus.hava.model.Activity
import com.kampplus.hava.model.ActivityModel
import com.kampplus.hava.model.Conditions
import com.kampplus.hava.testing.InMemoryStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Öğrenilenlerin ve sevdiklerimin kalıcı saklanması: uygulama kapanıp açılınca kaybolmamalı. */
class RepositoriesTest {

    private val cold = Conditions(temperatureC = 4.0, windKmh = 5.0, rainProbability = 0.0, humidity = 0.5, isDark = false)

    @Test
    fun `ogrenilen model uygulama yeniden acilinca korunur`() {
        val storage = InMemoryStorage()
        ActivityModelRepository(storage).feedback(Activity.RUNNING, cold, good = true)

        val reopened = ActivityModelRepository(storage)

        val model = reopened.models.value.getValue(Activity.RUNNING)
        assertEquals(1, model.samples)
        assertTrue(checkNotNull(model.idealTemperatureC) < 12.0)
    }

    @Test
    fun `her aktivite ayri ogrenir`() {
        val repository = ActivityModelRepository(InMemoryStorage())

        repository.feedback(Activity.RUNNING, cold, good = true)

        assertEquals(1, repository.models.value.getValue(Activity.RUNNING).samples)
        assertEquals(0, repository.models.value.getValue(Activity.CYCLING).samples)
    }

    @Test
    fun `sifirlama baslangic bilgisine doner ve kaydi siler`() {
        val storage = InMemoryStorage()
        val repository = ActivityModelRepository(storage)
        repository.feedback(Activity.RUNNING, cold, good = true)

        repository.reset(Activity.RUNNING)

        assertEquals(ActivityModel.initial(Activity.RUNNING).weights, repository.models.value.getValue(Activity.RUNNING).weights)
        assertEquals(0, ActivityModelRepository(storage).models.value.getValue(Activity.RUNNING).samples)
    }

    @Test
    fun `bozuk model kaydi baslangic bilgisine dusulur`() {
        val storage = InMemoryStorage(mapOf("activity_model_RUNNING" to "bozuk"))

        val model = ActivityModelRepository(storage).models.value.getValue(Activity.RUNNING)

        assertEquals(0, model.samples)
    }

    @Test
    fun `sevdikler eklenir silinir ve saklanir`() {
        val storage = InMemoryStorage()
        val repository = LovedOnesRepository(storage) { 1_000L }
        repository.add("Annem", 58)
        repository.add("Kardeşim", 35)

        val reopened = LovedOnesRepository(storage)

        assertEquals(listOf("Annem" to 58, "Kardeşim" to 35), reopened.lovedOnes.value.map { it.label to it.plate })

        reopened.remove(reopened.lovedOnes.value.first().id)
        assertEquals(listOf("Kardeşim"), LovedOnesRepository(storage).lovedOnes.value.map { it.label })
    }

    @Test
    fun `ayni anda eklenen kayitlarin kimligi cakismaz`() {
        val repository = LovedOnesRepository(InMemoryStorage()) { 5L }

        repository.add("Annem", 58)
        repository.add("Babam", 58)

        assertEquals(2, repository.lovedOnes.value.map { it.id }.toSet().size)
    }

    @Test
    fun `bos ad eklenmez ve ozel karakterler temizlenir`() {
        val storage = InMemoryStorage()
        val repository = LovedOnesRepository(storage)

        repository.add("   ", 6)
        repository.add("Anne\tm\nx", 6)

        // Sekme ve satır sonu bir kaydı başka bir kayda taşıyamaz.
        assertEquals(listOf("Anne m x"), LovedOnesRepository(storage).lovedOnes.value.map { it.label })
    }

    @Test
    fun `bozuk satirlar atlanir digerleri okunur`() {
        val storage = InMemoryStorage(mapOf("loved_ones" to "1\t58\tAnnem\nçöp\nx\t1\tHatalı\n3\t35\tBabam"))

        val loved = LovedOnesRepository(storage).lovedOnes.value

        assertEquals(listOf("Annem", "Babam"), loved.map { it.label })
    }
}
