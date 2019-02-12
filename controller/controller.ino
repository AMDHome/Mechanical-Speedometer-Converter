void setup() {
  pinMode(9, OUTPUT);
  TCCR1A = _BV(COM1A1) | _BV(COM1B1) | _BV(WGM11) | _BV(WGM10);
  TCCR1B = _BV(WGM12) | _BV(CS11) | _BV(CS10);
  OCR1A = 1023;
}

void loop() {
  
  for(int i = 1; i < 1023; i++) {
    OCR1A = i; // Range 0 - 1023
    delay(5);
  }
}
