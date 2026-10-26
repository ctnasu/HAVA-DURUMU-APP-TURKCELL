package com.kampplus.hava.model

/**
 * Özel isimlere ek getirme: "Sivas" → "Sivas'ta", "Ayşe" → "Ayşe'nin".
 *
 * Türkçe'de ekin hangi harflerle yazılacağı iki kurala bağlıdır:
 * - **Ünlü uyumu:** ekin ünlüsü, kelimenin **son ünlüsüne** uyar.
 * - **Sert ünsüz:** kelime sert bir ünsüzle (f, s, t, k, ç, ş, h, p) bitiyorsa "d" yerine "t" gelir.
 */
object TurkishSuffix {

    private val vowels = "aeıioöuüâîû"
    private val voiceless = "fstkçşhp"

    /** "Sivas" → "Sivas'ta", "İzmir" → "İzmir'de", "Adana" → "Adana'da". */
    fun locative(word: String): String {
        val consonant = if (word.lastOrNull()?.lowercaseChar() in voiceless.toList()) 't' else 'd'
        val vowel = if (lastVowel(word).isBackVowel()) 'a' else 'e'
        return "$word'$consonant$vowel"
    }

    /** "Annen" → "Annen'in", "Ayşe" → "Ayşe'nin", "Murat" → "Murat'ın", "Özgür" → "Özgür'ün". */
    fun genitive(word: String): String {
        val vowel = when (lastVowel(word)) {
            'a', 'ı', 'â' -> 'ı'
            'e', 'i', 'î' -> 'i'
            'o', 'u', 'û' -> 'u'
            else -> 'ü'
        }
        // Kelime ünlüyle bitiyorsa araya "n" girer: Ayşe'nin; ünsüzle bitiyorsa girmez: Murat'ın.
        val buffer = if (word.lastOrNull()?.lowercaseChar() in vowels.toList()) "n" else ""
        return "$word'$buffer${vowel}n"
    }

    /** Kelimedeki son ünlü; hiç ünlü yoksa "e" (ince) varsayılır. */
    private fun lastVowel(word: String): Char = word.lastOrNull { it.lowercaseChar() in vowels }?.lowercaseChar() ?: 'e'

    private fun Char.isBackVowel() = this in "aıouâû"
}
