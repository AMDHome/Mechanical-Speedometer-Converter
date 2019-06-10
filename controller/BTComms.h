#ifndef BTComms_H
#define BTComms_H

// access global variables from main
extern unsigned short targetRPM;  // RPM * 10 (ex. 543.5RPM will be stored at 5435)

extern byte CallibrationMode;
extern bool debug;
extern volatile unsigned short calTicks;

extern unsigned long outRatio;   // output ratio * 10,000,000 (to compensate for float)
extern unsigned short maxSpeed;  // max speed our speedometer can show

extern const byte encoderIntCount;

extern long KP;
extern long KI;
extern long KD;

// forward declaration from main
void updateInRatio(byte numMag, long wheelCirc, float finalDrive);

void checkBT();
char recvData(char* data, byte *pos);
void dataDump();

#endif
