/*
  wiring2.c - modified implementation of the original wiring.c Wiring API for 
  the ATmega328P. Part of Arduino - http://www.arduino.cc/

  *** Important! ***
  This file will change the clock to be on Timer 2. It will break any function
  uses millis(), delay(), micros(), and the like. You must modify your
  functions to use the functions defined in this page millis2(), delay2(),
  micros2(), etc.

  Note: Assumes ATmega328P running at 16 MHz
*/

#include "wiring_private.h"
#include "wiring2.h"

// the prescaler is set so that timer2 ticks every 64 clock cycles, and the
// the overflow handler is called every 256 ticks.
#define MICROSECONDS_PER_TIMER2_OVERFLOW (clockCyclesToMicroseconds(64 * 256))

// the whole number of milliseconds per timer2 overflow
#define MILLIS_INC (MICROSECONDS_PER_TIMER2_OVERFLOW / 1000)

// the fractional number of milliseconds per timer2 overflow. we shift right
// by three to fit these numbers into a byte. (for the clock speeds we care
// about - 8 and 16 MHz - this doesn't lose precision.)
#define FRACT_INC ((MICROSECONDS_PER_TIMER2_OVERFLOW % 1000) >> 3)
#define FRACT_MAX (1000 >> 3)

volatile unsigned long timer2_overflow_count = 0;
volatile unsigned long timer2_millis = 0;
volatile byte rpmTimer = 0;
static unsigned char timer2_fract = 0;

ISR(TIMER2_OVF_vect) {
  // copy these to local variables so they can be stored in registers
  // (volatile variables must be read from memory on every access)
  unsigned long m = timer2_millis;
  unsigned char f = timer2_fract;
  byte r = rpmTimer;

  m += MILLIS_INC;
  f += FRACT_INC;
  if (f >= FRACT_MAX) {
    f -= FRACT_MAX;
    m += 1;
  }

  timer2_fract = f;
  timer2_millis = m;
  timer2_overflow_count++;
  r++;

  if(r == encoderIntCount) {
    tickCounter[countENC % 2] = TCNT0;
    TCNT0 = 0;
    countENC++;
    rpmTimer = 0;

  } else {
    rpmTimer = r;
  }
}

unsigned long millis2() {
  unsigned long m;
  uint8_t oldSREG = SREG;

  // disable interrupts while we read timer0_millis or we might get an
  // inconsistent value (e.g. in the middle of a write to timer0_millis)
  cli();
  m = timer2_millis;
  SREG = oldSREG;

  return m;
}

unsigned long micros2() {
  unsigned long m;
  uint8_t oldSREG = SREG, t;
  
  cli();
  m = timer2_overflow_count;
  t = TCNT2;

  if ((TIFR2 & _BV(TOV2)) && (t < 255))
    m++;

  SREG = oldSREG;
  
  return ((m << 8) + t) * (64 / clockCyclesPerMicrosecond());
}

void delay2(unsigned long ms) {
  uint32_t start = micros2();

  while (ms > 0) {
    yield();
    while ( ms > 0 && (micros2() - start) >= 1000) {
      ms--;
      start += 1000;
    }
  }
}

/* Delay for the given number of microseconds.  Assumes a 1, 8, 12, 16, 20 or 24 MHz clock. */
void delayMicroseconds2(unsigned int us) {
  // call = 4 cycles + 2 to 4 cycles to init us(2 for constant delay, 4 for variable)

  // calling avrlib's delay_us() function with low values (e.g. 1 or
  // 2 microseconds) gives delays longer than desired.
  //delay_us(us);

  // for the 16 MHz clock on most Arduino boards

  // for a one-microsecond delay, simply return.  the overhead
  // of the function call takes 14 (16) cycles, which is 1us
  if (us <= 1) return; //  = 3 cycles, (4 when true)

  // the following loop takes 1/4 of a microsecond (4 cycles)
  // per iteration, so execute it four times for each microsecond of
  // delay requested.
  us <<= 2; // x4 us, = 4 cycles

  // account for the time taken in the preceeding commands.
  // we just burned 19 (21) cycles above, remove 5, (5*4=20)
  // us is at least 8 so we can substract 5
  us -= 5; // = 2 cycles,

  // busy wait
  __asm__ __volatile__ (
    "1: sbiw %0,1" "\n\t" // 2 cycles
    "brne 1b" : "=w" (us) : "0" (us) // 2 cycles
  );
  // return = 4 cycles
}

/* setup new clock */
void init2() {
  // on the ATmega328p, timer 2 is also used for fast hardware pwm
  // (using phase-correct PWM would mean that timer 2 overflowed half as often
  // resulting in different millis() behavior on the ATmega8 and ATmega168)
  /* In init()
      sbi(TCCR2A, WGM20);
    We need
      sbi(TCCR2A, WGM21);
      sbi(TCCR2A, WGM20);
  */
  sbi(TCCR2A, WGM21);

  // set timer 2 prescale factor to 64
  /* This is already done in init()
      sbi(TCCR2B, CS22);
  */

  // enable timer 2 overflow interrupt
  sbi(TIMSK2, TOIE2);

  /* timer 1 is already setup by init() ran in the hidden main function */

  // set timer 0 prescale factor to 64
  /* This is already done in init()
      sbi(TCCR0B, CS01);
      sbi(TCCR0B, CS00);
  */

  // configure timer 0 for phase correct pwm (8-bit)
  /* In init()
      sbi(TCCR0A, WGM01);
      sbi(TCCR0A, WGM00);
    We only need
      sbi(TCCR0A, WGM00);
  */
  cbi(TCCR0A, WGM01);

  /* in init()
      // enable timer 0 overflow interrupt
      sbi(TIMSK0, TOIE0);
    We need to disable it
  */
  cbi(TIMSK0, TOIE0);
}
