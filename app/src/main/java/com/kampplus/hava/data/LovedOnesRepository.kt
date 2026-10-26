package com.kampplus.hava.data

import com.kampplus.hava.model.LovedOne
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sevdiklerimin listesi; telefonda kalıcı saklanır.
 *
 * Kayıt biçimi her satırda `kimlik<TAB>plaka<TAB>ad`'dır. Ad kullanıcıdan geldiği için
 * sekme ve satır sonu karakterleri temizlenir; böylece bir satır başka bir satırı bozamaz.
 */
class LovedOnesRepository(private val storage: KeyValueStorage, private val clock: () -> Long = System::currentTimeMillis) {

    private val _lovedOnes = MutableStateFlow(load())
    val lovedOnes: StateFlow<List<LovedOne>> = _lovedOnes.asStateFlow()

    fun add(label: String, plate: Int) {
        val clean = label.sanitize()
        if (clean.isEmpty()) return
        // Aynı milisaniyede iki kayıt eklenirse kimlikler çakışmasın.
        val id = generateSequence(clock()) { it + 1 }.first { candidate -> _lovedOnes.value.none { it.id == candidate } }
        update(_lovedOnes.value + LovedOne(id, clean, plate))
    }

    fun remove(id: Long) = update(_lovedOnes.value.filterNot { it.id == id })

    private fun update(list: List<LovedOne>) {
        _lovedOnes.value = list
        storage.write(KEY, list.joinToString("\n") { "${it.id}\t${it.plate}\t${it.label}" })
    }

    private fun load(): List<LovedOne> = storage.read(KEY).orEmpty().lines().mapNotNull { line ->
        val (id, plate, label) = line.split("\t").takeIf { it.size == 3 } ?: return@mapNotNull null
        LovedOne(id.toLongOrNull() ?: return@mapNotNull null, label, plate.toIntOrNull() ?: return@mapNotNull null)
    }

    private fun String.sanitize() = replace(Regex("[\\t\\r\\n]+"), " ").trim().take(MAX_LABEL_LENGTH)

    private companion object {
        const val KEY = "loved_ones"
        const val MAX_LABEL_LENGTH = 24
    }
}
