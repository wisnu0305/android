# Kontrol Proyektor Lewat Suara (Android -> ESP32 -> Arduino Uno)

## Gambaran Umum

Sistem untuk mengontrol proyektor dengan perintah suara.

Alur kerja:
1. Pengguna berbicara ke aplikasi Android.
2. Aplikasi mengubah suara menjadi teks (speech-to-text).
3. Teks dicocokkan dengan daftar perintah yang sudah ditanamkan (hardcoded) di aplikasi.
4. Jika cocok, aplikasi mengirim kode perintah ke ESP32 lewat WiFi.
5. ESP32 meneruskan perintah ke Arduino Uno lewat serial (UART).
6. Arduino Uno mengeksekusi perintah untuk mengontrol proyektor.

```
[Suara] -> [Android App] --WiFi--> [ESP32] --Serial--> [Arduino Uno] --> [Proyektor]
           (STT + pencocokan)      (jembatan)           (eksekusi)
```

## Struktur Folder

```
/
├── CLAUDE.md
├── .gitignore
├── android/              # Project Android Studio (aplikasi suara)
├── esp32_firmware/       # Firmware ESP32 (penerima WiFi + jembatan serial)
└── arduino_uno_benq/     # Sketch Arduino Uno (kontrol proyektor BenQ)
```

Catatan: file `.ino` harus berada di dalam folder dengan nama yang sama
(contoh: `arduino_uno_benq/arduino_uno_benq.ino`). Jangan buat salinan `.ino` di root.

## Komponen

### 1. Android (`android/`)
- Menangkap suara dan mengubahnya menjadi teks.
- Mencocokkan teks dengan daftar perintah yang sudah ditanamkan di kode.
- Mengirim kode perintah ke ESP32 lewat WiFi.
- Menampilkan status: mendengarkan, perintah dikenali/tidak, terkirim, atau gagal terhubung.
- TODO: isi bahasa, library speech-to-text yang dipakai, minSdk, dan library HTTP/socket.

### 2. ESP32 (`esp32_firmware/`)
- Terhubung ke WiFi dan menerima perintah dari aplikasi Android.
- Memvalidasi perintah, lalu meneruskannya ke Arduino Uno lewat serial.
- Mengirim respons (OK/ERROR) kembali ke Android.
- Board: ESP32 Dev Module (Arduino IDE, paket Espressif).
- TODO: isi mode WiFi (STA atau AP), alamat IP, port, dan pin UART.

### 3. Arduino Uno (`arduino_uno_benq/`)
- Menerima perintah dari ESP32 lewat serial.
- Menerjemahkan perintah menjadi sinyal kontrol proyektor.
- TODO: isi metode kontrol proyektor yang dipakai (RS-232, IR, atau relay), model BenQ, dan pin.

## Protokol Komunikasi

### Android -> ESP32 (WiFi)
- TODO: isi jenis protokol (HTTP GET/POST, WebSocket, atau TCP) dan endpoint.
- Contoh rancangan HTTP: `GET http://<ip-esp32>/cmd?c=POWER_ON`
- Respons: `OK` atau `ERROR:<alasan>`

### ESP32 -> Arduino Uno (Serial)
- Baud rate: TODO (contoh 9600)
- Format: teks satu baris diakhiri newline, contoh `POWER_ON\n`
- Arduino membalas `OK\n` atau `ERR\n`.

### Daftar Perintah
Daftar ini harus **identik** di ketiga bagian (Android, ESP32, Uno). Jika menambah atau mengubah
perintah, perbarui ketiganya sekaligus.

| Kode perintah | Contoh ucapan | Fungsi |
|---|---|---|
| POWER_ON | "nyalakan proyektor" | Menyalakan proyektor |
| POWER_OFF | "matikan proyektor" | Mematikan proyektor |
| TODO | TODO | TODO (volume, input source, freeze, blank, dll.) |

## Wiring dan Pin

Sesuai pin di kode: ESP32 `HardwareSerial(2)` RX=GPIO16, TX=GPIO17 (9600 baud);
Uno `SoftwareSerial EspSerial(2, 3)` dan `SoftwareSerial BenQ_Serial(10, 11)` (9600 baud).
Jika pin diubah di sini, ubah juga di kode (dan sebaliknya).

```
[HP Android] ~~WiFi~~ [ESP32] --- [Arduino Uno] --- [Modul RS232 MAX3232] ==DB9== [Proyektor BenQ]
                         \__ breadboard __/
                         (pembagi tegangan + GND bersama)
```

### Kabel langsung

| Dari | Ke | Keterangan |
|---|---|---|
| ESP32 D17 (GPIO17, TX2) | Uno pin 2 (RX EspSerial) | ESP32 -> Uno. 3,3V aman untuk Uno, tanpa resistor |
| Uno pin 11 (TX BenQ_Serial) | Modul RS232 TXD | Uno -> proyektor. Sudah diuji: label di modul yang dipakai terbalik |
| Uno pin 10 (RX BenQ_Serial) | Modul RS232 RXD | Proyektor -> Uno |
| Uno 5V | Modul RS232 VCC | Daya modul RS232 |
| Modul RS232 DB9 | Port RS232 proyektor | Langsung atau kabel DB9; mungkin perlu null-modem |

### Breadboard (pembagi tegangan + GND)

Resistor: 3 x 1 kΩ (coklat-hitam-hitam-coklat-coklat). Jalur +/- di pinggir breadboard tidak dipakai.

| Dari | Ke | Keterangan |
|---|---|---|
| Uno pin 3 (TX EspSerial) | Breadboard A10 | Sinyal 5V dari Uno |
| Resistor 1 (1 kΩ) | B10 <-> B15 | |
| Breadboard A15 | ESP32 D16 (GPIO16, RX2) | Titik ±3,3V (5V x 2/3) |
| Resistor 2 (1 kΩ) | C15 <-> C20 | |
| Resistor 3 (1 kΩ) | D20 <-> D25 | |
| Uno GND | Breadboard A25 | GND bersama |
| ESP32 GND | Breadboard B25 | GND bersama |
| Modul RS232 GND | Breadboard C25 | GND bersama |

### Daya

- ESP32 dan Uno masing-masing lewat USB. Modul RS232 dari Uno 5V.
- Jangan sambungkan Uno 5V ke pin 3V3/VIN ESP32.

**Catatan tegangan:** Uno bekerja di 5V, ESP32 di 3,3V. Jalur Uno TX (pin 3) -> ESP32 RX (GPIO16)
wajib lewat pembagi tegangan di atas (atau level shifter) agar pin ESP32 tidak rusak. Dengan hanya
2 resistor (1 kΩ + 1 kΩ) hasilnya 2,5V: aman, tapi di batas bawah logika HIGH ESP32.

**Catatan RS232:** label TXD/RXD berbeda antar modul. Jika proyektor tidak bereaksi, tukar kabel
Uno pin 10/11 di modul atau pakai adaptor null-modem. Set baud RS232 proyektor ke 9600 dan aktifkan
RS232 saat standby di menu proyektor.

**Catatan upload ESP32:** jika upload gagal ("Wrong boot mode"), lepas kabel D16/D17, tahan BOOT
saat `Connecting...`, tekan EN sekali, lepas BOOT setelah upload mulai.

## Build dan Upload

- **Arduino Uno:** Arduino IDE, board "Arduino Uno", buka `arduino_uno_benq/arduino_uno_benq.ino`.
- **ESP32:** Arduino IDE, board "ESP32 Dev Module", buka sketch di `esp32_firmware/`.
  Jika upload gagal, tahan tombol BOOT saat proses upload dimulai.
- **Android:** buka folder `android/` di Android Studio, lalu Sync Gradle dan Run.
- Claude Code tidak dapat mengupload ke board atau menjalankan perangkat keras. Pengujian di
  perangkat dilakukan manual oleh pengguna.

## Library dan Versi

TODO: isi versi yang dipakai.

- ESP32 board package (Espressif): TODO
- Library ESP32: `WiFi`, `WebServer` (bawaan), TODO
- Library Uno: TODO
- Android: TODO

## Aturan Kerja untuk Claude Code

- Jaga konsistensi **daftar perintah dan format protokol** di ketiga bagian. Jika mengubah salah
  satu, periksa dan sesuaikan dua lainnya.
- Jangan mengubah pin, baud rate, atau format pesan tanpa memberi tahu pengguna dan memperbarui
  dokumen ini.
- Jangan menulis password WiFi, SSID, atau kredensial langsung di kode. Gunakan `secrets.h`
  (ESP32) atau file konfigurasi lokal (Android) yang sudah ada di `.gitignore`.
- Buat commit kecil dan terpisah per bagian (`android`, `esp32`, `uno`) dengan pesan yang jelas.
  Bekerja di branch terpisah, bukan langsung di `main`.
- Hindari `delay()` panjang di firmware agar penerimaan perintah tidak terblokir. Utamakan
  `millis()` untuk pewaktuan.
- Tangani kasus gagal: WiFi putus, perintah tidak dikenal, timeout dari Uno. Kirim pesan error
  yang jelas ke Android.
- Jelaskan perubahan yang dilakukan dan sebutkan bagian mana yang perlu di-upload ulang ke
  board atau di-build ulang.

## Masalah Umum (diisi seiring berjalannya project)

- Proyektor tidak bereaksi padahal Serial Monitor Uno mencetak `[SENT]`: label TXD/RXD modul RS232
  terbalik. Solusi (sudah diuji): Uno pin 11 -> TXD modul, Uno pin 10 -> RXD modul.
- Upload ESP32 gagal "Access is denied": COM port sedang dipakai Serial Monitor. Tutup dulu.
- Upload ESP32 gagal "Wrong boot mode (0x13)": tahan BOOT saat `Connecting...`, tekan EN sekali,
  lepas BOOT setelah `Writing at ...` muncul.
