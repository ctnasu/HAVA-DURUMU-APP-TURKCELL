package com.kampplus.hava.data

import android.content.Context

/**
 * Küçük, kalıcı anahtar-değer deposu.
 *
 * Neden arayüz? Öğrenilen modeller ve sevdiklerimiz telefonda saklanır, ama mantığı
 * (öğrenme, ekleme/silme) test ederken gerçek bir Android deposuna ihtiyaç yoktur:
 * testlerde bellekteki sahte bir uygulama verilir.
 */
interface KeyValueStorage {
    fun read(key: String): String?
    fun write(key: String, value: String)
    fun remove(key: String)
}

/** Gerçek uygulama: `SharedPreferences`. Veri uygulama silinene kadar kalır. */
class SharedPreferencesStorage(context: Context) : KeyValueStorage {

    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun read(key: String): String? = preferences.getString(key, null)

    override fun write(key: String, value: String) = preferences.edit().putString(key, value).apply()

    override fun remove(key: String) = preferences.edit().remove(key).apply()

    private companion object {
        const val FILE_NAME = "hava_prefs"
    }
}
