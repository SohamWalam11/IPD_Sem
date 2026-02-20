#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEAdvertisedDevice.h>

// Set this to true if you want to see ALL devices to find yours first
bool SHOW_ALL_DEVICES = true; 

// Likely name of your sensor
String TARGET_NAME = "Treel"; 

int scanTime = 5; // seconds

class MyAdvertisedDeviceCallbacks: public BLEAdvertisedDeviceCallbacks {
    void onResult(BLEAdvertisedDevice advertisedDevice) {

      String deviceName = advertisedDevice.getName();

      // Filter device
      if (SHOW_ALL_DEVICES || deviceName.indexOf(TARGET_NAME) >= 0) {

        Serial.print("Device Found: ");
        Serial.println(deviceName);

        Serial.print("Address: ");
        Serial.println(advertisedDevice.getAddress().toString().c_str());

        // -------- Manufacturer Data --------
        if (advertisedDevice.haveManufacturerData()) {

          // FIX: use Arduino String
          String mfgData = advertisedDevice.getManufacturerData();

          Serial.print("RAW HEX (Manufacturer Data): ");

          for (int i = 0; i < mfgData.length(); i++) {
            Serial.printf("%02X ", (uint8_t)mfgData[i]);
          }
          Serial.println();
        }

        // -------- Service Data --------
        if (advertisedDevice.haveServiceData()) {

          int serviceCount = advertisedDevice.getServiceDataCount();
          for (int i = 0; i < serviceCount; i++) {

            // FIX: use Arduino String
            String serviceData = advertisedDevice.getServiceData(i);

            Serial.print("RAW HEX (Service Data): ");
            for (int j = 0; j < serviceData.length(); j++) {
              Serial.printf("%02X ", (uint8_t)serviceData[j]);
            }
            Serial.println();
          }
        }

        Serial.println("------------------------------------------------");
      }
    }
};

void setup() {
  Serial.begin(115200);
  Serial.println("Starting BLE Scanner...");

  BLEDevice::init("");
  BLEScan* pBLEScan = BLEDevice::getScan();
  pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
  pBLEScan->setActiveScan(true);
  pBLEScan->setInterval(100);
  pBLEScan->setWindow(99);
}

void loop() {
  BLEScan* pBLEScan = BLEDevice::getScan();
  pBLEScan->start(scanTime, false);
  pBLEScan->clearResults();
  delay(2000);
}
