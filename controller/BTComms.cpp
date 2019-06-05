#include <EEPROM.h>
#include "wiring2.h"
#include "BTComms.h"

void checkBT() {
  static char buf[26];
  static byte dPos;
  static char *data = buf + 2;
  float tempF;
  long tempL;
  char *tempC;
  byte i;

  // check and recieve data
  switch(recvData(buf, &dPos)) {
    case 'U': // Store Units
      EEPROM.update(0, data[0] - '0');
      break;

    case 'M': // Store Max Speed
      maxSpeed = (short) atol(data);
      EEPROM.put(1, maxSpeed);
      maxSpeed *= 10;
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
      OCR1A = 0;
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
          char finalDrive[15];
          finalDrive[0] = 'F';
          finalDrive[1] = ':';
          
          // Do calculations, update ratios and send value off
          tempF = (256.0 * encoderIntCount * tempL * EEPROM.read(3) * 153) / (9.0 * calTicks);
          tempF /= (EEPROM.get(12, tempL));

          EEPROM.put(4, tempF);
          updateInRatio(EEPROM.read(3), EEPROM.get(12, tempL), EEPROM.get(4, tempF));

          sprintf(finalDrive + 2, "%lu\0", (unsigned long) (tempF * 1000000));
          Serial.print(finalDrive);
        }

        CallibrationMode = 0;
      }
      
      break;

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

char recvData(char* data, byte *pos) {
  byte itr = *pos;

  while(Serial.available() > 0) {
    data[itr] = Serial.read();

    /* 
     * if uppercase check if valid.
     * uppercase must be the first character of the line, be R from wheel size,
     * or immediately follow a colon (:) [S and P]. If none of these conditions
     * are true, assume the letter is the first character and adjust accordingly
     */
    if(isupper(data[itr])) {

      if(itr == 0) {
        goto readNext;

      } else if(data[itr] == 'R') {
      	goto readNext;

      } else if(data[itr - 1] != ':') {
        data[0] = data[itr];
        itr = 1;
        continue;
      }
    }

    // if ending, reset itr and check string. if valid return. else reset to front
    if(data[itr] == '\0' || data[itr] == '\r' || data[itr] == '\n') {
      
      if(isupper(data[0]) && data[1] == ':' && itr > 2) {
        data[itr] = '\0';
        *pos = itr = 0;
        return data[0];
      }
      
      *pos = itr = 0;
    }

    readNext:
      itr++;
  }

  *pos = itr;
  return '\0';
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
