/*
 * SmartGasML - LED Test
 * Tests 3 Green LEDs (GPIO23) and 3 Red LEDs (GPIO32)
 * 
 * Wiring:
 * - Green LEDs: GPIO23 -> 220Ω -> LED+ -> LED- -> GND
 * - Red LEDs:   GPIO32 -> 220Ω -> LED+ -> LED- -> GND
 */

#define GREEN_LED_PIN 23
#define RED_LED_PIN   32

void setup() {
  Serial.begin(115200);
  pinMode(GREEN_LED_PIN, OUTPUT);
  pinMode(RED_LED_PIN, OUTPUT);
  
  Serial.println("\n=================================");
  Serial.println("   SMARTGASML - LED TEST");
  Serial.println("=================================\n");
}

void loop() {
  // Test 1: All Green ON
  Serial.println("Green LEDs ON");
  digitalWrite(GREEN_LED_PIN, HIGH);
  digitalWrite(RED_LED_PIN, LOW);
  delay(2000);
  
  // Test 2: All Red ON
  Serial.println("Red LEDs ON");
  digitalWrite(GREEN_LED_PIN, LOW);
  digitalWrite(RED_LED_PIN, HIGH);
  delay(2000);
  
  // Test 3: Alternate blinking (3 cycles)
  Serial.println("Alternating Blink");
  for(int i = 0; i < 6; i++) {
    digitalWrite(GREEN_LED_PIN, i % 2 == 0 ? HIGH : LOW);
    digitalWrite(RED_LED_PIN, i % 2 == 0 ? LOW : HIGH);
    delay(500);
  }
  
  // Test 4: All OFF
  Serial.println("All LEDs OFF");
  digitalWrite(GREEN_LED_PIN, LOW);
  digitalWrite(RED_LED_PIN, LOW);
  delay(2000);
  
  Serial.println("\nTest sequence complete. Restarting...\n");
}