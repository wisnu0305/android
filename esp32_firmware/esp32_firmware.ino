// ===== ESP32 FIRMWARE: WiFi + HTTP + Forward ke Arduino Uno =====
#include <WiFi.h>
#include <WebServer.h>

// ====== KONFIGURASI WiFi ======
const char* WIFI_SSID     = "RSUD_86x";      // SSID WiFi Anda
const char* WIFI_PASSWORD = "perangkatbaru";  // Password WiFi Anda

// UART2 ESP32 untuk komunikasi ke Arduino Uno
// GPIO16 = RX2, GPIO17 = TX2 pada DOIT ESP32 DevKit V1
HardwareSerial SerialUno(2);

// HTTP server di port 80
WebServer server(80);

// ---------- Utilitas baca satu baris dari Uno (dengan timeout) ----------
String readLineFromUno(unsigned long timeoutMs) {
  String line = "";
  unsigned long start = millis();
  bool gotData = false;

  Serial.print("[FROM UNO] Waiting for reply (timeout=");
  Serial.print(timeoutMs);
  Serial.println("ms)");

  while (millis() - start < timeoutMs) {
    if (SerialUno.available() > 0) {
      gotData = true;
      char c = (char)SerialUno.read();
      if (c == '\n') {
        Serial.print("[FROM UNO] Received line: '");
        Serial.print(line);
        Serial.println("'");
        return line;
      } else if (c != '\r' && c >= 32 && c <= 126) {
        // Hanya ambil karakter printable ASCII
        line += c;
      }
    } else if (gotData) {
      // Jika sudah dapat data tapi tidak ada data baru untuk sementara, anggap selesai
      delay(10);
      if (SerialUno.available() == 0) {
        Serial.print("[FROM UNO] Received partial line: '");
        Serial.print(line);
        Serial.println("'");
        return line;
      }
    }
    delay(5);
  }
  
  if (line.length() > 0) {
    Serial.print("[FROM UNO] Timeout, partial data: '");
    Serial.print(line);
    Serial.println("'");
  } else {
    Serial.println("[FROM UNO] Timeout, no data received");
  }
  return line;
}

// ---------- Kirim perintah ke Arduino Uno ----------
void sendToUno(const String& cmd) {
  // Format: "PON\n", "POFF\n", "VUP\n", "VDOWN\n", atau "RAW:*sour=hdmi#\n"
  SerialUno.print(cmd);
  SerialUno.print('\n');

  if (cmd.startsWith("RAW:")) {
    Serial.print("[UNO] Sent CUSTOM command: ");
    Serial.println(cmd);
  } else {
    Serial.print("[UNO] Sent STANDARD command: ");
    Serial.println(cmd);
  }
}

// ---------- Handler root ----------
void handleRoot() {
  String html = "Projector Remote ESP32 (gateway ke Arduino Uno)<br>";
  html += "IP: " + WiFi.localIP().toString() + "<br>";
  html += "Endpoint:<br>";
  html += "- GET /command?cmd=PON|POFF|VUP|VDOWN<br>";
  html += "- GET /commandRaw?raw=<command_string><br>";
  server.send(200, "text/html", html);
}

// ---------- Handler /command ----------
void handleCommand() {
  String cmd = server.arg("cmd");
  cmd.toUpperCase();
  cmd.trim();

  Serial.print("\n[HTTP] /command endpoint called, cmd=");
  Serial.println(cmd);

  if (cmd.length() == 0) {
    server.send(400, "text/plain", "Missing cmd");
    return;
  }

  String forward;
  String httpBody;

  if (cmd == "PON") {
    forward = "PON";
    httpBody = "Forward: PON ke Arduino Uno";
  } else if (cmd == "POFF") {
    forward = "POFF";
    httpBody = "Forward: POFF ke Arduino Uno";
  } else if (cmd == "VUP") {
    forward = "VUP";
    httpBody = "Forward: VUP ke Arduino Uno";
  } else if (cmd == "VDOWN") {
    forward = "VDOWN";
    httpBody = "Forward: VDOWN ke Arduino Uno";
  } else {
    Serial.print("[HTTP] Unknown standard command: ");
    Serial.println(cmd);
    server.send(400, "text/plain", "Unknown cmd: " + cmd);
    return;
  }

  Serial.print("[HTTP] Standard command detected: ");
  Serial.println(forward);

  // Kirim ke Uno
  sendToUno(forward);

  // Opsional: baca balasan singkat dari Uno (mis. "OK PON", "ERR", dsb.)
  String reply = readLineFromUno(500); // timeout 500 ms
  if (reply.length() > 0) {
    httpBody += "\nUNO_REPLY: " + reply;
    Serial.print("[HTTP] UNO reply: ");
    Serial.println(reply);
  }

  server.send(200, "text/plain", httpBody);
}

// Fungsi untuk decode URL (mengubah %23 menjadi #, %20 menjadi spasi, dll)
String urlDecode(String str) {
  String decoded = "";
  char c;
  for (int i = 0; i < str.length(); i++) {
    c = str.charAt(i);
    if (c == '%' && i + 2 < str.length()) {
      // Decode %XX menjadi karakter ASCII
      char hex[3] = {str.charAt(i+1), str.charAt(i+2), '\0'};
      int value = strtol(hex, NULL, 16);
      decoded += (char)value;
      i += 2;
    } else if (c == '+') {
      decoded += ' '; // + menjadi spasi di URL
    } else {
      decoded += c;
    }
  }
  return decoded;
}

// ---------- Handler /commandRaw (untuk custom command string) ----------
void handleCommandRaw() {
  String rawCmd = server.arg("raw");
  
  Serial.print("\n[HTTP] /commandRaw endpoint called");
  Serial.print("\n[HTTP] Raw parameter (before decode): '");
  Serial.print(rawCmd);
  Serial.print("' (length=");
  Serial.print(rawCmd.length());
  Serial.println(")");

  // Decode URL (mengubah %23 menjadi #, dll)
  rawCmd = urlDecode(rawCmd);
  rawCmd.trim();

  Serial.print("[HTTP] After URL decode: '");
  Serial.print(rawCmd);
  Serial.print("' (length=");
  Serial.print(rawCmd.length());
  Serial.println(")");

  if (rawCmd.length() == 0) {
    Serial.println("[HTTP] ERROR: Missing raw parameter");
    server.send(400, "text/plain", "Missing raw parameter");
    return;
  }

  // Auto-append '#' untuk command BenQ yang dimulai dengan '*' tapi tidak berakhir dengan '#'
  if (rawCmd.startsWith("*") && !rawCmd.endsWith("#")) {
    Serial.print("[HTTP] Auto-appending '#' untuk command BenQ: '");
    Serial.print(rawCmd);
    Serial.println("'");
    rawCmd += "#";
    Serial.print("[HTTP] Command setelah auto-append: '");
    Serial.print(rawCmd);
    Serial.println("'");
  } else if (!rawCmd.endsWith("#") && rawCmd.length() > 0) {
    Serial.print("[HTTP] WARNING: Command tidak berakhir dengan '#': '");
    Serial.print(rawCmd);
    Serial.println("'");
    Serial.println("[HTTP] Command tetap akan dikirim tanpa auto-append");
  }

  Serial.print("[HTTP] CUSTOM COMMAND detected: '");
  Serial.print(rawCmd);
  Serial.println("'");

  // Forward ke Uno dengan prefix "RAW:" supaya Uno tahu ini command string mentah
  String forward = "RAW:" + rawCmd;
  Serial.print("[HTTP] Forwarding to UNO as: '");
  Serial.print(forward);
  Serial.println("'");
  
  sendToUno(forward);

  // Beri waktu sedikit untuk Uno memproses
  delay(50);
  
  // Baca balasan dari Uno
  Serial.println("[HTTP] Reading reply from UNO...");
  String reply = readLineFromUno(1000); // timeout lebih lama: 1000ms
  String httpBody = "Forward raw command: " + rawCmd;
  if (reply.length() > 0) {
    httpBody += "\nUNO_REPLY: " + reply;
    Serial.print("[HTTP] UNO reply received: '");
    Serial.print(reply);
    Serial.println("'");
  } else {
    Serial.println("[HTTP] WARNING: No reply from UNO (timeout atau komunikasi bermasalah)");
    httpBody += "\nUNO_REPLY: <timeout>";
  }

  server.send(200, "text/plain", httpBody);
}

// ---------- Stub /setCustom (agar Android Settings tidak error) ----------
void handleSetCustom() {
  // Kita tidak perlu profil di ESP32 lagi, karena semua RS232 diurus Uno.
  server.send(200, "text/plain", "OK (profil RS232 di-handle Arduino Uno)");
}

// ---------- SETUP ----------
void setup() {
  Serial.begin(115200);
  Serial.println("\n\n=== ESP32 Gateway (WiFi + HTTP + link ke Arduino Uno) ===");

  // UART2 ke Uno: 9600-8N1
  SerialUno.begin(9600, SERIAL_8N1, 16, 17);
  Serial.println("Serial ke Arduino Uno (UART2) init OK");

  // WiFi
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();
  Serial.println("WiFi connected");
  Serial.print("IP: ");
  Serial.println(WiFi.localIP());

  // Routing HTTP
  server.on("/", handleRoot);
  server.on("/command", handleCommand);
  server.on("/commandRaw", handleCommandRaw); // endpoint untuk custom command
  server.on("/setCustom", handleSetCustom); // supaya Settings Android tetap dapat "OK"
  server.begin();
  Serial.println("HTTP server started on port 80");
}

// ---------- LOOP ----------
void loop() {
  server.handleClient();
}