/*
 * SmartGasML - Passive Buzzer Test
 * Tests buzzer on GPIO19 with different frequencies
 * 
 * Wiring:
 * - Buzzer Signal (positive) -> GPIO19
 * - Buzzer GND -> GND
 */

#define BUZZER_PIN 19

void setup() {
  Serial.begin(115200);
  pinMode(BUZZER_PIN, OUTPUT);
  
  Serial.println("\n=================================");
  Serial.println("   SMARTGASML - BUZZER TEST");
  Serial.println("=================================\n");
}

void loop() {
  // Test 1: Single beep
  Serial.println("Single beep - 1 second");
  tone(BUZZER_PIN, 2000);
  delay(1000);
  noTone(BUZZER_PIN);
  delay(500);
  
  // Test 2: Multiple beeps
  Serial.println("Multiple beeps - 3 beeps");
  for(int i = 0; i < 3; i++) {
    tone(BUZZER_PIN, 2000);
    delay(200);
    noTone(BUZZER_PIN);
    delay(200);
  }
  delay(500);
  
  // Test 3: Frequency sweep
  Serial.println("Frequency sweep (500Hz to 3000Hz)");
  for(int freq = 500; freq <= 3000; freq += 100) {
    tone(BUZZER_PIN, freq);
    delay(30);
  }
  noTone(BUZZER_PIN);
  delay(500);
  
  // Test 4: Alarm pattern
  Serial.println("Alarm pattern (fast beeps)");
  for(int i = 0; i < 10; i++) {
    tone(BUZZER_PIN, 2500);
    delay(100);
    noTone(BUZZER_PIN);
    delay(100);
  }
  
  Serial.println("\nTest sequence complete. Restarting...\n");
  delay(2000);
}