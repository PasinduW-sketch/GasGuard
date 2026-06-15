/*
 * SmartGasML - I2C LCD Scanner
 * Finds the I2C address of your LCD display
 * 
 * Wiring:
 * - LCD VCC -> 5V
 * - LCD GND -> GND
 * - LCD SDA -> GPIO21
 * - LCD SCL -> GPIO22
 */

#include <Wire.h>

void setup() {
  Serial.begin(115200);
  delay(1000);
  
  Serial.println("\n=================================");
  Serial.println("   I2C LCD ADDRESS SCANNER");
  Serial.println("=================================");
  
  Wire.begin(21, 22);  // SDA=21, SCL=22
  Serial.println("Scanning I2C bus...");
  Serial.println("SDA=GPIO21, SCL=GPIO22");
}

void loop() {
  byte error, address;
  int deviceCount = 0;
  
  Serial.println("\nScanning addresses 1-127...");
  
  for (address = 1; address < 127; address++) {
    Wire.beginTransmission(address);
    error = Wire.endTransmission();
    
    if (error == 0) {
      Serial.print("✓ FOUND at 0x");
      if (address < 16) Serial.print("0");
      Serial.print(address, HEX);
      
      if (address == 0x27) Serial.print(" ← STANDARD LCD (0x27)");
      if (address == 0x3F) Serial.print(" ← STANDARD LCD (0x3F)");
      if (address == 0x20) Serial.print(" ← LCD (0x20)");
      
      Serial.println();
      deviceCount++;
    }
  }
  
  if (deviceCount == 0) {
    Serial.println("\n❌ NO I2C DEVICES FOUND!");
    Serial.println("\nCheck:");
    Serial.println("1. LCD VCC connected to 5V?");
    Serial.println("2. LCD GND connected to GND?");
    Serial.println("3. SDA→GPIO21, SCL→GPIO22?");
    Serial.println("4. I2C module firmly attached?");
  } else {
    Serial.print("\n✅ Found ");
    Serial.print(deviceCount);
    Serial.println(" device(s)");
    Serial.println("\n📝 USE THIS ADDRESS:");
    Serial.println("   LiquidCrystal_I2C lcd(0x27, 16, 2);");
  }
  
  delay(5000);
}