/*
 * SmartGasML - MG995 Servo Test
 * Tests servo movement on GPIO2
 * 
 * Wiring:
 * - Servo Signal -> GPIO2
 * - Servo VCC -> 5V (External power!)
 * - Servo GND -> GND
 */

#include <ESP32Servo.h>

Servo myServo;
#define SERVO_PIN 2

void setup() {
  Serial.begin(115200);
  delay(1000);
  
  Serial.println("\n=================================");
  Serial.println("   MG995 SERVO TEST");
  Serial.println("=================================");
  
  myServo.attach(SERVO_PIN);
  
  Serial.println("Commands:");
  Serial.println("  o - Open (90°)");
  Serial.println("  c - Close (0°)");
  Serial.println("  m - Middle (45°)");
  Serial.println("  t - Test sequence");
  Serial.println("=================================\n");
  
  myServo.write(0);
  Serial.println("Servo at 0° (closed)");
}

void loop() {
  if (Serial.available()) {
    char cmd = Serial.read();
    
    switch(cmd) {
      case 'o':
      case 'O':
        Serial.println("Moving to 90° (open)...");
        myServo.write(90);
        delay(500);
        Serial.print("Current angle: ");
        Serial.println(myServo.read());
        break;
        
      case 'c':
      case 'C':
        Serial.println("Moving to 0° (closed)...");
        myServo.write(0);
        delay(500);
        Serial.print("Current angle: ");
        Serial.println(myServo.read());
        break;
        
      case 'm':
      case 'M':
        Serial.println("Moving to 45° (middle)...");
        myServo.write(45);
        delay(500);
        Serial.print("Current angle: ");
        Serial.println(myServo.read());
        break;
        
      case 't':
      case 'T':
        testServo();
        break;
    }
  }
  delay(50);
}

void testServo() {
  Serial.println("\n--- SERVO TEST SEQUENCE ---");
  
  int positions[] = {0, 45, 90, 135, 180, 135, 90, 45, 0};
  
  for(int i = 0; i < 9; i++) {
    Serial.print("Moving to ");
    Serial.print(positions[i]);
    Serial.println("°");
    myServo.write(positions[i]);
    delay(1000);
  }
  
  Serial.println("Test complete!\n");
}