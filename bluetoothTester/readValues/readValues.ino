/* Example data in brackets of output
 * Should be in the same ballpark if sent data follows specifications
 *  Units: MPH/KMH
 *  maxSpeed: 60
 *  numMag: 2
 *  finalDrive Ratio: 1.00
 *  speedoRatio: 1400000 (1,400,000)
 *  Wheel Diameter: 1.043876
 */

#include <EEPROM.h>

long offset = 20;

void setup(){
  float temp;
  long ltemp;
  short sh;
  Serial.begin(9600);
  // assumes 1 = MPH
  //         0 = KMH
  Serial.print("Units: ");
  if(EEPROM.read(0)){
    Serial.println("KPH");
  } else {
    Serial.println("MPH");
  }
  

  Serial.print("maxSpeed: ");
  Serial.print(EEPROM.get(1, sh));
  Serial.println("   [60]");

  Serial.print("numMag: ");
  Serial.print(EEPROM.read(3));
  Serial.println("   [2]");

  Serial.print("finalDrive Ratio: ");
  Serial.print(EEPROM.get(4, temp), 9);
  Serial.println("   [1.00]");

  Serial.print("speedoRatio: ");
  Serial.print(EEPROM.get(8, ltemp));
  Serial.println("   [1400000]");

  Serial.print("Wheel Diameter: ");
  Serial.print(EEPROM.get(12, temp), 6);
  Serial.println("   [1.043876]");

  for(int i = 0; i < 13; i++) {
    Serial.println(EEPROM.read(offset + i));
  }  
}

void loop(){
  
}
