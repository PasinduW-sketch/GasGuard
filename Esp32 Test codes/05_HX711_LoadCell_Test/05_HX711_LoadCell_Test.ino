/*
 * SmartGasML - HX711 Load Cell Test
 * Reads weight from load cell
 * 
 * Wiring:
 * - HX711 VCC -> 5V
 * - HX711 GND -> GND
 * - HX711 DT -> GPIO16
 * - HX711 SCK -> GPIO17
 * - Load Cell Red -> E+
 * - Load Cell Black -> E-
 * - Load Cell Green -> A+
 * - Load Cell White -> A-
 */

#include <HX711.h>

#define LOADCELL_DT_PIN  16
#define LOADCELL_SCK_PIN 17

HX711 scale;

void setup() {
  Serial.begin(115200);
  delay(1000);
  
  Serial.println("\n=================================");
  Serial.println("   HX711 LOAD CELL TEST");
  Serial.println("=================================");
  
  scale.begin(LOADCELL_DT_PIN, LOADCELL_SCK_PIN);
  
  Serial.println("Place known weight on scale for calibration");
  Serial.println("Commands:");
  Serial.println("  t - Tare (zero) the scale");
  Serial.println("  c - Calibrate with known weight");
  Serial.println("  r - Read raw value");
  Serial.println("=================================\n");
  
  // Initial tare
  scale.tare();
  Serial.println("Scale tared. Ready for readings.");
}

void loop() {
  if (Serial.available()) {
    char cmd = Serial.read();
    
    switch(cmd) {
      case 't':
      case 'T':
        scale.tare();
        Serial.println("Scale tared (zero set)");
        break;
        
      case 'c':
      case 'C':
        calibrateScale();
        break;
        
      case 'r':
      case 'R':
        Serial.print("Raw reading: ");
        Serial.println(scale.read());
        break;
    }
  }
  
  // Read weight
  float weight = scale.get_units(5);  // Average of 5 readings
  Serial.print("Weight: ");
  Serial.print(weight);
  Serial.println(" kg");
  
  delay(1000);
}

void calibrateScale() {
  Serial.println("\n--- CALIBRATION MODE ---");
  Serial.println("Place known weight on scale");
  Serial.print("Enter weight in kg: ");
  
  while(!Serial.available()) {
    delay(100);
  }
  
  float knownWeight = Serial.parseFloat();
  if(knownWeight > 0) {
    scale.set_scale(scale.get_units(10) / knownWeight);
    Serial.print("Calibration factor set to: ");
    Serial.println(scale.get_scale());
  } else {
    Serial.println("Invalid weight. Calibration cancelled.");
  }
}