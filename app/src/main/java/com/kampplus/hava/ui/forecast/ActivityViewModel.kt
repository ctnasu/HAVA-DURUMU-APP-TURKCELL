package com.kampplus.hava.ui.forecast

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.kampplus.hava.data.ActivityModelRepository
import com.kampplus.hava.data.SharedPreferencesStorage
import com.kampplus.hava.model.Activity
import com.kampplus.hava.model.ActivityModel
import com.kampplus.hava.model.Conditions
import kotlinx.coroutines.flow.StateFlow

/**
 * Kullanıcının aktivite modellerini ekrana taşır.
 *
 * Modeller şehirden bağımsızdır ve telefonda saklanır: bu yüzden ViewModel tahmin
 * ekranından ayrıdır ve `Application` ister (depolama için bir `Context` gerekir).
 */
class ActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ActivityModelRepository(SharedPreferencesStorage(application))

    val models: StateFlow<Map<Activity, ActivityModel>> = repository.models

    /** "İyiydi" / "Kötüydü" düğmeleri buraya bağlanır. */
    fun onFeedback(activity: Activity, conditions: Conditions, good: Boolean) = repository.feedback(activity, conditions, good)

    fun onReset(activity: Activity) = repository.reset(activity)
}
