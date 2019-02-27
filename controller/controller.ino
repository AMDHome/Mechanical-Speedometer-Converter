volatile unsigned short SPHr = 0;       // Speed Per Hour * 10 (Unit friendly, can use both mph and kph)
volatile unsigned short targetRPM = 0;  // RPM * 10 (ex. 543.5RPM will be stored at 5435)

unsigned long inRatio;    // input ratio, Also happens to be dt for 0.1 SPH [dt > inRatio => SPHr = 0]
unsigned long outRatio;   // output ratio * 10,000,000 (to compensate for float)

volatile unsigned long comp_ptime = 0;

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
  Serial.println("~~~Starting Motor~~~");
  pinMode(9, OUTPUT);
  TCCR1A = _BV(COM1A1) | _BV(COM1B1) | _BV(WGM11) | _BV(WGM10);
  TCCR1B = _BV(WGM12) | _BV(CS11) | _BV(CS10);

  // No Load Operating Values
  // Start-Up Min 205, Absolute Min: 0
  // Operating Range ~180 - 1023 (Lowest operating value may be lower)
  OCR1A = 0;
}

ISR(EXT_INT1_vect) {
  static unsigned long comp_ptime = 0;
  static unsigned long ctime = 0;
  
  ctime = micros();

  // if counter overflow then just ignore, happens once every 70-ish minutes
  if(ctime < comp_ptime)
    return;

  SPHr = inRatio / (ctime - comp_ptime);
  targetRPM = (long) SPHr * outRatio / 10000000;
}

void loop() {
  static short i = 205;

  // Slowly Rev Motor Up
  for(; i < 500; i+= 5) {
    OCR1A = i; // Range 0 - 1023
    Serial.println(i);
    delay(500);
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

   if(micros() - comp_ptime > inRatio) {
    SPHr = 0;
   }
}
