package com.kampplus.hava.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** Ünlü uyumu ve sert ünsüz kuralları: cümlelerin doğru Türkçe çıkması için. */
class TurkishSuffixTest {

    @Test
    fun `bulunma eki ince ve kalin unluye uyar`() {
        assertEquals("Sivas'ta", TurkishSuffix.locative("Sivas"))
        assertEquals("Adana'da", TurkishSuffix.locative("Adana"))
        assertEquals("İzmir'de", TurkishSuffix.locative("İzmir"))
        assertEquals("Rize'de", TurkishSuffix.locative("Rize"))
        assertEquals("Ankara'da", TurkishSuffix.locative("Ankara"))
        assertEquals("Muğla'da", TurkishSuffix.locative("Muğla"))
        assertEquals("Ağrı'da", TurkishSuffix.locative("Ağrı"))
        assertEquals("Konya'da", TurkishSuffix.locative("Konya"))
        assertEquals("Bolu'da", TurkishSuffix.locative("Bolu"))
        assertEquals("Hakkâri'de", TurkishSuffix.locative("Hakkâri"))
    }

    @Test
    fun `sert unsuzle biten kelimede d yerine t gelir`() {
        assertEquals("Kars'ta", TurkishSuffix.locative("Kars"))
        assertEquals("Kahramanmaraş'ta", TurkishSuffix.locative("Kahramanmaraş"))
        assertEquals("Erzurum'da", TurkishSuffix.locative("Erzurum"))
        assertEquals("Diyarbakır'da", TurkishSuffix.locative("Diyarbakır"))
        assertEquals("Hatay'da", TurkishSuffix.locative("Hatay"))
        assertEquals("Edirne'de", TurkishSuffix.locative("Edirne"))
        assertEquals("Şanlıurfa'da", TurkishSuffix.locative("Şanlıurfa"))
        assertEquals("Muş'ta", TurkishSuffix.locative("Muş"))
    }

    @Test
    fun `iyelik eki dort yonlu unlu uyumuna uyar`() {
        assertEquals("Murat'ın", TurkishSuffix.genitive("Murat"))
        assertEquals("Mehmet'in", TurkishSuffix.genitive("Mehmet"))
        assertEquals("Zeynep'in", TurkishSuffix.genitive("Zeynep"))
        assertEquals("Burak'ın", TurkishSuffix.genitive("Burak"))
        assertEquals("Özgür'ün", TurkishSuffix.genitive("Özgür"))
        assertEquals("Okan'ın", TurkishSuffix.genitive("Okan"))
        assertEquals("Mert'in", TurkishSuffix.genitive("Mert"))
        assertEquals("Umut'un", TurkishSuffix.genitive("Umut"))
    }

    @Test
    fun `unluyle biten kelimede araya n girer`() {
        assertEquals("Ayşe'nin", TurkishSuffix.genitive("Ayşe"))
        assertEquals("Ali'nin", TurkishSuffix.genitive("Ali"))
        assertEquals("Hülya'nın", TurkishSuffix.genitive("Hülya"))
        assertEquals("Ebru'nun", TurkishSuffix.genitive("Ebru"))
        assertEquals("Selma'nın", TurkishSuffix.genitive("Selma"))
    }
}
