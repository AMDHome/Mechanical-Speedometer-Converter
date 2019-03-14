#include <EEPROM.h>

void setup() {
  
  Serial.begin(9600);
}

void loop() {

  if(checkBT()){
    delay2(150);
  }
}

bool checkBT() {
  static char data[13];
  bool updated = false;
  float temp;

  // check and recieve data
  switch(recvData(data)) {
    case 'U': // Store Units
      EEPROM.update(0, data[0] - '0');
      updated = true;
      break;

    case 'M': // Store Max Speed
      maxSpeed = (short) atoi(data);
      EEPROM.put(1, maxSpeed);
      updated = true;
      break;
      
    case 'N': // Store Number of Magnets
      EEPROM.update(3, data[0] - '0');
      updateInputRatio(EEPROM.read(3), EEPROM.get(12, temp), EEPROM.get(4, temp));
      updated = true;
      break;

    case 'F': // Store Final Drive Ratio
      EEPROM.put(4, ((float) atoi(data)) / 1000000);
      updateInputRatio(EEPROM.read(3), EEPROM.get(12, temp), EEPROM.get(4, temp));
      updated = true;
      break;

    case 'S': // Store Speedometer Ratio
      outRatio = atoi(data);
      EEPROM.put(8, outRatio);
      updated = true;
      break;

    case 'W': // Store Wheel Circumference
      EEPROM.put(12, ((float) atoi(data)) / 1000000);
      updateInputRatio(EEPROM.read(3), EEPROM.get(12, temp), EEPROM.get(4, temp));
      updated = true;
      break;

    case '\0':
    default:
      break;
  }
/*
  if(updated) {
    digitalWrite(LED_BUILTIN, HIGH);
    for(byte i = 0; data[i] != '\0' && i < 13; i++) {
      Serial.print(data[i]);
    }
    Serial.println("");
  }*/
}

char recvData(char* data) {
  char type = '\0';
  byte itr = 0;

  if(Serial.available() > 5) {
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

    for(; itr < 13 && (Serial.available() > 0); itr++) {
      data[itr] = Serial.read();
    }

    if(itr == 13 && data[10] != '\0') {
      digitalWrite(LED_BUILTIN, HIGH);
      return '\0';
    }

    digitalWrite(LED_BUILTIN, LOW);
  }
  
  return type;
}
