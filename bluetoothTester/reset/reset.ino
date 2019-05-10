#include <EEPROM.h>
void setup() {
  byte units = 1;
  short maxSpeed = 60;
  byte nMag = 2;
  float finalDrive = 1.0;
  long speedoRatio = 1400000;
  float wheel = 1.0438762;
  
  Serial.begin(9600);
  
  EEPROM.update(0, units);
  EEPROM.put(1, maxSpeed);
  EEPROM.update(3, nMag);
  EEPROM.put(4, finalDrive);
  EEPROM.put(8, speedoRatio);
  EEPROM.put(12, wheel);

  char wheelSize[11] = "P205/65R15";

  for(byte i = 0; i < 11; i++) {
      EEPROM.update(16 + i, wheelSize[i]);
    }
}

void loop() {
  // put your main code here, to run repeatedly:

}
