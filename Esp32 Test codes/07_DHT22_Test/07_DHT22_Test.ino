/*
 * SmartGasML - DHT22 Temperature & Humidity Test
 * Reads temperature and humidity from GPIO4
 * 
 * Wiring:
 * - DHT22 VCC -> 5V
 * - DHT22 GND -> GND
 * - DHT22 Data -> GPIO4 (with 10kΩ pull-up resistor)
 */

#include <DHT.h>

#define DHTPIN 4
#define DHTTYPE DHT22

DHT dht(DHTPIN, DHTTYPE);

void setup() {
  Serial.begin(115200);
  delay(1000);
  
  Serial.println("\n=================================");
  Serial.println("   DHT22 SENSOR TEST");
  Serial.println("=================================");
  
  dht.begin();
  Serial.println("Sensor initialized. Reading data...\n");
}

void loop() {
  // Wait 2 seconds between readings
  delay(2000);
  
  float humidity = dht.readHumidity();
  float temperature = dht.readTemperature();
  
  // Check if readings are valid
  if (isnan(humidity) || isnan(temperature)) {
    Serial.println("❌ Failed to read from DHT22 sensor!");
    Serial.println("   Check wiring and pull-up resistor\n");
    return;
  }
  
  Serial.println("--- DHT22 Readings ---");
  Serial.print("Temperature: ");
  Serial.print(temperature);
  Serial.println(" °C");
  
  Serial.print("Humidity: ");
  Serial.print(humidity);
  Serial.println(" %");
  
  // Status indicators
  if (temperature > 35) {
    Serial.println("   ⚠️  High temperature!");
  }
  if (humidity > 70) {
    Serial.println("   ⚠️  High humidity!");
  }
  
  Serial.println("----------------------\n");
}