#include <EEPROM.h>
#define MAX 1023
#define MIN 0

volatile unsigned short SPHr = 0;       // Speed Per Hour * 10 (Unit friendly, can use both mph and kph)
volatile unsigned short targetRPM = 0;  // RPM * 10 (ex. 543.5RPM will be stored at 5435)

unsigned long kp = 30; // Proportional constant
unsigned long ki = 5;  // Integral constant
unsigned long kd = 20; // Derivative constant
unsigned long oldErr = 0; // Previous error
unsigned long pid_p; // Proportional term
unsigned long pid_i = 0; // Integral term
unsigned long pid_d; // Derivative term
unsigned long currentRPM = 0; // Feedback from slotted wheel on motor shaft
unsigned long quarterTurns = 0; // Number of 90 degree rotations
unsigned long slotCounter = 0; // Will count up to five, interrupt only acts every 5th slot encountered
unsigned long rpmToPwm = 100; // Rough conversion ratio of rpm numbers to pwm numbers
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
/* Every 10ms (can be changed), a measurement of current motor RPM is taken. At this time, the error
 * between targetRPM and currentRPM is used to make a change to OCR1A via PID. This process is repsonsible
 * for all PWM control except entering and exiting rest, since at rest this interrupt will not run.
 */
ISR(INT0_vect) {
  static unsigned long current_time = 0;
  static unsigned long last_time = 0;
  static unsigned long elapsed = -1;
  unsigned long error;
  unsigned long PID;
  unsigned long deltaPWM;

  current_time = micros();

  // As before, if timer overflow occurs, do nothing on that interrupt instance
  if(last_time > current_time) {
    last_time = current_time;
    return;
  }

  if (++slotCounter != 5)
    return;

  quarterTurns++; //Increment quarter turns every 5th slot
  slotCounter = 0;

  if (current_time - last_time < 10000) //In addition, if > 10ms has elapsed, compute currentRPM
    return;

  elapsed = current_time - last_time;
  /* Quarter turns*10 to match targetRPM format, divided by 4 to get revolutions,
   * divided by elapsed to get revolutions per microsecond, times one million micros/sec
   * to get revolutions per second, times 60 to get rpm.
   */
  currentRPM = (((quarterTurns*10)/4)/elapsed)*1000000*60;
  /* PID Implementation. Divisions by a hard coded 100 reflect that the PID coefficients
   * (kp, ki, kd) are larger by a factor of 100 to avoid float use. These may require
   * additional tuning.
   */
  error = targetRPM - currentRPM;
  pid_p = kp*error/100;
  pid_i += ki*error/100;
  pid_d = kd*((error - oldErr)/elapsed)/100;
  PID = pid_p + pid_i + pid_d;
  deltaPWM = PID/rpmToPwm;
  if (OCR1A + deltaPWM > MAX)
    OCR1A = MAX;
  else if (OCR1A + deltaPWM < MIN)
    OCR1A = MIN;
  else
    OCR1A += deltaPWM;
  oldErr = error;
  quarterTurns = 0;
  last_time = current_time;
}

void loop() {

  checkBT();

  if(micros() - pTime > inRatio) { //We have come to rest; stop the motor
    SPHr = 0;
    targetRPM = 0;
    OCR1A = 0;
    currentRPM = 0;
  }
  if(SPHr != 0 && currentRPM == 0) {
    OCR1A = 210;
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
  }

  digitalWrite(LED_BUILTIN, LOW);
  return type;
}
