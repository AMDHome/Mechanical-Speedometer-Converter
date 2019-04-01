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
#define MAX 320
#define MIN 50

volatile unsigned short SPHr = 0;       // Speed Per Hour * 10 (Unit friendly, can use both mph and kph)
volatile unsigned short targetRPM = 20000;  // RPM * 10 (ex. 543.5RPM will be stored at 5435)

const long kp = 41; // Proportional constant
const long ki = 1;  // Integral constant
const long kd = 6; // Derivative constant
const long kff = 90; // Feed Forward constant
const long rpmToPwm = 100; // Rough conversion ratio of rpm numbers to pwm numbers
volatile long oldErr = 0; // Previous error
volatile long pid_p; // Proportional term
volatile long pid_i = 0; // Integral term
volatile long pid_d; // Derivative term
volatile long pid_ff; // Feed Forward term
volatile long currentRPM = 0; // Feedback from slotted wheel on motor shaft
volatile long revolutions = 0; // Number of 360 degree rotations
volatile long pTime = 0;
volatile long elapsed;
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
  float temp;

  // change clock to timer 2
  init2();
  Serial.begin(9600);
  Serial.println("~~~Starting Motor~~~");
  pinMode(4, INPUT_PULLUP);
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

  TCCR0A = _BV(WGM01);
  TCCR0B = _BV(CS02) | _BV(CS01) | _BV(CS00);   // remove CS00 for Falling Edge
  TIMSK0 = _BV(OCIE0A);

  TCNT0 = 0;  // Reset Counter 0
  OCR0A = 20;  // Set compare value (number of holes that pass before interrupt is triggered


  // No Load Operating Values
  // Start-Up Min 205, Absolute Min: 0
  // Operating Range ~180 - 1023 (Lowest operating value may be lower)
  OCR1A = 200;
}

ISR(TIMER0_COMPA_vect) {
  static unsigned long current_time = 0;
  static unsigned long last_time = 0;

  current_time = micros2();

  if(last_time > current_time) {
    last_time = current_time;
    return;
  }

  elapsed = current_time - last_time;
  last_time = current_time;
}

void loop() {
  long error;
  long newPWM;
  long duration;
  static long present = 0;
  static long past = 0;

  present = micros2();

  if(past > present) {
    past = present;
    return;
  }

  duration = present - past;
  // Every ten seconds change targetRPM and see how pid handles it
  if(duration >= 5000000 && targetRPM >= 3800) {
    //targetRPM = random(10000, 70000); //random rpm between 1000.0 and 10,000.0
    targetRPM -= 1800;
    past = present;
  }

  /* Math is one revolution*10 to match targetRPM format, divided by elapsed to get
   * revs per microsecond, times one million micros/sec to get revs per second,
   * times 60 to get rpm. But a different order is used to save division for the end.
   */
  currentRPM = (1*10*1000000*60)/elapsed;
  Serial.print("Current: ");
  Serial.print(currentRPM/10);
  Serial.print(" w/ PWM: "); 
  Serial.print(OCR1A); 
  Serial.print("  Target: ");
  Serial.println(targetRPM/10);
  /* PID Implementation. Divisions by hard coded 100 reflect that kp, ki, kd, and kff
   * are larger by a factor of 100 to avoid floats. These may require additional tuning.
   */
   error = targetRPM - currentRPM;
   //pid_ff = kff*targetRPM/100;
   pid_p = kp*error/100;
   pid_i += ki*error/100;
   pid_d = kd*(error - oldErr)/100;
   newPWM = (pid_p + pid_i + pid_d)/rpmToPwm;
   if (newPWM > MAX)
     OCR1A = MAX   else if (newPWM < MIN)
     OCR1A = MIN;
   else
     OCR1A = newPWM;
   oldErr = error;
}
