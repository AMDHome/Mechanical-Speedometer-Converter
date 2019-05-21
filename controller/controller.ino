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
/*
#define KP 125          // Proportional constant
#define KI 10           // Integral constant
#define KD 10           // Derivative constant
#define KFF 90          // Feed Forward constant
*/
#define RPM_TO_PWM 100  // Rough conversion ratio of rpm numbers to pwm numbers

#define MAX_RECORD 32


long KP = 125;
long KI = 10;
long KD = 10;

void loadVariables();

unsigned short SPHr = 0;       // Speed/Hour * 10 (Unit friendly, for mph & kph)
unsigned short targetRPM = 0;  // RPM * 10 (ex. 543.5RPM will be stored at 5435)

// calibration variables
byte CallibrationMode = 0;
volatile unsigned short calTicks;
extern volatile byte rTime;

long oldErr = 0; // Previous error
long pid_p; // Proportional term
long pid_i = 0; // Integral term
long pid_d; // Derivative term
//long pid_ff; // Feed Forward term

unsigned long inRatio;    // Also == dt for 0.1 SPH [dt > inRatio => SPHr > 0.1]
unsigned long outRatio;   // output ratio * 10,000,000 (to compensate for float)
unsigned short maxSpeed;  // max speed our speedometer can show

volatile byte encoderCtr[2] = {0};  // # of ticks every ~0.13 seconds
volatile byte speedCtr[MAX_RECORD] = {0};  // # of ticks every ~0.13 seconds
volatile byte currSpeedCtr;
volatile byte counterIndex = 0;                   // flag to tell which tickCounter to put new data into

volatile unsigned long currentRPM  = 0;
const byte encoderIntCount = 64;

//volatile unsigned long pTime = 0;
//volatile unsigned long eTime[4] = {0};
//volatile bool updated = false;
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
void updateInRatio(byte numMag, long wheelCirc, float finalDrive) {
  // 16 for limit adjustment
  inRatio = ((long) (finalDrive * wheelCirc / numMag)) >> 4;
}

void setup() {
  // change clock to timer 2
  init2();
  
  Serial.begin(9600);

  pinMode(4, INPUT_PULLUP);       // encoder input
  pinMode(9, OUTPUT);             // motor PWM output
  pinMode(LED_BUILTIN, OUTPUT);   // onboard LED output

  // Read numbers in from EEPROM and check for valid input
  // if no value isnt valid then set default value
  loadVariables();

  Serial.println(inRatio);
  Serial.println(outRatio);
  Serial.println(maxSpeed);
  Serial.println(numMag);
  Serial.println(shift);

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

  Serial.println("~~~Starting~~~");
  Serial.print("inRatio: ");
  Serial.println(inRatio);
  Serial.print("outRatio: ");
  Serial.println(outRatio);
}

void loop() {
  long error;
  long newPWM;
  long currentRPM = 0; // Feedback from slotted wheel on motor shaft

  checkBT();
  
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
    currentRPM = ((46875 * (encoderCtr[0] + encoderCtr[1]) / 2) / 1024) * 10;

    if(SPHr > 0 && currentRPM <= 10) {
      OCR1A = 400;
    } 
    
    /*
     * PID Implementation. Divisions by hard coded 100 reflect that kp, ki, kd, and kff
     * are larger by a factor of 100 to avoid floats. These may require additional tuning.
     */

    error = targetRPM - currentRPM;
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
  currentRPM = ((46875 * (encoderCtr[0] + encoderCtr[1]) / 2) / 1024) * 10;
  
  
  Serial.print("Current: ");
  Serial.print(currentRPM/10);
  Serial.print("\tw/ PWM: ");
  Serial.print(OCR1A);
  Serial.print("\tTarget: ");
  Serial.print(targetRPM/10);
  Serial.print("\tSPHr: ");
  Serial.println(SPHr/10);
  
}

unsigned short calcSpeed() {
  unsigned long totTicks = 0;
  byte readings = 32;
  bool leadingZeros = true;
  byte cIndex = counterIndex;

  for(byte i = 0; i < MAX_RECORD; i++) {    

    if(speedCtr[(cIndex + i) % MAX_RECORD] == 0 && leadingZeros) {
      readings--;

    } else {
      leadingZeros = false;
      totTicks += speedCtr[(cIndex + i) % MAX_RECORD];
    }
  }

  if(readings == 0){
    return 0;
  } else {
    return (unsigned short) ((inRatio * totTicks * 9) / ((unsigned long) readings * 1024));
  }
}

ISR(ANALOG_COMP_vect) {
  currSpeedCtr++;
  if(CallibrationMode == 2 && rTime) {
    calTicks++;
  }
}

void loadVariables() {

  long templ;
  float temp;

  // check NumMags
  numMag = EEPROM.read(3);
  if(numMag == 0){
    numMag = 1;
    EEPROM.update(3, numMag);
  }

  shift = numMag / 2;

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
  Serial.println(numMag);
  Serial.println(EEPROM.get(12, temp));
  Serial.println(EEPROM.get(4, temp));

  if(EEPROM.get(8, outRatio) == 0) {
    outRatio = 10000000;
    EEPROM.put(8, outRatio);
  }

  if(EEPROM.get(1, maxSpeed) == 0) {
    maxSpeed = 60;
    EEPROM.put(1, maxSpeed);
  }
}
