// ===== ARDUINO UNO: Terima perintah dari ESP32, kirim ke BenQ via RS232 =====
#include <SoftwareSerial.h>

// SoftwareSerial untuk komunikasi ke BenQ via RS232
// Pin 10 = RX (dari modul RS232 TX)
// Pin 11 = TX (ke modul RS232 RX)
SoftwareSerial BenQ_Serial(10, 11); // RX, TX

// SoftwareSerial untuk link ke ESP32
// ESP32 TX2 (GPIO17) -> Uno pin 2 (RX)
// ESP32 RX2 (GPIO16) <- Uno pin 3 (TX)
SoftwareSerial EspSerial(2, 3); // RX, TX

// Konstanta untuk komunikasi ke BenQ
const long BAUD_RATE = 9600; // Baud Rate standar BenQ
const char CR = '\r';        // Carriage Return (0x0D)

// Perintah BenQ (tanpa CR, nanti dibungkus CR di fungsi kirim)
const char* CMD_POWER_ON    = "*pow=on#";
const char* CMD_POWER_OFF   = "*pow=off#";
const char* CMD_GET_STATUS  = "*pow=?#";
const char* CMD_INPUT_HDMI1 = "*sour=hdmi#";
const char* CMD_BLANK_ON    = "*blank=on#";

void setup() {
  // Serial utama ke PC untuk debug
  Serial.begin(9600);
  Serial.println("--- BenQ Projector Control (Arduino UNO + ESP32 link) ---");
  Serial.println("Dari ESP32 diharapkan kirim: PON, POFF, STATUS, HDMI, BLANK, atau RAW:<command>");

  // Serial untuk komunikasi ke proyektor (SoftwareSerial)
  BenQ_Serial.begin(BAUD_RATE);

  // Serial untuk komunikasi ke ESP32
  EspSerial.begin(9600);
}

// Fungsi untuk membersihkan string dari karakter non-printable
String cleanString(String input) {
  String cleaned = "";
  for (int i = 0; i < input.length(); i++) {
    char c = input.charAt(i);
    // Hanya ambil karakter printable ASCII (32-126) atau newline/carriage return
    if ((c >= 32 && c <= 126) || c == '\n' || c == '\r') {
      cleaned += c;
    }
  }
  return cleaned;
}

void loop() {
  // 1) Perintah dari ESP32
  if (EspSerial.available()) {
    // Baca dengan timeout untuk menghindari blocking
    String in = "";
    unsigned long startTime = millis();
    
    while (millis() - startTime < 100) { // timeout 100ms
      if (EspSerial.available()) {
        char c = EspSerial.read();
        if (c == '\n') {
          break; // Baris selesai
        } else if (c != '\r' && c >= 32 && c <= 126) {
          // Hanya ambil karakter printable ASCII
          in += c;
        }
      }
      delay(1);
    }
    
    in.trim();

    if (in.length() > 0) {
      Serial.print("\n[FROM ESP32] Raw: '");
      Serial.print(in);
      Serial.print("' (length=");
      Serial.print(in.length());
      Serial.println(")");
      handleCommandFromEsp(in);
    }
  }

  // 2) (Opsional) Perintah manual dari PC via Serial Monitor
  if (Serial.available()) {
    String in = Serial.readStringUntil('\n');
    in.trim();

    if (in == "PROYEKTOR_ON") {
      sendCommand(CMD_POWER_ON);
    } else if (in == "PROYEKTOR_OFF") {
      sendCommand(CMD_POWER_OFF);
    } else if (in == "STATUS") {
      sendCommand(CMD_GET_STATUS);
    } else if (in == "HDMI") {
      sendCommand(CMD_INPUT_HDMI1);
    } else if (in == "b") {
      sendCommand(CMD_BLANK_ON);
    } else if (in.startsWith("RAW:")) {
      // Format: RAW:*sour=hdmi#
      String rawCmd = in.substring(4); // ambil setelah "RAW:"
      sendRawCommand(rawCmd);
    }
  }

  // 3) Cek balasan dari proyektor dan print ke Serial Monitor PC
  if (BenQ_Serial.available()) {
    readAndDebugReply();
  }
}

// Terjemahkan perintah teks dari ESP32 ke perintah BenQ
void handleCommandFromEsp(const String& in) {
  Serial.print("[HANDLE] Processing command: '");
  Serial.print(in);
  Serial.println("'");
  
  if (in == "PON") {
    Serial.println("[HANDLE] → Standard command: PON");
    sendCommand(CMD_POWER_ON);
    EspSerial.println("OK PON");
  } else if (in == "POFF") {
    Serial.println("[HANDLE] → Standard command: POFF");
    sendCommand(CMD_POWER_OFF);
    EspSerial.println("OK POFF");
  } else if (in == "STATUS") {
    Serial.println("[HANDLE] → Standard command: STATUS");
    sendCommand(CMD_GET_STATUS);
    EspSerial.println("OK STATUS");
  } else if (in == "HDMI") {
    Serial.println("[HANDLE] → Standard command: HDMI");
    sendCommand(CMD_INPUT_HDMI1);
    EspSerial.println("OK HDMI");
  } else if (in == "BLANK") {
    Serial.println("[HANDLE] → Standard command: BLANK");
    sendCommand(CMD_BLANK_ON);
    EspSerial.println("OK BLANK");
  } else if (in.startsWith("RAW:")) {
    // Custom command: RAW:*sour=hdmi#
    String rawCmd = in.substring(4); // ambil setelah "RAW:"
    rawCmd.trim(); // bersihkan spasi
    
    Serial.print("[HANDLE] → CUSTOM COMMAND detected: '");
    Serial.print(rawCmd);
    Serial.print("' (length=");
    Serial.print(rawCmd.length());
    Serial.println(")");
    
    // Auto-append '#' untuk command BenQ yang dimulai dengan '*' tapi tidak berakhir dengan '#'
    if (rawCmd.startsWith("*") && !rawCmd.endsWith("#")) {
      Serial.print("[HANDLE] Auto-appending '#' untuk command BenQ: '");
      Serial.print(rawCmd);
      Serial.println("'");
      rawCmd += "#";
      Serial.print("[HANDLE] Command setelah auto-append: '");
      Serial.print(rawCmd);
      Serial.println("'");
    } else if (!rawCmd.endsWith("#") && rawCmd.length() > 0) {
      Serial.print("[HANDLE] WARNING: Command tidak berakhir dengan '#': '");
      Serial.print(rawCmd);
      Serial.println("'");
      Serial.println("[HANDLE] Command tetap akan dikirim tanpa auto-append");
    }
    
    sendRawCommand(rawCmd);
    
    // Kirim balasan ke ESP32
    String reply = "OK RAW: " + rawCmd;
    Serial.print("[TO ESP32] Sending reply: '");
    Serial.print(reply);
    Serial.println("'");
    EspSerial.println(reply);
    delay(10); // Beri waktu untuk serial flush
    Serial.println("[TO ESP32] Reply sent");
  } else {
    Serial.print("[HANDLE] → ERROR: Unknown command '");
    Serial.print(in);
    Serial.println("'");
    EspSerial.println("ERR UNKNOWN CMD");
  }
}

/**
 * Mengirimkan perintah ke proyektor.
 * Format: <CR> + command + <CR>
 */
void sendCommand(const char* command) {
  // Kirim CR pembuka
  BenQ_Serial.print(CR);
  // Kirim perintah
  BenQ_Serial.print(command);
  // Kirim CR penutup
  BenQ_Serial.print(CR);

  Serial.print("\n[SENT] ");
  Serial.print("[CR]");
  Serial.print(command);
  Serial.print("[CR]");
  Serial.println();
}

/**
 * Mengirim custom command string langsung ke proyektor (tanpa mapping).
 * Format: <CR> + rawCommand + <CR>
 */
void sendRawCommand(const String& rawCommand) {
  Serial.print("\n[TO BENQ] Sending CUSTOM command: '");
  Serial.print(rawCommand);
  Serial.print("' (length=");
  Serial.print(rawCommand.length());
  Serial.println(")");
  
  // Debug: print setiap karakter untuk memastikan # tidak hilang
  Serial.print("[TO BENQ] Command bytes: ");
  for (int i = 0; i < rawCommand.length(); i++) {
    char c = rawCommand.charAt(i);
    Serial.print("'");
    Serial.print(c);
    Serial.print("'");
    if (i < rawCommand.length() - 1) Serial.print(" ");
  }
  Serial.println();
  
  BenQ_Serial.print(CR);
  BenQ_Serial.print(rawCommand);
  BenQ_Serial.print(CR);

  Serial.print("[TO BENQ] Sent to RS232: [CR]");
  Serial.print(rawCommand);
  Serial.println("[CR]");
}

/**
 * Membaca dan menampilkan balasan dari proyektor ke Serial Monitor.
 * (kode Anda yang lama, dipertahankan)
 */
void readAndDebugReply() {
  String reply = "";
  char incomingByte;
  unsigned long startTime = millis();

  // Batasan waktu untuk menunggu balasan agar loop tidak terhenti
  while (BenQ_Serial.available() || (millis() - startTime < 100)) { 
    if (BenQ_Serial.available()) {
      incomingByte = BenQ_Serial.read();
      
      // Tambahkan byte yang masuk ke string balasan
      // Karakter non-cetak (seperti CR, LF) akan ditampilkan sebagai [HEX]
      if (incomingByte >= 32 && incomingByte <= 126) {
        reply += incomingByte;
      } else {
        // Tampilkan HEX untuk karakter kontrol
        char buffer[5];
        sprintf(buffer, "[%02X]", incomingByte);
        reply += buffer;
      }
      
      // Reset timeout jika ada data masuk
      startTime = millis();
    }
    // Tambahkan delay singkat agar SoftwareSerial bisa bekerja lebih baik
    delay(2); 
  }

  if (reply.length() > 0) {
    Serial.print("[RECV] ");
    Serial.println(reply);
  }
}
