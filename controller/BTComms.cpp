#include <EEPROM.h>
#include "wiring2.h"
#include "BTComms.h"

bool checkBT() {
  static char data[26];
  bool updated = false;
  float temp;
  long templ;
  char *temps;
  byte i, tempb;

  // check and recieve data
  switch(recvData(data)) {
    case 'U': // Store Units
      EEPROM.update(0, data[0] - '0');
      updated = true;
      break;

    case 'M': // Store Max Speed
      maxSpeed = (short) atol(data);
      EEPROM.put(1, maxSpeed);
      updated = true;
      break;
      
    case 'N': // Store Number of Magnets
      EEPROM.update(3, data[0] - '0');
      updateInRatio(EEPROM.read(3), EEPROM.get(12, templ), EEPROM.get(4, temp));
      updated = true;
      break;

    case 'F': // Store Final Drive Ratio
      EEPROM.put(4, ((float) atol(data)) / 1000000);
      updateInRatio(EEPROM.read(3), EEPROM.get(12, templ), EEPROM.get(4, temp));
      updated = true;
      break;

    case 'S': // Store Speedometer Ratio
      outRatio = atol(data);
      EEPROM.put(8, outRatio);
      updated = true;
      break;

    case 'W': // Store Tire Size & Circumference
      EEPROM.put(12, atol(strtok(data, ":")));

      temps = strtok(NULL, ":");
      for(i = 0; i < 14 && temps[i] != '\0'; i++) {
        EEPROM.update(16 + i, temps[i]);
      }
      EEPROM.update(16 + i, '\0');

      updateInRatio(EEPROM.read(3), EEPROM.get(12, templ), EEPROM.get(4, temp));
      updated = true;
      break;

    case 'P':
      SCalMode = data[0] - '0';
      break;

    case 'T':
      if(SCalMode) {
        targetRPM = atol(data);
      }
      break;

    case 'L':
      if(data[0] - '0') {
        dataDump();
      }
      break;

    case 'D':
      tempb = atol(data);

      if(tempb) {
        SRecMode = tempb;
        // set all recording data to 0
        // start recording data

      } else {
        // Do calculations, update ratios and send value off
        /*
          EEPROM.put(4, ((float) atol(data)) / 1000000);
          updateInRatio(EEPROM.read(3), EEPROM.get(12, temp), EEPROM.get(4, temp));
          Serial.print(EEPROM.get(4, temp), 9);
          updated = true;
        */
        Serial.print("F:10000000");
        SRecMode = 0;
      }

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
/*
  if(updated) {
    digitalWrite(LED_BUILTIN, HIGH);
    Serial.print("Data Payload: ");
    for(i = 0; data[i] != '\0' && i < 26; i++) {
      Serial.print(data[i]);
    }
    Serial.println("");
  }*/
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
