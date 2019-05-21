#ifndef Wiring2_H
#define Wiring2_H

#include "wiring_private.h"

extern const byte encoderIntCount;

extern volatile byte encoderCtr[2];
extern volatile byte speedCtr[32];
extern volatile byte currSpeedCtr;
extern volatile byte counterIndex;

extern byte CallibrationMode;
extern volatile byte rTime;

unsigned long millis2();
unsigned long micros2();
void delay2(unsigned long ms);
void delayMicroseconds2(unsigned int us);
void init2();

#endif
