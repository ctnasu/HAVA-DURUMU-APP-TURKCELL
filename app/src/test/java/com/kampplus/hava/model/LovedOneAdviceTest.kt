package com.kampplus.hava.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** "Sevdiklerim" kartındaki cümleler: doğru uyarı, doğru Türkçe. */
class LovedOneAdviceTest {

    private val mother = LovedOne(1, "Annem", 58)

    private fun day(code: Int = 0, min: Double = 8.0, max: Double = 20.0, rain: Int? = 0) = DaySummary(code, min, max, rain)

    private fun message(person: LovedOne = mother, city: String = "Sivas", tomorrow: DaySummary) =
        checkNotNull(adviseLovedOne(person, city, tomorrow)).message

    @Test
    fun `kar icin tam olarak bekledigimiz cumle cikar`() {
        assertEquals(
            "Annen'in olduğu Sivas'ta yarın kar var, kalın giyinmesini söyle.",
            message(tomorrow = day(code = 73, min = -4.0, max = 1.0))
        )
    }

    @Test
    fun `yagmur sirasi ve cumlesi`() {
        assertEquals(
            "Annen'in olduğu Sivas'ta yarın yağmur bekleniyor, şemsiye almasını hatırlat.",
            message(tomorrow = day(code = 61))
        )
        // Kod "açık" olsa bile yağış ihtimali yüksekse yağmur uyarısı çıkar.
        assertEquals(Severity.ALERT, adviseLovedOne(mother, "Sivas", day(code = 3, rain = 75))!!.severity)
    }

    @Test
    fun `firtina digerlerinden once gelir`() {
        val advice = checkNotNull(adviseLovedOne(mother, "Sivas", day(code = 95, min = -2.0, max = 3.0, rain = 90)))

        assertEquals("Annen'in olduğu Sivas'ta yarın gök gürültülü fırtına var, mümkünse evden çıkmasın.", advice.message)
    }

    @Test
    fun `kar hem soguk hem karli gunde soguk uyarisini ezer`() {
        val advice = checkNotNull(adviseLovedOne(mother, "Sivas", day(code = 71, min = -9.0, max = -2.0)))

        assertEquals(true, "kar var" in advice.message)
    }

    @Test
    fun `don cok soguk ve sicak uyarilari`() {
        assertEquals(
            "Annen'in olduğu Sivas'ta yarın gece don var, akşam sıkı giyinmesini söyle.",
            message(tomorrow = day(min = -1.0, max = 12.0))
        )
        assertEquals(
            "Annen'in olduğu Sivas'ta yarın hava çok soğuk (en çok 5°), kalın giyinmesini söyle.",
            message(tomorrow = day(min = 1.0, max = 5.2))
        )
        assertEquals(
            "Annen'in olduğu Sivas'ta yarın hava çok sıcak (38°), bol su içmesini ve öğle güneşinden kaçmasını söyle.",
            message(tomorrow = day(min = 22.0, max = 37.8))
        )
    }

    @Test
    fun `guzel havada iyi gunler dilenir`() {
        val advice = checkNotNull(adviseLovedOne(mother, "Sivas", day(code = 1, max = 22.4)))

        assertEquals("Annen'in olduğu Sivas'ta yarın hava güzel, en çok 22°. Ona iyi günler dile.", advice.message)
        assertEquals(Severity.NICE, advice.severity)
    }

    @Test
    fun `akrabalik adi sen bicimine cevrilir`() {
        assertEquals("Annen", LovedOne.toSecondPerson("Annem"))
        assertEquals("Baban", LovedOne.toSecondPerson("Babam"))
        assertEquals("Kardeşin", LovedOne.toSecondPerson("Kardeşim"))
        assertEquals("Eşin", LovedOne.toSecondPerson("eşim"))
        assertEquals("Kardeşin'in", LovedOne(2, "Kardeşim", 35).genitiveForYou)
    }

    @Test
    fun `kisi adlari bozulmaz`() {
        // "Selim"in son harfini "n" yapmak adı "Selin"e çevirirdi; yalnızca bilinen akrabalık adları dönüşür.
        assertEquals("Selim", LovedOne.toSecondPerson("Selim"))
        assertEquals(
            "Ayşe'nin olduğu İzmir'de yarın hava güzel, en çok 20°. Ona iyi günler dile.",
            message(LovedOne(3, "Ayşe", 35), "İzmir", day())
        )
    }

    @Test
    fun `yarinin verisi yoksa kart cumlesi yoktur`() {
        assertNull(adviseLovedOne(mother, "Sivas", null))
    }
}
