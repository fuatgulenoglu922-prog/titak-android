# TiTak - TikTok Bot Uygulaması

![Version](https://img.shields.io/badge/version-2.8-blue)
![Platform](https://img.shields.io/badge/platform-Android-green)
![License](https://img.shields.io/badge/license-MIT-orange)

## 📱 Özellikler

### ✨ Yeni! Versiyon 2.8
- 🎵 **Arka Plan Müziği** - Uygulama içi müzik desteği
- 🔊 **Müzik Kontrolü** - Tek tuşla müziği aç/kapat
- 🎼 **Otomatik Başlatma** - Müzik otomatik olarak başlar

### 🤖 Bot Özellikleri
- ✅ TikTok sandık otomasyonu
- ✅ Ekran yakalama ve görüntü işleme
- ✅ Şablon eşleştirme (Template Matching)
- ✅ Erişilebilirlik servisi ile otomatik tıklama
- ✅ Kayan widget kontrolü

### 🔐 Güvenlik
- Şifre koruması
- Güvenli veri saklama
- İzin yönetimi

### 🔄 Güncelleme Sistemi
- GitHub üzerinden otomatik güncelleme
- Tek tuşla güncelleme kontrolü
- APK otomatik indirme ve kurulum

## 📥 Kurulum

1. **APK İndirme**
   - [Releases](https://github.com/fuatgulenoglu922-prog/titak-android/releases) sayfasından en son sürümü indirin
   - `app-debug.apk` dosyasını telefonunuza yükleyin

2. **İzinler**
   - Ekran üstü izni verin
   - Erişilebilirlik servisini aktif edin
   - APK kurulum izni verin

3. **Müzik Ekleme** (Geliştirici için)
   - Kendi müzik dosyanızı `app/src/main/res/raw/background_music.mp3` olarak ekleyin
   - Desteklenen formatlar: MP3, OGG, WAV

## 🎵 Müzik Sistemi Kullanımı

1. Uygulama açıldığında müzik otomatik başlar
2. "🔊 Müzik: Açık" butonuna basarak müziği kapatabilirsiniz
3. Tekrar basarak açabilirsiniz
4. Müzik arka planda döngü halinde çalar

## 🚀 Kullanım

1. Uygulamayı açın
2. Gerekli izinleri verin
3. Şablon fotoğraflarını ayarlayın
4. "Botu Başlat" butonuna basın
5. TikTok'u açın ve sandıkları bekleyin

## 🔧 Geliştirme

### Gereksinimler
- Android Studio
- JDK 17
- Android SDK 34
- Gradle 8.x

### Proje Yapısı
```
titak-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/efe/titak/
│   │   │   ├── MainActivity.java
│   │   │   ├── MusicManager.java
│   │   │   ├── UpdateManager.java
│   │   │   ├── BotEngine.java
│   │   │   └── ...
│   │   └── res/
│   │       ├── raw/
│   │       │   └── background_music.mp3
│   │       ├── layout/
│   │       └── ...
│   └── build.gradle
├── version.txt
├── CHANGELOG.md
└── README.md
```

### Build
```bash
./gradlew assembleDebug
```

APK dosyası: `app/build/outputs/apk/debug/app-debug.apk`

## 📝 Güncelleme Notları

### v2.8 (16 Mart 2026)
- Müzik sistemi eklendi
- MusicManager sınıfı ile müzik yönetimi
- Müzik kontrol butonu eklendi
- Otomatik müzik başlatma özelliği

[Tüm değişiklikler için CHANGELOG.md dosyasına bakın](CHANGELOG.md)

## 🤝 Katkıda Bulunma

1. Fork yapın
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Commit yapın (`git commit -m 'Add amazing feature'`)
4. Push yapın (`git push origin feature/amazing-feature`)
5. Pull Request açın

## 📄 Lisans

Bu proje MIT lisansı altında lisanslanmıştır.

## 👨‍💻 Geliştirici

**Fuat Gülenoğlu**
- GitHub: [@fuatgulenoglu922-prog](https://github.com/fuatgulenoglu922-prog)

## ⚠️ Uyarı

Bu uygulama eğitim amaçlıdır. TikTok'un kullanım şartlarına uygun kullanın.

## 📞 Destek

Sorun bildirmek veya öneride bulunmak için:
- [Issues](https://github.com/fuatgulenoglu922-prog/titak-android/issues) sayfasını kullanın
- Uygulama içi "Geri Bildirim" butonunu kullanın

---

⭐ Projeyi beğendiyseniz yıldız vermeyi unutmayın!
