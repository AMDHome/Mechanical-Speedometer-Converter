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

void loop() {
  static short i = 205;

  // Slowly Rev Motor Up
  for(; i < 500; i+= 5) {
    OCR1A = i; // Range 0 - 1023
    Serial.println(i);
    delay(500);
  }

  // Quickly Decelerate Motor
  for(; i > 180; i--) {
    OCR1A = i; // Range 0 - 1023
    delay(10);
  }
}
