/*
 * SMARTGASML - Complete Gas Monitoring System
 * ESP32 DevKit V1
 * 
 * Components:
 * - MQ-5 Gas Sensor (A0 on GPIO34)
 * - I2C LCD Display (SDA=21, SCL=22)
 * - 3x Green LEDs (GPIO23) - Normal status
 * - 3x Red LEDs (GPIO32) - Alarm status
 * - Passive Buzzer (GPIO19) - Audible alarm
 * - MG995 Servo Motor (GPIO2) - Gas valve control
 * - HX711 Load Cell (GPIO16, GPIO17)
 * - DHT22 Temperature/Humidity (GPIO4)
 * 
 * All components powered from common 5V rail (LM2596)
 */

#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <ESP32Servo.h>
#include <HX711.h>
#include <DHT.h>

// ========== PIN DEFINITIONS ==========
// Gas Sensor
#define MQ5_PIN         34

// LED Indicators
#define GREEN_LED_PIN   23
#define RED_LED_PIN     32

// Buzzer
#define BUZZER_PIN      19

// Servo
#define SERVO_PIN       2

// HX711 Load Cell
#define LOADCELL_DT     16
#define LOADCELL_SCK    17

// DHT22
#define DHTPIN          4
#define DHTTYPE         DHT22

// I2C LCD
#define I2C_SDA         21
#define I2C_SCL         22
#define LCD_ADDRESS     0x27    // Change to your LCD address (0x27 or 0x3F)
#define LCD_COLUMNS     16
#define LCD_ROWS        2

// ========== CONFIGURATION ==========
// Gas detection threshold (0-4095, lower = more sensitive)
#define GAS_THRESHOLD   700

// Alarm timing
#define ALARM_BLINK     200     // Blink rate (ms)
#define BUZZER_FREQ     2000    // Buzzer frequency (Hz)

// Servo positions
#define VALVE_CLOSED    0       // 0 degrees
#define VALVE_OPEN      90      // 90 degrees

// System timing
#define SENSOR_DELAY    100     // Main loop delay (ms)
#define LCD_UPDATE_DELAY 2000   // LCD update interval (ms)
#define STATUS_PRINT_DELAY 30000 // Status print interval (ms)

// Load cell calibration (adjust after calibration)
#define CALIBRATION_FACTOR 705.0  // Your calibration factor

// ========== OBJECTS ==========
LiquidCrystal_I2C lcd(LCD_ADDRESS, LCD_COLUMNS, LCD_ROWS);
Servo gasValve;
HX711 scale;
DHT dht(DHTPIN, DHTTYPE);

// ========== GLOBAL VARIABLES ==========
bool alarmActive = false;
bool gasDetected = false;
unsigned long lastBlinkTime = 0;
bool blinkState = false;
int gasLevel = 0;
float gasWeight = 0;
float temperature = 0;
float humidity = 0;
float baselineGas = 0;

// Debounce timers
unsigned long clearTime = 0;
unsigned long lastLCDUpdate = 0;
unsigned long lastStatusPrint = 0;
unsigned long lastSensorRead = 0;

// ========== SETUP ==========
void setup() {
  Serial.begin(115200);
  delay(1000);
  
  Serial.println("\n╔══════════════════════════════════════════════════════════╗");
  Serial.println("║               SMARTGASML - GAS MONITORING SYSTEM        ║");
  Serial.println("║                         v2.0                             ║");
  Serial.println("╚══════════════════════════════════════════════════════════╝\n");
  
  // Initialize pins
  pinMode(GREEN_LED_PIN, OUTPUT);
  pinMode(RED_LED_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  
  // Set initial states
  digitalWrite(GREEN_LED_PIN, HIGH);
  digitalWrite(RED_LED_PIN, LOW);
  noTone(BUZZER_PIN);
  
  // Initialize I2C
  Wire.begin(I2C_SDA, I2C_SCL);
  
  // Initialize LCD
  lcd.init();
  lcd.backlight();
  lcd.setCursor(0, 0);
  lcd.print("SmartGasML v2.0");
  lcd.setCursor(0, 1);
  lcd.print("Initializing...");
  
  // Initialize Servo
  gasValve.attach(SERVO_PIN);
  gasValve.write(VALVE_CLOSED);
  
  // Initialize HX711
  scale.begin(LOADCELL_DT, LOADCELL_SCK);
  scale.set_scale(CALIBRATION_FACTOR);
  scale.tare();
  
  // Initialize DHT22
  dht.begin();
  
  // System info
  printSystemInfo();
  
  // Warm up MQ-5 sensor
  warmUpMQ5();
  
  // System ready
  digitalWrite(GREEN_LED_PIN, HIGH);
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("System Ready!");
  lcd.setCursor(0, 1);
  lcd.print("Monitoring...");
  
  Serial.println("\n✅ SYSTEM READY - Monitoring for gas leaks");
  Serial.println("═══════════════════════════════════════════════════════════\n");
}

// ========== SYSTEM INFO ==========
void printSystemInfo() {
  Serial.println("📋 SYSTEM CONFIGURATION:");
  Serial.println("  MQ-5 Gas Sensor:  GPIO34 (Analog)");
  Serial.println("  Green LEDs:       GPIO23");
  Serial.println("  Red LEDs:         GPIO32");
  Serial.println("  Buzzer:           GPIO19 (tone)");
  Serial.println("  Servo Valve:      GPIO2");
  Serial.println("  HX711 Load Cell:  DT=16, SCK=17");
  Serial.println("  DHT22:            GPIO4");
  Serial.println("  I2C LCD:          SDA=21, SCL=22");
  Serial.print("  LCD Address:       0x");
  Serial.println(LCD_ADDRESS, HEX);
  Serial.print("  Gas Threshold:     ");
  Serial.println(GAS_THRESHOLD);
  Serial.println("  Commands: S=status, T=test, +=more sensitive, -=less sensitive");
  Serial.println("═══════════════════════════════════════════════════════════\n");
}

// ========== MQ-5 WARM-UP ==========
void warmUpMQ5() {
  Serial.println("🔥 MQ-5 Sensor warm-up (30 seconds)...");
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Warming MQ-5");
  
  unsigned long start = millis();
  int baselineSum = 0;
  int baselineCount = 0;
  
  while (millis() - start < 30000) {
    baselineSum += analogRead(MQ5_PIN);
    baselineCount++;
    
    int progress = (millis() - start) / 300;
    lcd.setCursor(0, 1);
    lcd.print("Progress: ");
    lcd.print(progress);
    lcd.print("%   ");
    
    digitalWrite(GREEN_LED_PIN, (progress % 20 < 10) ? HIGH : LOW);
    delay(100);
  }
  
  baselineGas = baselineSum / baselineCount;
  
  Serial.print("✅ Warm-up complete! Baseline gas level: ");
  Serial.println(baselineGas);
  
  digitalWrite(GREEN_LED_PIN, HIGH);
}

// ========== SENSOR READINGS ==========
void readAllSensors() {
  // Read gas sensor
  gasLevel = analogRead(MQ5_PIN);
  
  // Read load cell
  if (scale.wait_ready_timeout(100)) {
    gasWeight = scale.get_units(3);
    if (gasWeight < 0) gasWeight = 0;
  }
  
  // Read DHT22
  humidity = dht.readHumidity();
  temperature = dht.readTemperature();
  
  // Check for invalid readings
  if (isnan(humidity) || isnan(temperature)) {
    humidity = 0;
    temperature = 0;
  }
}

// ========== GAS DETECTION ==========
bool checkForGas() {
  return (gasLevel > GAS_THRESHOLD);
}

// ========== ALARM FUNCTIONS ==========
void activateAlarm() {
  if (!alarmActive) {
    alarmActive = true;
    
    digitalWrite(GREEN_LED_PIN, LOW);
    gasValve.write(VALVE_CLOSED);
    
    Serial.println("\n🚨🚨🚨 GAS LEAK DETECTED! 🚨🚨🚨");
    Serial.print("   Gas Level: ");
    Serial.print(gasLevel);
    Serial.print(" (Threshold: ");
    Serial.print(GAS_THRESHOLD);
    Serial.println(")");
    Serial.println("   Valve CLOSED - Gas supply shut off");
    
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("🚨 LEAK! 🚨");
    lcd.setCursor(0, 1);
    lcd.print("Valve: CLOSED");
  }
}

void deactivateAlarm() {
  if (alarmActive) {
    alarmActive = false;
    
    digitalWrite(GREEN_LED_PIN, HIGH);
    digitalWrite(RED_LED_PIN, LOW);
    noTone(BUZZER_PIN);
    gasValve.write(VALVE_OPEN);
    
    Serial.println("\n✅✅✅ ALARM CLEARED ✅✅✅");
    Serial.println("   Gas levels returned to normal");
    Serial.println("   Valve OPENED");
    
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("System Normal");
    lcd.setCursor(0, 1);
    lcd.print("Valve: OPEN");
  }
}

void updateAlarmOutputs() {
  if (!alarmActive) return;
  
  unsigned long currentTime = millis();
  
  if (currentTime - lastBlinkTime > ALARM_BLINK) {
    blinkState = !blinkState;
    
    digitalWrite(RED_LED_PIN, blinkState ? HIGH : LOW);
    
    if (blinkState) {
      tone(BUZZER_PIN, BUZZER_FREQ);
    } else {
      noTone(BUZZER_PIN);
    }
    
    lastBlinkTime = currentTime;
  }
}

// ========== LCD DISPLAY ==========
void updateLCD() {
  unsigned long currentTime = millis();
  
  if (currentTime - lastLCDUpdate < LCD_UPDATE_DELAY) return;
  lastLCDUpdate = currentTime;
  
  lcd.clear();
  
  if (alarmActive) {
    lcd.setCursor(0, 0);
    lcd.print("⚠️ LEAK! ⚠️");
    lcd.setCursor(0, 1);
    lcd.print("Gas: ");
    lcd.print(gasLevel);
    lcd.print("  ");
  } else {
    // First line: Gas info
    lcd.setCursor(0, 0);
    lcd.print("G:");
    lcd.print(gasLevel);
    lcd.print(" W:");
    lcd.print(gasWeight, 1);
    lcd.print("kg");
    
    // Second line: Temperature and humidity
    lcd.setCursor(0, 1);
    lcd.print("T:");
    lcd.print(temperature, 1);
    lcd.print("C H:");
    lcd.print(humidity, 0);
    lcd.print("%");
    
    // Gas level bar graph
    int barLength = map(gasLevel, 0, GAS_THRESHOLD * 1.5, 0, 8);
    barLength = constrain(barLength, 0, 8);
    
    lcd.setCursor(12, 1);
    for (int i = 0; i < barLength; i++) {
      lcd.print((char)255);
    }
  }
}

// ========== STATUS DISPLAY ==========
void printStatus() {
  Serial.println("\n📊 SYSTEM STATUS:");
  Serial.print("  Gas Level:      ");
  Serial.print(gasLevel);
  Serial.print(" / 4095 (");
  Serial.print((gasLevel * 100) / 4095);
  Serial.println("%)");
  
  Serial.print("  Gas Weight:     ");
  Serial.print(gasWeight);
  Serial.println(" kg");
  
  Serial.print("  Temperature:    ");
  Serial.print(temperature);
  Serial.println(" °C");
  
  Serial.print("  Humidity:       ");
  Serial.print(humidity);
  Serial.println(" %");
  
  Serial.print("  Alarm State:    ");
  Serial.println(alarmActive ? "ACTIVE 🔴" : "INACTIVE 🟢");
  
  Serial.print("  Valve State:    ");
  Serial.println(alarmActive ? "CLOSED" : "OPEN");
  
  Serial.print("  Uptime:         ");
  Serial.print(millis() / 1000);
  Serial.println(" seconds");
}

// ========== TEST ALARM ==========
void testAlarm() {
  Serial.println("\n🔊 TESTING ALARM SYSTEM...");
  
  bool savedState = alarmActive;
  
  alarmActive = true;
  digitalWrite(GREEN_LED_PIN, LOW);
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("TEST MODE");
  lcd.setCursor(0, 1);
  lcd.print("Alarm Testing");
  
  for (int i = 0; i < 10; i++) {
    blinkState = !blinkState;
    digitalWrite(RED_LED_PIN, blinkState ? HIGH : LOW);
    
    if (blinkState) {
      tone(BUZZER_PIN, BUZZER_FREQ);
    } else {
      noTone(BUZZER_PIN);
    }
    delay(200);
  }
  
  alarmActive = savedState;
  
  if (!alarmActive) {
    digitalWrite(GREEN_LED_PIN, HIGH);
    digitalWrite(RED_LED_PIN, LOW);
    noTone(BUZZER_PIN);
    
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("System Normal");
    lcd.setCursor(0, 1);
    lcd.print("Test Complete");
    delay(1500);
  }
  
  Serial.println("✅ Test complete!");
}

// ========== ADJUST SENSITIVITY ==========
void increaseSensitivity() {
  gasThreshold = max(200, GAS_THRESHOLD - 50);
  Serial.print("⬆️ Increased sensitivity. New threshold: ");
  Serial.println(gasThreshold);
}

void decreaseSensitivity() {
  gasThreshold = min(3500, GAS_THRESHOLD + 50);
  Serial.print("⬇️ Decreased sensitivity. New threshold: ");
  Serial.println(gasThreshold);
}

// ========== CALIBRATE LOAD CELL ==========
void calibrateLoadCell() {
  Serial.println("\n📏 LOAD CELL CALIBRATION");
  Serial.println("Remove all weight from scale");
  Serial.print("Press 't' to tare, then enter known weight: ");
  
  while (!Serial.available()) delay(100);
  
  if (Serial.read() == 't') {
    scale.tare();
    Serial.println("\nScale tared. Place known weight and enter value:");
    
    while (!Serial.available()) delay(100);
    float knownWeight = Serial.parseFloat();
    
    if (knownWeight > 0) {
      float factor = scale.get_units(10) / knownWeight;
      scale.set_scale(factor);
      Serial.print("Calibration factor: ");
      Serial.println(factor);
      Serial.println("Update CALIBRATION_FACTOR in code with this value");
    }
  }
}

// ========== MANUAL VALVE CONTROL ==========
void openValve() {
  if (!alarmActive) {
    gasValve.write(VALVE_OPEN);
    Serial.println("Valve OPENED");
    lcd.setCursor(0, 1);
    lcd.print("Valve: OPEN   ");
  } else {
    Serial.println("Cannot open valve - leak detected!");
  }
}

void closeValve() {
  gasValve.write(VALVE_CLOSED);
  Serial.println("Valve CLOSED");
  lcd.setCursor(0, 1);
  lcd.print("Valve: CLOSED ");
}

// ========== MAIN LOOP ==========
void loop() {
  // Read all sensors
  readAllSensors();
  
  // Check for gas
  bool currentGasDetected = checkForGas();
  
  // Handle gas detection state
  if (currentGasDetected && !gasDetected) {
    gasDetected = true;
    activateAlarm();
    clearTime = 0;
  }
  
  if (gasDetected && !currentGasDetected) {
    if (clearTime == 0) {
      clearTime = millis();
      Serial.println("\n⚠️ Gas cleared. Waiting 5 seconds...");
      lcd.setCursor(0, 0);
      lcd.print("Confirming Clear");
    }
    
    if (millis() - clearTime > 5000) {
      gasDetected = false;
      deactivateAlarm();
      clearTime = 0;
    }
  } else if (!gasDetected) {
    clearTime = 0;
  }
  
  // Update alarm outputs
  if (alarmActive) {
    updateAlarmOutputs();
  }
  
  // Update LCD
  updateLCD();
  
  // Periodic status print
  if (!alarmActive && (millis() - lastStatusPrint > STATUS_PRINT_DELAY)) {
    printStatus();
    lastStatusPrint = millis();
  }
  
  // Serial commands
  if (Serial.available()) {
    char cmd = Serial.read();
    
    switch(cmd) {
      case 's': case 'S':
        printStatus();
        break;
      case 't': case 'T':
        testAlarm();
        break;
      case '+':
        increaseSensitivity();
        break;
      case '-':
        decreaseSensitivity();
        break;
      case 'o': case 'O':
        openValve();
        break;
      case 'c': case 'C':
        closeValve();
        break;
      case 'l': case 'L':
        calibrateLoadCell();
        break;
      case 'h': case 'H':
        Serial.println("\nCOMMANDS:");
        Serial.println("  s - Show status");
        Serial.println("  t - Test alarm");
        Serial.println("  o - Open valve");
        Serial.println("  c - Close valve");
        Serial.println("  + - Increase sensitivity");
        Serial.println("  - - Decrease sensitivity");
        Serial.println("  l - Calibrate load cell");
        break;
    }
  }
  
  delay(SENSOR_DELAY);
}