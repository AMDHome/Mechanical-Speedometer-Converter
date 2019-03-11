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
#define MAX 1023
#define MIN 0

volatile unsigned short SPHr = 0;       // Speed Per Hour * 10 (Unit friendly, can use both mph and kph)
volatile unsigned short targetRPM = 0;  // RPM * 10 (ex. 543.5RPM will be stored at 5435)

const unsigned long kp = 31; // Proportional constant
const unsigned long ki = 18;  // Integral constant
const unsigned long kd = 22; // Derivative constant
const unsigned long kff = 90; // Feed Forward constant
const unsigned long rpmToPwm = 100; // Rough conversion ratio of rpm numbers to pwm numbers
volatile unsigned long oldErr = 0; // Previous error
volatile unsigned long pid_p; // Proportional term
volatile unsigned long pid_i = 0; // Integral term
volatile unsigned long pid_d; // Derivative term
volatile unsigned long pid_ff; // Feed Forward term
volatile unsigned long currentRPM = 0; // Feedback from slotted wheel on motor shaft
volatile unsigned long revolutions = 0; // Number of 360 degree rotations
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
  float temp;

  // change clock to timer 2
  intit2();
  Serial.begin(9600);
  pinMode(9, OUTPUT);
  pinMode(LED_BUILTIN, OUTPUT);

  // change hardCoded numbers to be read in from EEPROM
  updateInputRatio(EEPROM.read(3), EEPROM.get(12, temp), EEPROM.get(4, temp));
  outRatio = EEPROM.get(8, outRatio);
  EEPROM.get(1, maxSpeed);

  // Configure PWM (Count Up, Fast PWM 10-bit, CLK/64)
  TCCR1A = _BV(COM1A1) | _BV(COM1B1) | _BV(WGM11) | _BV(WGM10);
  TCCR1B = _BV(WGM12) | _BV(CS11) | _BV(CS10);

  // Enable Analog Comparator
  ADCSRB = 0;
  ACSR = _BV(ACI) | _BV(ACIE);

  // Enable T0 External Clock Counter (Count Rising Edge)
  TCCR0A = _BV(WGM01);
  TCCR0B = _BV(CS02) | _BV(CS01) | _BV(CS00);   // remove CS00 for Falling Edge

  TCNT0 = 0;  // Reset Counter 0
  OCR0A = 20;  // Set compare value (number of holes that pass before interrupt is triggered

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

  currTime = micros2();

  // if counter overflow then just ignore, happens once every 70-ish minutes
  if(currTime < prevTime || skipNext) {
    if(cycle % 2)
      skipNext = true;

    else if(skipNext)
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

/* Every 100ms (can be changed), a measurement of current motor RPM is taken. At this time, the error
 * between targetRPM and currentRPM is used to make a change to OCR1A via PID. This process is repsonsible
 * for all PWM control except entering and exiting rest, since at rest this interrupt will not run.
 */
ISR(TIMER0_COMPA_vect) {
  static unsigned long current_time = 0;
  static unsigned long last_time = 0;
  unsigned long elapsed;
  unsigned long error;
  unsigned long newPWM;

  current_time = micros2();

  // As before, if timer overflow occurs, do nothing on that interrupt instance
  if(last_time > current_time) {
    last_time = current_time;
    return;
  }

  revolutions++; //Interrupt has occured, so 20 slots have been past

  if (current_time - last_time < 100000) //In addition, if > 100ms has elapsed, compute currentRPM
    return;

  elapsed = current_time - last_time;
  /* Math is revolutions*10 to match targetRPM format, divided by elapsed to get
   * revs per microsecond, times one million micros/sec to get revs per second,
   * times 60 to get rpm. But a different order is used to save division for the end.
   */
  currentRPM = (revolutions*10*1000000*60)/elapsed;
  /* PID Implementation. Divisions by hard coded 100 reflect that kp, ki, kd, and kff
   * are larger by a factor of 100 to avoid floats. These may require additional tuning.
   */
  error = targetRPM - currentRPM;
  pid_ff = kff*targetRPM/100;
  pid_p = kp*error/100;
  pid_i += ki*error/100;
  pid_d = kd*(error - oldErr)/100;
  newPWM = (pid_p + pid_i + pid_d + pid_ff)/rpmToPwm;
  if (newPWM > MAX)
    OCR1A = MAX;
  else if (newPWM < MIN)
    OCR1A = MIN;
  else
    OCR1A = newPWM;
  oldErr = error;
  revolutions = 0;
  last_time = current_time;
}

void loop() {

  checkBT();
  delay2(150);

  if(micros2() - pTime > inRatio) { //We have come to rest; stop the motor
    SPHr = 0;
    targetRPM = 0;
    OCR1A = 0;
    currentRPM = 0;
  }
  if(SPHr != 0 && currentRPM == 0) {
    OCR1A = 250;
  }
}

void checkBT() {
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
      // Update inRatio
      break;

    case '\0':
    default:
      break;
  }
  if(updated) {
    digitalWrite(LED_BUILTIN, HIGH);
    for(byte i = 0; data[i] != '\0' && i < 13; i++) {
      Serial.print(data[i]);
    }
    Serial.println("");
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
