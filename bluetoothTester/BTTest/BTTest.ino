#include <EEPROM.h>

unsigned long inRatio;    // input ratio, Also happens to be dt for 0.1 SPH [dt > inRatio => SPHr = 0]
unsigned long outRatio;   // output ratio * 10,000,000 (to compensate for float)
unsigned short maxSpeed;  // max speed our speedometer can show

/*
 * Calculate inRatio via stored values.
 * finalDrive should be 1 if not being used
 * wheelCirc will have units of meter or milliMiles (miles * 1000) [odd units but necessary for easy calcs]
 */
void updateInputRatio(char numMag, float wheelCirc, float finalDrive) {
  // Hard coded number = 3,600,000,000 [microseconds/hr] / 1000 [compensate for milliMiles/meters]
  //                     * 10 [compensate for SPHr/and targetRPM units]
  // Complicated I know, but floats on arduino are very Very VERY slow
  inRatio = (long) (finalDrive * wheelCirc * 36000000 / (float) numMag);
}

void setup() {
  
  Serial.begin(9600);
}

void loop() {

  if(checkBT()){
    delay(150);
  }
}

bool checkBT() {
  static char data[13];
  bool updated = false;
  float temp;

  // check and recieve data
  switch(recvData(data)) {
    Serial.print(recvData(data));
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
