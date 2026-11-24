# WA Number Runner V2

Aplikasi Android native Kotlin untuk mengelola antrean nomor secara **Guided Queue**.

## Fitur MVP
- Tambah nomor secara manual (satu nomor per baris).
- Import daftar nomor TXT.
- Pilih WhatsApp atau WhatsApp Business.
- RUN menyalin nomor aktif ke clipboard lalu membuka aplikasi pilihan.
- Pengguna menyelesaikan login dan OTP sendiri.
- Konfirmasi status Success / Failed / Limited.
- Status Limited dicatat dan antrean tidak otomatis dialihkan.
- Data antrean disimpan lokal dalam JSON di SharedPreferences.
- Tombol Pause dan Stop.

## Build APK
Build melalui GitHub Actions menggunakan workflow `.github/workflows/android-build.yml`.

## Catatan
Aplikasi ini tidak membaca SMS/OTP, tidak mengakses akun SMSCode.gg, tidak mengotomatisasi permintaan OTP, dan tidak mencoba menghindari pembatasan WhatsApp.
