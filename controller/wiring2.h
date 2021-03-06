#ifndef Wiring2_H
#define Wiring2_H

#include "wiring_private.h"

#define MAX_RECORD 16

extern const byte encoderIntCount;

extern volatile byte encoderCtr[4];
extern volatile byte speedCtr[MAX_RECORD];
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
