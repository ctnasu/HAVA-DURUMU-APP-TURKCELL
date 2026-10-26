package com.kampplus.hava.data

import com.kampplus.hava.model.Activity
import com.kampplus.hava.model.ActivityModel
import com.kampplus.hava.model.Conditions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Kullanıcının her aktivite için öğrettiği modelleri tutar ve kalıcı saklar.
 *
 * Modeller uygulama genelindedir: koşu için öğrendiğin zevk, hangi ile baksan geçerlidir.
 * Kayıt bozuksa ya da hiç yoksa model başlangıç bilgisiyle kurulur.
 */
class ActivityModelRepository(private val storage: KeyValueStorage) {

    private val _models = MutableStateFlow(Activity.entries.associateWith(::load))
    val models: StateFlow<Map<Activity, ActivityModel>> = _models.asStateFlow()

    /** Kullanıcı bu koşulların kendisine uygun olduğunu ([good]) ya da olmadığını söyledi. */
    fun feedback(activity: Activity, conditions: Conditions, good: Boolean) {
        val learned = _models.value.getValue(activity).learn(conditions, good, activity)
        storage.write(key(activity), learned.serialize())
        _models.value = _models.value + (activity to learned)
    }

    /** Öğrenilenleri unutup başlangıç bilgisine döner. */
    fun reset(activity: Activity) {
        storage.remove(key(activity))
        _models.value = _models.value + (activity to ActivityModel.initial(activity))
    }

    private fun load(activity: Activity): ActivityModel =
        storage.read(key(activity))?.let(ActivityModel::parse) ?: ActivityModel.initial(activity)

    private fun key(activity: Activity) = "activity_model_${activity.name}"
}
