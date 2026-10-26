# Hava
Türkiye'nin 81 ili için **canlı gökyüzü** üzerinde çalışan, kullanıcıya göre öğrenen bir hava durumu uygulaması.

Arka plan şehrin gerçek saatine ve havasına göre çizilir: şafakta turuncu, gecede yıldızlı, yağmurda damlalı. Sıcaklığın yanında "Ne zaman koşmalıyım?" ve "Annem'e ne söylemeliyim?" gibi soruları da cevaplar.

**Teknoloji:** Kotlin · Jetpack Compose (Material 3) · ViewModel + StateFlow · Coroutines · Retrofit + Gson · Navigation Compose
<img width="1080" height="2424" alt="Screenshot_20261026_094654" src="https://github.com/user-attachments/assets/92b5ea74-2e94-4ddf-a936-2fd43c51e8a4" /> <img width="1080" height="2424" alt="Screenshot_20261026_094708" src="https://github.com/user-attachments/assets/3ba36a4e-8bd5-4577-bfd2-f194f64a8606" /> <img width="1080" height="2424" alt="Screenshot_20261026_094731" src="https://github.com/user-attachments/assets/516c739e-9b49-42b0-88d6-8055272dcaca" /> <img width="1080" height="2424" alt="Screenshot_20261026_094743" src="https://github.com/user-attachments/assets/9ed774f8-85c0-4edc-8b94-8dd0cda15d47" /> <img width="1080" height="2424" alt="Screenshot_20261026_094753" src="https://github.com/user-attachments/assets/b0ae8822-58d5-469b-9e8c-c0bfd576ecce" />


## Özellikler

### Canlı gökyüzü
- Gökyüzü; gün doğumu/batımı saatlerine göre **şafak, gündüz, alacakaranlık ve gece** paletlerine geçer.
- Hava durumuna göre yağmur, fırtına (şimşekle), kar, sis, bulut, yıldız, güneş ve ay çizilir. Hepsi `Canvas` ile, harici görsel kullanılmadan çizilir.
- Katmanlar birbirine yumuşakça geçer: yağmur birden belirmez, yavaşça başlar; güneş gün boyunca gökyüzünde yürür.

### Zaman makinesi
Saatlik grafikte bir saate dokunun ya da basılı tutup kaydırın: **gökyüzü, büyük sıcaklık ve durum yazısı o saate gider.** Her saat değişiminde hafif bir titreşim verilir.

### Tahmin ekranı
- Etkileşimli saatlik grafik: sıcaklığa göre renklenen Bezier eğrisi ve yağış çubukları.
- 7 günlük liste: haftanın en düşük/en yüksek değerine göre ölçeklenmiş **sıcaklık aralığı çubukları**.
- Güneşin gün içindeki yolunu gösteren güneş yayı.
- Açılışta sayarak yükselen sıcaklık, sırayla gelen kartlar.

### 81 il, canlı kartlar
- Her il kartı, o ilin **şu anki** gökyüzüne açılan küçük bir penceredir: yerel saat, sıcaklık, durum.
- Arama internetsizdir ve her tuşta anında çalışır. Türkçe karakterler ve büyük/küçük harf önemsizdir: `Izmir`, `sanliurfa`, `mugla` bulunur. Rakam yazarsanız plaka aranır: `34` İstanbul'u getirir.
- 81 ilin havası, 30'arlık üç istekle alınır.

### En iyi zaman: sizi öğrenen aktivite penceresi
Koşu, bisiklet, yürüyüş, çamaşır asma ve araba yıkama için önümüzdeki 48 saatin en uygun penceresi bulunur: **"Yarın 07:00–09:00 · Çok uygun"**.

Her aktivite için küçük bir **lojistik regresyon** modeli çalışır ve kullanıcıya göre öğrenir:

```
P(uygun) = sigmoid(w · x),   x = [1, t, t², rüzgâr, yağmur, nem, gece]
```

- Model, "koşu için ideal 12°, yağmur kötü" gibi bir **başlangıç bilgisiyle** başlar.
- "👍 İyiydi / 👎 Kötüydü" cevaplarıyla her seferinde bir gradyan adımı atılır (çevrimiçi öğrenme).
- Ağırlıklar başlangıç değerlerine hafifçe çekilir (L2 düzenlileştirme): tek bir tuhaf cevap modeli bozmaz, tutarlı cevaplar kalıcı olarak kaydırır.
- `t²` terimi sayesinde model bir **ideal sıcaklık** öğrenebilir; ekran bunu "başlangıç → şimdi" olarak gösterir.
- Çamaşır ve araba yıkama için yağmur, ilerideki saatler de hesaba katılarak aranır (yıkadıktan sonra yağan yağmur işi boşa çıkarır).
- Öğrenilenler telefonda saklanır; **"Sıfırla"** ile başlangıca dönülür.

### Sevdiklerim
"Annem – Sivas", "Kardeşim – İzmir" gibi kişiler eklenir. Ana ekranda, o ilin canlı gökyüzüyle birlikte yarına dair bir cümle çıkar:

> **Annen'in olduğu Sivas'ta yarın kar var, kalın giyinmesini söyle.**

Kar, yağmur, fırtına, don, aşırı soğuk, aşırı sıcak ve sis için ayrı cümleler vardır. Cümleler **Türkçe ek uyumuna** uygun kurulur: ünlü uyumu ve sert ünsüz kuralları kodlanmıştır (`Sivas'ta`, `İzmir'de`, `Ayşe'nin`, `Murat'ın`, `Özgür'ün`). "Annem" cümlede "Annen" olur; "Selim" gibi kişi adları bozulmaz.

## Mimari

Üç katman: **UI görünümü çizer, ViewModel durumu yönetir, Repository veriye erişir.**

```
UI (Composable)  →  ViewModel (StateFlow)  →  Repository (suspend)  →  Retrofit → Open-Meteo
```

```
com.kampplus.hava
├── MainActivity.kt                  Uygulamanın kapısı
├── model/                           Saf Kotlin: Android'e bağımlı değil, birim testle doğrulanır
│   ├── City.kt                      İl (plaka, ad, koordinat)
│   ├── Forecast.kt                  Tahmin, saatlik/günlük satırlar, il özeti
│   ├── WeatherCondition.kt          WMO kodu → emoji + Türkçe etiket
│   ├── Activity.kt                  Aktiviteler ve başlangıç bilgileri
│   ├── ActivityModel.kt             Öğrenen model (lojistik regresyon)
│   ├── ActivityAdvisor.kt           Saatleri puanlar, en iyi pencereyi bulur
│   ├── LovedOne.kt, LovedOneAdvice.kt   Sevdiklerim ve yarına dair cümleler
│   └── TurkishSuffix.kt             Türkçe ek uyumu
├── data/
│   ├── CityCatalog.kt, CitySearch.kt    81 il ve aramanın kuralları
│   ├── ForecastApi.kt, ForecastResponse.kt   Open-Meteo (sütun → satır çevrimi)
│   ├── WeatherRepository.kt             Tahmin ve toplu il özeti
│   ├── KeyValueStorage.kt               Kalıcı depo (arayüz + SharedPreferences)
│   └── ActivityModelRepository.kt, LovedOnesRepository.kt
└── ui/
    ├── city/                        İl listesi, kartlar, Sevdiklerim
    ├── forecast/                    Tahmin ekranı, grafik, aktivite penceresi
    │   └── sky/                     Canlı gökyüzü (durum + çizim)
    └── component/                   Ortak bileşenler
```

**Performans notları**
- Gökyüzü animasyonunda zaman ve katman görünürlükleri yalnızca **çizim aşamasında** okunur; her karede yeniden oluşturma (recomposition) olmaz, yalnızca `Canvas` yeniden çizilir.
- Parçacıklar bir kez üretilir, konumları zamandan hesaplanır: çizim sırasında nesne oluşturulmaz.
- Listede yalnızca görünen kartlar (ve gökyüzleri) çizilir (`LazyColumn`); kartlarda parçacık yoğunluğu düşürülür.

## Veri kaynağı

[Open-Meteo](https://open-meteo.com/): API anahtarı gerekmez. Ücretsiz kullanım ticari olmayan projeler içindir.

| Amaç | Çağrı |
|---|---|
| Tahmin | `https://api.open-meteo.com/v1/forecast?latitude=..&longitude=..&current=..&hourly=..&daily=..&timezone=auto` |
| Toplu il özeti | Aynı adres, virgülle ayrılmış birden çok koordinat (tek istekte 30 il) |

Zamanlar `timezone=auto` ile **şehrin yerel saatinde** gelir; uygulama telefonun saatini değil servisin bildirdiği "şimdi"yi kullanır (Tokyo'ya bakarken İstanbul saati yanlış olurdu).

## Çalıştırma

Android Studio'da açıp çalıştırın ya da komut satırından:

```bash
./gradlew assembleDebug        # debug APK
./gradlew testDebugUnitTest    # birim testler
./gradlew spotlessApply        # ktlint formatı (denetim için spotlessCheck)
./gradlew assembleRelease      # release APK (R8 açık)
```

Android SDK yolunu `local.properties` içinde `sdk.dir=...` ile ya da `ANDROID_HOME` ortam değişkeniyle verin. Minimum Android sürümü 8.0 (API 26).

## Testler

Birim testler internete çıkmaz; sahte repository'ler ve örnek JSON dosyaları kullanır.

| Test | Neyi korur |
|---|---|
| `ActivityModelTest` | Modelin gerçekten öğrendiği; tek bir tuhaf cevabın modeli bozmadığı; kaydetme/okuma |
| `ActivityAdvisorTest` | En iyi pencerenin seçimi; yağmur ve gece cezası; öğrenilen tercihin pencereyi değiştirmesi |
| `TurkishSuffixTest` | Ünlü uyumu, sert ünsüz ve araya giren "n" kuralları |
| `LovedOneAdviceTest` | Hangi havada hangi cümlenin çıktığı; akrabalık adlarının "sen" biçimine çevrilmesi |
| `CitySearchTest`, `CityCatalogTest` | Türkçe karakterli arama, plaka araması; 81 ilin veri doğruluğu (plaka ve koordinat sınırları) |
| `RepositoriesTest` | Öğrenilenlerin ve sevdiklerin uygulama yeniden açılınca korunması |
| `ForecastApiTest`, `SnapshotParsingTest` | Sütun → satır çevrimi (index kayması hata vermez, yanlış veri gösterir), toplu yanıtın çözümlenmesi |
| `SkyStateTest` | Günün hangi saatinde hangi gökyüzü paletinin seçildiği |
| `CityViewModelTest`, `ForecastViewModelTest`, `WeatherConditionTest` | Ekran durumları ve WMO kodu eşlemesi |

## Release imzası

`keystore.properties.example` dosyasını `keystore.properties` olarak kopyalayıp doldurun (repoya girmez). Dosya yoksa release APK debug anahtarıyla imzalanır.
