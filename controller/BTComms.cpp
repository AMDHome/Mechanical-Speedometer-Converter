#include <EEPROM.h>
#include "wiring2.h"
#include "BTComms.h"

void checkBT() {
  static char data[26];
  float tempF;
  long tempL;
  char *tempC;
  byte i;

  // check and recieve data
  switch(recvData(data)) {
    case 'U': // Store Units
      EEPROM.update(0, data[0] - '0');
      break;

    case 'M': // Store Max Speed
      maxSpeed = (short) atol(data);
      EEPROM.put(1, maxSpeed);
      break;
      
    case 'N': // Store Number of Magnets
      EEPROM.update(3, data[0] - '0');
      updateInRatio(EEPROM.read(3), EEPROM.get(12, tempL), EEPROM.get(4, tempF));
      break;

    case 'F': // Store Final Drive Ratio
      EEPROM.put(4, ((float) atol(data)) / 1000000);
      updateInRatio(EEPROM.read(3), EEPROM.get(12, tempL), EEPROM.get(4, tempF));
      break;

    case 'S': // Store Speedometer Ratio
      outRatio = atol(data);
      EEPROM.put(8, outRatio);
      break;

    case 'W': // Store Tire Size & Circumference
      EEPROM.put(12, atol(strtok(data, ":")));

      tempC = strtok(NULL, ":");
      for(i = 0; i < 14 && tempC[i] != '\0'; i++) {
        EEPROM.update(16 + i, tempC[i]);
      }
      EEPROM.update(16 + i, '\0');

      updateInRatio(EEPROM.read(3), EEPROM.get(12, tempL), EEPROM.get(4, tempF));
      break;

    case 'P':
      CallibrationMode = data[0] - '0';
      targetRPM = 0;
      break;

    case 'T':
      if(CallibrationMode) {
        targetRPM = atol(data);
      }
      break;

    case 'L':
      if(data[0] - '0') {
        dataDump();
      }
      break;

    case 'D':
      if (data[0] == 'S')
      {
        CallibrationMode = 2;
        targetRPM = 0;
        rTime = 153;
        calTicks = 0;
      } else {

        tempL = atol(data);

        if(tempL) {
          // Do calculations, update ratios and send value off
          
          tempF = (140625.0 * calTicks) / (39168.0 * EEPROM.read(3) * tempL);
          tempF *= EEPROM.get(12, tempL);

          EEPROM.put(4, tempF);
          updateInRatio(EEPROM.read(3), EEPROM.get(12, tempL), EEPROM.get(4, tempF));
          
          Serial.print((long) (tempF * 1000000));
        }

        CallibrationMode = 0;

        break;
      }

    case 'B':
      debug = data[0] - '0';
      break;

    case 'X':
      KP = atol(data);
      Serial.println(KP);
      delay2(1000);
      break;

    case 'Y':
      KI = atol(data);
      Serial.println(KI);
      delay2(1000);
      break;

    case 'Z':
      KD = atol(data);
      Serial.println(KD);
      delay2(1000);
      break;

    case '\0':
    default:
      break;
  }
}

char recvData(char* data) {
  char type = '\0';
  byte itr;

  if(Serial.available() > 2) {
    type = Serial.read();

    if(!isupper(type)) {
      digitalWrite(LED_BUILTIN, HIGH);
      return '\0';
    }

    data[0] = Serial.read();

    if(data[0] != ':') {
      digitalWrite(LED_BUILTIN, HIGH);
      return '\0';
    }

    delay2(1);

    for(itr = 0; itr < 26 && (Serial.available() > 0); itr++) {
      data[itr] = Serial.read();
      delay2(1);
    }

    if(itr == 13 && data[10] != '\0') {
      digitalWrite(LED_BUILTIN, HIGH);
      return '\0';
    }

    digitalWrite(LED_BUILTIN, LOW);
  }
  
  return type;
}



void dataDump() {
  char out[50];
  byte index = 4;

  unsigned short tempS;
  unsigned long tempL;
  float tempF;

  // Header
  out[0] = 'D';
  out[1] = ':';

  // Units
  out[2] = EEPROM.read(0) + '0';
  out[3] = ':';

  // Max Speed
  utoa((int) EEPROM.get(1, tempS), out + index, 10);
  index += strlen(out + index);
  out[index++] = ':';

  // Number of Magnets
  out[index++] = EEPROM.read(3) + '0';
  out[index++] = ':';

  // Final Drive Ratio
  tempL = EEPROM.get(4, tempF) * 1000000;
  ultoa(tempL, out + index, 10);
  index += strlen(out + index);
  out[index++] = ':';

  // Speedometer Ratio
  ultoa(EEPROM.get(8, tempL), out + index, 10);
  index += strlen(out + index);
  out[index++] = ':';

  // Wheel Circumference
  for(byte i = 0; i < 13; i++){
    out[index + i] = EEPROM.read(16 + i);

    if(out[index + i] == '\0')
      break;
  }
  
  Serial.println(out);
}
