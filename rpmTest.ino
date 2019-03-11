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

void setup() {
  float temp;

  // change clock to timer 2
  intit2();
  Serial.begin(9600);
  pinMode(9, OUTPUT);
  pinMode(LED_BUILTIN, OUTPUT);

  // Configure PWM (Count Up, Fast PWM 10-bit, CLK/64)
  TCCR1A = _BV(COM1A1) | _BV(COM1B1) | _BV(WGM11) | _BV(WGM10);
  TCCR1B = _BV(WGM12) | _BV(CS11) | _BV(CS10);

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
  Serial.print(currentRPM/10); 
  Serial.println(" RPM");
  revolutions = 0;
  last_time = current_time; 
}

void loop() {
  OCR1A = 200;
  /* Rev Motor Up
  for(short i = 150; i < 1000; i+= 5) {
    OCR1A = i; // Range 0 - 1023
    Serial.println(i);
    delay(100);
  }

  // Rev Motor Down 
  for(short j = 1000; j > 150; j-=5) {
    OCR1A = j; // Range 0 - 1023
    Serial.println(j);
    delay(100);
  }*/
}