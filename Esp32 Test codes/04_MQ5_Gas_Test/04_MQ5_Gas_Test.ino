/*
 * SmartGasML - MQ-5 Gas Sensor Test
 * Reads analog values from GPIO34
 * 
 * Wiring:
 * - MQ-5 VCC -> 5V
 * - MQ-5 GND -> GND
 * - MQ-5 A0 -> GPIO34
 */

#define MQ5_PIN 34

void setup() {
  Serial.begin(115200);
  delay(1000);
  
  Serial.println("\n=================================");
  Serial.println("   MQ-5 GAS SENSOR TEST");
  Serial.println("=================================");
  Serial.println("Warming up sensor (30 seconds)...");
  Serial.println("Readings will stabilize after warm-up");
  
  // Warm-up countdown
  for(int i = 30; i > 0; i--) {
    Serial.print("Warming up: ");
    Serial.print(i);
    Serial.println(" seconds remaining");
    delay(1000);
  }
  
  Serial.println("\nSensor ready!");
  Serial.println("Expose sensor to gas to see readings increase\n");
}

void loop() {
  int gasValue = analogRead(MQ5_PIN);
  
  Serial.print("Gas Level: ");
  Serial.print(gasValue);
  Serial.print(" / 4095 (");
  Serial.print((gasValue * 100) / 4095);
  Serial.println("%)");
  
  // Warning thresholds
  if(gasValue > 700) {
    Serial.println("   ⚠️  WARNING: Elevated gas level!");
  }
  if(gasValue > 1000) {
    Serial.println("   🚨 ALERT: High gas concentration!");
  }
  
  Serial.println("------------------------");
  delay(1000);
}