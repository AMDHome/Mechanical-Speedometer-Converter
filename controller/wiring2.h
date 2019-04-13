#ifndef Wiring2_h
#define Wiring2_h

extern const byte encoderIntCount;
extern volatile unsigned long tickCounter[2];
extern volatile byte countENC;

unsigned long millis2();
unsigned long micros2();
void delay2(unsigned long ms);
void delayMicroseconds2(unsigned int us);
void init2();

#endif
