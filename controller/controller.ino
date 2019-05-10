/*  *** Important! ***
 *  This file has changed the default clock timer from timer 0 to timer 2.
 *  micros(), millis(), delay(), delayMicroseconds(), and any functions that
 *  rely on these functions will not work. I have provided identical functions
 *  named micros2(), millis2(), delay2(), and delayMicroseconds2(). Any function
 *  you want to use that also uses the default timing functions will need to be 
 *  rewritten to use my timing functions.
 */

#include <EEPROM.h>
#include "wiring2.h"

// define PWM limits
#define MAX 1023
#define MIN 0

// define PID constants
#define KP 125          // Proportional constant
#define KI 10            // Integral constant
#define KD 10           // Derivative constant
#define KFF 90          // Feed Forward constant
#define RPM_TO_PWM 100  // Rough conversion ratio of rpm numbers to pwm numbers

unsigned short SPHr = 0;       // Speed/Hour * 10 (Unit friendly, for mph & kph)
unsigned short targetRPM = 0;  // RPM * 10 (ex. 543.5RPM will be stored at 5435)

long oldErr = 0; // Previous error
long pid_p; // Proportional term
long pid_i = 0; // Integral term
long pid_d; // Derivative term
//long pid_ff; // Feed Forward term

unsigned long inRatio;    // Also == dt for 0.1 SPH [dt > inRatio => SPHr > 0.1]
unsigned long outRatio;   // output ratio * 10,000,000 (to compensate for float)
unsigned short maxSpeed;  // max speed our speedometer can show

volatile unsigned long tickCounter[2] = {0};  // # of ticks every ~0.13 seconds
volatile byte countENC = 0;                   // flag to tell which tickCounter to put new data into
volatile unsigned long currentRPM  = 0;
const byte encoderIntCount = 64;

volatile unsigned long pTime = 0;
volatile unsigned long eTime[4] = {0};
volatile bool updated = false;
volatile byte count = 0;
volatile bool fromStop = true;
byte numMag;
byte shift;

/*
 * Calculate inRatio via stored values.
 * finalDrive should be 1 if not being used
 * wheelCirc will have units of meter or milliMiles (miles * 1000)
 *           [odd units but necessary for easy calcs]
 */
void updateInRatio(byte numMag, float wheelCirc, float finalDrive) {
  // Hard coded number = 3,600,000,000 [microseconds/hr]
  //                     / 1000 [compensate for milliMiles & meters fom mi & km]
  //                     * 10 [compensate for SPHr/and targetRPM units]
  // Complicated I know, but floats on arduino are very Very VERY slow
  inRatio = (long) (finalDrive * wheelCirc * 36000000 / (float) numMag);
}

void setup() {
  float temp;

  // change clock to timer 2
  init2();
  
  Serial.begin(9600);
  Serial.println("~~~Starting Motor~~~");

  pinMode(4, INPUT_PULLUP);       // encoder input
  pinMode(9, OUTPUT);             // motor PWM output
  pinMode(LED_BUILTIN, OUTPUT);   // onboard LED output

  pTime = micros2();

  // read numbers in from EEPROM
  numMag = EEPROM.read(3);
  shift = numMag / 2;
  updateInRatio(numMag, EEPROM.get(12, temp), EEPROM.get(4, temp));
  outRatio = EEPROM.get(8, outRatio);

  // Calculate maxRPM for our speedometer
  EEPROM.get(1, maxSpeed);

  // Configure PWM (Count Up, Fast PWM 10-bit, CLK/64)
  TCCR1A = _BV(COM1A1) | _BV(COM1B1) | _BV(WGM11) | _BV(WGM10);
  TCCR1B = _BV(WGM12) | _BV(CS11) | _BV(CS10);

  // Enable Analog Comparator
  ADCSRB = 0;
  ACSR = _BV(ACI) | _BV(ACIE)| _BV(ACIS1);

  // Enable T0 External Clock Counter (Count Rising Edge)
  TCCR0A = _BV(WGM01);
  TCCR0B = _BV(CS02) | _BV(CS01) | _BV(CS00);   // remove CS00 for Falling Edge

  TCNT0 = 0;  // Reset Counter 0

  // No Load Operating Values
  // Start-Up Min 205, Absolute Min: 0
  // Operating Range ~100 - 1023 (Lowest operating value may be lower)
  OCR1A = 0;

  Serial.println("~~~Starting~~~");
  Serial.print("inRatio: ");
  Serial.println(inRatio);
  Serial.print("outRatio: ");
  Serial.println(outRatio);
}

ISR(ANALOG_COMP_vect) {
  static unsigned long currTime = 0;

  currTime = micros2();

  // if counter overflow then just ignore, happens once every 70-ish minutes
  if(currTime < pTime) {
    pTime = currTime;
    return;
  }

  // update time
  eTime[count] = currTime - pTime;
  pTime = currTime;

  if(numMag > 1) {
    if(fromStop && count + 1 == numMag) {
      fromStop = false;
    }
    count = (count + 1) % numMag;
  }
  
  updated = true;
}

void loop() {
  long error;
  long newPWM;
  long currentRPM = 0; // Feedback from slotted wheel on motor shaft
  //long revolutions = 0; // Number of 360 degree rotations

  if(checkBT()){
    delay2(10);
  }

  if(micros2() - pTime > inRatio / 20) { // We have come to rest; stop the motor
    SPHr = 0;
    targetRPM = 0;
    OCR1A = 0;
    tickCounter[0] = tickCounter[1] = 0;
    fromStop = true;
    count = 0;
  } else {

    if(updated){
      SPHr = (unsigned long) inRatio / calcTime();
      targetRPM = (unsigned long) (((long) SPHr) * (outRatio / 1000)) / 1000;
      updated = false;
    }
  
    // RPM based on number of ticks passed
    // see Technical Manual pg. 16 for explanation of numbers
    currentRPM = ((46875 * (tickCounter[0] + tickCounter[1]) / 2) / 1024) * 10;

    if(SPHr > 0 && currentRPM <= 10) {
      OCR1A = 400;
    } 
    
    /*
     * PID Implementation. Divisions by hard coded 100 reflect that kp, ki, kd, and kff
     * are larger by a factor of 100 to avoid floats. These may require additional tuning.
     */
  
     error = targetRPM - currentRPM;
     //pid_ff = KFF * targetRPM / 100;
     pid_p = KP * error / 100;
     pid_i += KI * error / 100;
     pid_d = KD * (error - oldErr) / 100;
     newPWM = (pid_p + pid_i + pid_d) / RPM_TO_PWM;
  
     if (newPWM > MAX)
       OCR1A = MAX;
     else if (newPWM < MIN)
       OCR1A = MIN;
     else 
       OCR1A = newPWM;
     oldErr = error;
  }

  // debug printout
  currentRPM = ((46875 * (tickCounter[0] + tickCounter[1]) / 2) / 1024) * 10;
  
  Serial.print("Current: ");
  Serial.print(currentRPM/10);
  Serial.print("\tw/ PWM: ");
  Serial.print(OCR1A);
  Serial.print("\tTarget: ");
  Serial.print(targetRPM/10);
  Serial.print("\tSPHr: ");
  Serial.println(SPHr/10);
}

unsigned long calcTime() {
  unsigned long avgTime = 0;

  if(numMag == 1) {
    return eTime[0];
  
  } else if(fromStop) {
    for(byte i = 0; i < count; i++){
      avgTime += eTime[i] / count;
    }
    
  } else {
    for(byte i = 0; i < numMag; i++){
      avgTime += (eTime[i] >> shift);
    }
  }
  
  return avgTime;
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
      maxSpeed = (short) atol(data);
      EEPROM.put(1, maxSpeed);
      updated = true;
      break;

    case 'N': // Store Number of Magnets
      EEPROM.update(3, data[0] - '0');
      updateInRatio(EEPROM.read(3), EEPROM.get(12, temp), EEPROM.get(4, temp));
      updated = true;
      break;

    case 'F': // Store Final Drive Ratio
      EEPROM.put(4, ((float) atol(data)) / 1000000);
      updateInRatio(EEPROM.read(3), EEPROM.get(12, temp), EEPROM.get(4, temp));
      updated = true;
      break;

    case 'S': // Store Speedometer Ratio
      outRatio = atol(data);
      EEPROM.put(8, outRatio);
      updated = true;
      break;

    case 'W': // Store Wheel Circumference
      EEPROM.put(12, ((float) atol(data)) / 1000000);
      updateInRatio(EEPROM.read(3), EEPROM.get(12, temp), EEPROM.get(4, temp));
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
  byte itr;

  if(Serial.available() > 1) {
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

    for(itr = 0; itr < 13 && (Serial.available() > 0); itr++) {
      data[itr] = Serial.read();
      delay(1);
    }

    if(itr == 13 && data[10] != '\0') {
      digitalWrite(LED_BUILTIN, HIGH);
      return '\0';
    }

    digitalWrite(LED_BUILTIN, LOW);
  }
  
  return type;
}
