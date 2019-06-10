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
#include "BTComms.h"

// define PWM limits
#define MAX 1023
#define MIN 0

// define PID constants
#define RPM_TO_PWM 12  // Rough conversion ratio of rpm numbers to pwm numbers
long KP = 11;
long KI = 9;
long KD = 0;

void loadVariables();

unsigned short targetRPM = 0;  // RPM * 10 (ex. 543.5RPM will be stored at 5435)

bool debug = false;

// calibration variables
byte CallibrationMode = 0;
volatile unsigned short calTicks;
extern volatile byte rTime;

long oldErr = 0;  // Previous error
long pid_p;       // Proportional term
long pid_i = 0;   // Integral term
long pid_d;       // Derivative term

unsigned long inRatio;
unsigned long outRatio;   // output ratio * 10,000,000 (to compensate for float)
unsigned short maxSpeed;  // max speed our speedometer can show * 10
unsigned short minSpeed;  // min speed of our speedometer (unimplemented)

volatile byte encoderCtr[4] = {0};          // # of ticks every ~0.13 seconds on encoder
volatile byte speedCtr[MAX_RECORD] = {0};   // # of ticks every ~0.13 seconds on speed sensor
volatile byte currSpeedCtr;
volatile byte counterIndex = 0;             // flag to tell which tickCounter to put new data into

const byte encoderIntCount = 64;
unsigned long itrTime;


/*
 * Calculate inRatio via stored values.
 * finalDrive should be 1 if not being used
 * wheelCirc will have units of meter or milliMiles (miles * 1000)
 *           [odd units but necessary for easy calcs]
 */
void updateInRatio(byte numMag, long wheelCirc, float finalDrive) {
  // 16 for limit adjustment
  inRatio = ((long) (wheelCirc / (finalDrive * numMag))) >> 4;
}


void setup() {
  // change clock to timer 2
  init2();
  
  Serial.begin(9600);

  pinMode(4, INPUT_PULLUP);       // encoder input
  pinMode(9, OUTPUT);             // motor PWM output

  // Read numbers in from EEPROM and check for valid input
  // if no value isnt valid then set default value
  loadVariables();
  minSpeed = (maxSpeed * 0.1 > 100) ? 100 : maxSpeed * 0.1;

  // Configure PWM (Count Up, Fast PWM 10-bit, 16kHz [15,625 Hz])
  TCCR1A = _BV(COM1A1) | _BV(COM1B1) | _BV(WGM11) | _BV(WGM10);
  TCCR1B = _BV(WGM12) | _BV(CS10);

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
  itrTime = millis2() + 5;
  Serial.println(itrTime);
}


void loop() {
  unsigned short SPHr;      // Speed/Hour * 10 (Unit friendly, for mph & kph)
  long currentRPM;          // Feedback from slotted wheel on motor shaft
  unsigned long currTime;

  long error;
  long newPWM;

  // if there is an incomming signal, go read it
  if(Serial.available() > 0){
    checkBT();
  }
  
  SPHr = calcSpeed();
  
  // limit max speed shown
  if(SPHr > maxSpeed) {
    SPHr = maxSpeed;
  }

  if(!SPHr && !CallibrationMode) {
    targetRPM = 0;
    OCR1A = 0;

  } else {

    if(!CallibrationMode) {
      targetRPM = (((unsigned long) SPHr) * (outRatio / 1000)) / 1000;
    }

    // RPM based on number of ticks passed
    // see Technical Manual pg. 16 for explanation of numbers
    currentRPM = ((46875 * (encoderCtr[0] + encoderCtr[1] + encoderCtr[2] + encoderCtr[3]) / 4) / 1024) * 10;
    
    /*
     * PID Implementation. Divisions by hard coded 100 reflect that kp, ki, kd, and kff
     * are larger by a factor of 100 to avoid floats.
     */
    error = targetRPM - currentRPM;
    pid_p = KP * error / 1000;
    pid_i += KI * error / 10000;
    pid_d = KD * (error - oldErr) / 10000;
    newPWM = (pid_p + pid_i + pid_d) / RPM_TO_PWM;

    if (newPWM > MAX)
      OCR1A = MAX;
    else if (newPWM < MIN)
      OCR1A = MIN;
    else if (newPWM > OCR1A + 100)
      OCR1A += 256;
    else
      OCR1A = newPWM;
    oldErr = error;
  }
  
  if(debug) {
    Serial.print("Current: ");
    Serial.print(currentRPM/10);
    Serial.print("\tw/ PWM: ");
    Serial.print(OCR1A);
    Serial.print("\tTarget: ");
    Serial.print(targetRPM/10);
    Serial.print("\tSPHr: ");
    Serial.println(SPHr/10);
  }

  if(itrTime - millis2() <= 5){
    delay2(1 + itrTime - millis2());
  } else {
    if(debug) {
      Serial.print("T: ");
      Serial.print(itrTime);
      Serial.print("/tI: ");
      Serial.println(millis2());
    }
    itrTime = millis2();
  }

  itrTime += 5;
}


unsigned short calcSpeed() {

  static unsigned short oldSPH;
  unsigned long totTicks = 0;
  unsigned long newSPH;

  for(byte i = 0; i < MAX_RECORD; i++) {
    totTicks += speedCtr[i];
  }

  newSPH = ((inRatio * totTicks * 9) / ((unsigned long) MAX_RECORD * 1024));
  oldSPH = (oldSPH * 2 / 10) + ((unsigned short) (newSPH * 8 / 10));
  return oldSPH;
}


ISR(ANALOG_COMP_vect) {
  currSpeedCtr++;
  if(CallibrationMode == 2 && rTime) {
    calTicks++;
  }
}


void loadVariables() {
  byte numMag;
  long templ;
  float temp;

  // check NumMags
  numMag = EEPROM.read(3);
  if(numMag == 0){
    numMag = 1;
    EEPROM.update(3, numMag);
  }

  // check final drive
  if(EEPROM.get(4, templ) == 0) {
    EEPROM.put(4, 1.0);
  }

  // check Tire Size/Circ
  if(EEPROM.get(12, templ) == 0) {
    char defaultTire[11] = "P205/65R15";
    EEPROM.put(12, 1263341);
    EEPROM.put(16, defaultTire);
  }

  updateInRatio(numMag, templ, EEPROM.get(4, temp));

  if(EEPROM.get(8, outRatio) == 0) {
    outRatio = 10000000;
    EEPROM.put(8, outRatio);
  }

  if(EEPROM.get(1, maxSpeed) == 0) {
    maxSpeed = 60;
    EEPROM.put(1, maxSpeed);
  }
  maxSpeed *= 10;
}
