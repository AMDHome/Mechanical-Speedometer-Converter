#include <EEPROM.h>

volatile unsigned short SPHr = 0;       // Speed Per Hour * 10 (Unit friendly, can use both mph and kph)
volatile unsigned short targetRPM = 0;  // RPM * 10 (ex. 543.5RPM will be stored at 5435)

unsigned long inRatio;    // input ratio, Also happens to be dt for 0.1 SPH [dt > inRatio => SPHr = 0]
unsigned long outRatio;   // output ratio * 10,000,000 (to compensate for float)
unsigned short maxSpeed;  // max speed our speedometer can show

volatile unsigned long pTime = 0;

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
  pinMode(9, OUTPUT);
  pinMode(LED_BUILTIN, OUTPUT);

  // change hardCoded numbers to be read in from EEPROM
  updateInputRatio(4, 1.04829, 1.0);
  outRatio = 1.4 * 1000000;
  maxSpeed = 160;

  // Configure PWM (Count Up, Fast PWM 10-bit, CLK/64)
  TCCR1A = _BV(COM1A1) | _BV(COM1B1) | _BV(WGM11) | _BV(WGM10);
  TCCR1B = _BV(WGM12) | _BV(CS11) | _BV(CS10);

  // Enable Analog Comparator
  ADCSRB = 0;
  ACSR = _BV(ACI) | _BV(ACIE);

  // Enable HW Interrupts: INT0 Rising Interrupt 
  EIMSK = _BV(INT0); 
  EICRA = _BV(ISC01) | _BV(ISC00); 

  // No Load Operating Values
  // Start-Up Min 205, Absolute Min: 0
  // Operating Range ~180 - 1023 (Lowest operating value may be lower)
  OCR1A = 0;
}

ISR(ANALOG_COMP_vect) {
  static unsigned long prevTime = 0;
  static unsigned long currTime = 0;
  static unsigned long elapsedTime = -1;
  static byte cycle = 1;
  static bool skipNext = false;
  
  currTime = micros();

  // if counter overflow then just ignore, happens once every 70-ish minutes
  if(currTime < prevTime || skipNext) {
    if(cycle % 2)
      skipNext = true;
      
    if(skipNext)
      skipNext = false;
      
    cycle += 1;
    pTime = currTime;
    return;
  }

  // There are two pulses of time we need to add to get total time between magnets.
  // Do math on second pulse
  if(currTime - prevTime > 2000) {
    if(cycle % 2) {
      elapsedTime = currTime - prevTime;
      
    } else {
      elapsedTime += currTime - prevTime;
      pTime = currTime;
      SPHr = (unsigned long) inRatio / elapsedTime;
      targetRPM = (unsigned long) SPHr * outRatio / 1000000;
    }

    prevTime = currTime;
    cycle += 1;
  }  
}

ISR(INT0_vect) {

}

void loop() {

  checkBT();

  delay(100);

  if(micros() - pTime > inRatio) {
    SPHr = 0;
    targetRPM = 0;
    OCR1A = 0;
  }
}

void checkBT() {
  static char data[13];

  // check and recieve data
  switch(recvData(data)) {
    case 'U': // Store Units
      EEPROM.update(0, data[0] - '0');
      break;

    case 'M': // Store Max Speed
      maxSpeed = (short) atoi(data);
      EEPROM.put(1, maxSpeed);
      break;
      
    case 'N': // Store Number of Magnets
      EEPROM.update(3, data[0] - '0');
      // Update inRatio
      break;

    case 'F': // Store Final Drive Ratio
      EEPROM.put(4, ((float) atoi(data)) / 1000000);
      // Update inRatio
      break;

    case 'S': // Store Speedometer Ratio
      outRatio = ((float) atoi(data)) / 1000000;
      EEPROM.put(8, outRatio);
      break;

    case 'W': // Store Wheel Circumference
      EEPROM.put(12, ((float) atoi(data)) / 1000000);
      // Update inRatio
      break;

    case '\0':
    default:
      break;
  }
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
