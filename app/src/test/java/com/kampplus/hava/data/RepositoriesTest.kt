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
}
