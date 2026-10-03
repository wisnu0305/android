# Firmware ESP32 - Kendali Proyektor via IoT

Firmware ini berjalan di ESP32, menerima perintah HTTP dari aplikasi Android lalu mengirim perintah ke proyektor melalui RS232.

## Persyaratan Hardware

- **ESP32** (dev board atau modul)
- **Modul RS232** (level converter TTL ↔ RS232), misalnya MAX3232
- **Kabel/konektor** male RS232 untuk colok ke port RS232 proyektor

### Rangkaian Singkat

```
ESP32          Modul RS232 (MAX3232)      Proyektor
GPIO 17 (TX) -> RX modul -> TX modul -> RX port RS232 proyektor
GPIO 16 (RX) <- TX modul <- RX modul <- TX port RS232 proyektor
GND           GND           GND
```

Pin bisa diubah di `src/main.cpp`: `RS232_RX_PIN`, `RS232_TX_PIN`.

## Konfigurasi

### 1. WiFi

Edit di `src/main.cpp`:

```cpp
const char* WIFI_SSID     = "NAMA_WIFI_ANDA";
const char* WIFI_PASSWORD = "PASSWORD_WIFI_ANDA";
```

### 2. Perintah RS232 ke Proyektor

Setiap merek proyektor punya **protokol RS232** sendiri. Buka **manual proyektor** (bagian RS232/Serial/Control) dan sesuaikan string perintah di `src/main.cpp`:

```cpp
const char* CMD_PON    = "PON\r\n";     // Power On  - ganti sesuai manual
const char* CMD_POFF   = "POFF\r\n";    // Power Off
const char* CMD_VUP    = "VUP\r\n";     // Volume Up
const char* CMD_VDOWN  = "VDOWN\r\n";   // Volume Down
```

Contoh format yang sering dipakai:

- **ASCII + CR+LF:** `"* 0 PON \r\n"` (PJLink-style)
- **Dengan STX/ETX:** `"\x02PON\x03\r\n"`
- **Tanpa CRLF:** `"PON"`

Baud rate juga bisa beda (9600, 19200, 38400). Ubah `RS232_BAUD` jika perlu.

## Build & Upload

### Menggunakan PlatformIO (VS Code / Cursor)

1. Buka folder `esp32_firmware` di VS Code/Cursor.
2. Pastikan ekstensi **PlatformIO** terpasang.
3. **Build:** `PlatformIO: Build` atau tombol ✓.
4. **Upload:** sambungkan ESP32 via USB, lalu `PlatformIO: Upload` atau tombol →.

### Menggunakan Arduino IDE

1. Di Arduino IDE: **File → Open** → pilih folder `esp32_firmware` (yang berisi `esp32_firmware.ino`).
2. Tambahkan board ESP32: **File → Preferences → Additional Boards Manager URLs** → tambah `https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json` → **Tools → Board → Boards Manager** → cari "esp32" → Install.
3. Pilih board: **Tools → Board → ESP32 Arduino → ESP32 Dev Module**.
4. Pilih port COM, lalu **Upload**.

## Endpoint HTTP (untuk Aplikasi Android)

- **GET /**  
  Info singkat + IP ESP32 + konfigurasi profil saat ini.

- **GET /command?cmd=XXX**  
  `XXX` = `PON` | `POFF` | `VUP` | `VDOWN`  
  Mengirim perintah ke proyektor menggunakan profil yang sudah dikonfigurasi.  
  Respon: `200 OK` dengan body teks (mis. `OK PON`).

- **GET /setCustom?baud=9600&pon=...&poff=...&vup=...&vdown=...**  
  Mengatur profil proyektor (baudrate + command strings).  
  Dipanggil otomatis oleh aplikasi Android saat user menyimpan profil proyektor.  
  Semua parameter di-URL-encode.  
  Respon: `200 OK` dengan body `OK profile set`.

### Contoh Request

```
http://192.168.1.100/command?cmd=PON
http://192.168.1.100/setCustom?baud=9600&pon=PON%0D%0A&poff=POFF%0D%0A&vup=VUP%0D%0A&vdown=VDOWN%0D%0A
```

**Catatan:** Semua profil proyektor sekarang **user-defined** (dibuat dari aplikasi Android). Tidak ada lagi profil bawaan. User wajib mengisi nama proyektor, baudrate, dan semua command strings saat pertama kali setup.

Setelah upload, buka **Serial Monitor** (115200 baud) untuk melihat IP dan log debug.
