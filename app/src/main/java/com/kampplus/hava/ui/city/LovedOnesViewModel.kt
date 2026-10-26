package com.kampplus.hava.ui.city

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.kampplus.hava.data.LovedOnesRepository
import com.kampplus.hava.data.SharedPreferencesStorage
import com.kampplus.hava.model.LovedOne
import kotlinx.coroutines.flow.StateFlow

/** "Sevdiklerim" listesini ekrana taşır; liste telefonda saklanır. */
class LovedOnesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LovedOnesRepository(SharedPreferencesStorage(application))

    val lovedOnes: StateFlow<List<LovedOne>> = repository.lovedOnes

    fun add(label: String, plate: Int) = repository.add(label, plate)

    fun remove(id: Long) = repository.remove(id)
}
