#include "HX711.h"

// ---------------------------------------------------------------------
// CONFIGURATION
// ---------------------------------------------------------------------
// Pin definitions (Matches the wiring instructions I gave you)
const int LOADCELL_DOUT_PIN = 21;
const int LOADCELL_SCK_PIN = 22;

// Initialize the library
HX711 scale;

void setup() {
  // Start Serial Monitor at 115200 baud
  Serial.begin(115200);
  Serial.println("\n-------------------------------------");
  Serial.println("TyreGuard AI: Hardware Test Initialization");
  Serial.println("-------------------------------------");

  // Initialize the scale
  scale.begin(LOADCELL_DOUT_PIN, LOADCELL_SCK_PIN);

  // 1. Wait for the sensor to become ready
  Serial.print("Connecting to HX711...");
  // Timeout usually indicates bad wiring
  unsigned long startTime = millis();
  while (!scale.is_ready()) {
    if (millis() - startTime > 2000) {
      Serial.println("\n[ERROR] HX711 not found.");
      Serial.println(" - Check your VCC/GND wires.");
      Serial.println(" - Check if DT is in Pin 21 and SCK in Pin 22.");
      while(1); // Stop here if hardware fails
    }
    delay(10);
  }
  Serial.println(" [CONNECTED]");

  // 2. Tare the scale (Reset to 0)
  // This assumes no weight is on the sensor when you turn it on.
  Serial.println("Tareing (Zeroing) the scale...");
  scale.set_scale(); // Set scale to default 1
  scale.tare();      // Reset the scale to 0
  
  Serial.println("Setup Complete. Reading data...");
}

void loop() {
  // Check if the scale is ready to provide data
  if (scale.is_ready()) {
    // Read the raw value (long integer)
    long reading = scale.get_units(5); // Average of 5 readings
    
    Serial.print("Raw Weight Value: ");
    Serial.println(reading);
    
    // In the future, we will convert 'reading' to Kilograms/PSI 
    // using a calibration factor.
  } else {
    Serial.println("HX711 not ready...");
  }

  // Slow down the loop slightly
  delay(250);
}