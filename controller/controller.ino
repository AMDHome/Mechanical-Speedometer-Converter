volatile unsigned short SPHr = 0;       // Speed Per Hour * 10 (Unit friendly, can use both mph and kph)
volatile unsigned short targetRPM = 0;  // RPM * 10 (ex. 543.5RPM will be stored at 5435)

unsigned long inRatio;
unsigned long outRatio;

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
  outRatio = 1.4;

  // Configure PWM (Count Up, Fast PWM 10-bit, CLK/64)
  TCCR1A = _BV(COM1A1) | _BV(COM1B1) | _BV(WGM11) | _BV(WGM10);
  TCCR1B = _BV(WGM12) | _BV(CS11) | _BV(CS10);

  // Enable HW Interrupts: INT1 Rising Interrupt
  EIMSK = _BV(INT1);
  EICRA = _BV(ISC11) | _BV(ISC10);

  // No Load Operating Values
  // Start-Up Min 205, Absolute Min: 0
  // Operating Range ~180 - 1023 (Lowest operating value may be lower)
  OCR1A = 0;
}

ISR(EXT_INT1_vect) {
  static unsigned long ptime = 0;
  static unsigned long ctime = 0;
  
  ctime = micros();

  // if counter overflow then just ignore, happens once every 70-ish minutes
  if(ctime < ptime)
    return;

  SPHr = inRatio / (ctime - ptime);
  targetRPM = SPHr * targetRPM;
}

void loop() {
  static char data[6];
  static char counter = 0;
  
  if(Serial.available()>0)
   {     
      data[counter] = Serial.read(); // reading the data received from the bluetooth module

      if(data[counter] == '\n'){
        data[counter] = '\0';
        counter = 0;
        
        int num = atoi(data);
        
        if(num > 1023){
          num = num % 1024;
        }
        
        if(num > 180){
          digitalWrite(LED_BUILTIN, HIGH);
          OCR1A = num;
        } else {
          digitalWrite(LED_BUILTIN, LOW);
          OCR1A = 0;
        }

        char p[6];
        itoa(num, p, 10);

        for(char i = 0; data[i] != '\0' && i < 6; i++) {
          Serial.print(p[i]);
        }
        Serial.println("");
        
      } else {
        counter++;
      }
   }

   delay(50);
}
