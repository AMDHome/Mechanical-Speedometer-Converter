/*
 * Example using non-blocking mode to move until a switch is triggered.
 *
 * Copyright (C)2015-2017 Laurentiu Badea
 *
 * This file may be redistributed under the terms of the MIT license.
 * A copy of this license has been included with this distribution in the file LICENSE.
 */
#include <Arduino.h>

// this pin should connect to Ground when want to stop the motor
#define STOPPER_PIN 12
#define UP_PIN 10
#define DOWN_PIN 11

// Motor steps per revolution. Most steppers are 200 steps or 1.8 degrees/step
#define MOTOR_STEPS 200

// Microstepping mode. If you hardwired it to save pins, set to the same value here.
#define MICROSTEPS 1

#define DIR 3
#define STEP 2
#define SLEEP 4 // optional (just delete SLEEP from everywhere if not used)

/*
 * Choose one of the sections below that match your board
 */

#include "DRV8825.h"
DRV8825 stepper(MOTOR_STEPS, DIR, STEP, SLEEP);
int RPM = 60;

// #include "BasicStepperDriver.h" // generic
// BasicStepperDriver stepper(DIR, STEP);

void setup() {
    Serial.begin(115200);

    // Configure stopper pin to read HIGH unless grounded
    pinMode(STOPPER_PIN, INPUT_PULLUP);

    stepper.begin(RPM, MICROSTEPS);
    // if using enable/disable on ENABLE pin (active LOW) instead of SLEEP uncomment next line
    // stepper.setEnableActiveState(LOW);
    stepper.enable();

    // set current level (for DRV8880 only). Valid percent values are 25, 50, 75 or 100.
    // stepper.setCurrent(100);

    Serial.println("START");

    // set the motor to move continuously for a reasonable time to hit the stopper
    // let's say 100 complete revolutions (arbitrary number)
    stepper.startMove(9999999 * MOTOR_STEPS * MICROSTEPS);     // in microsteps
    // stepper.startRotate(100 * 360);                     // or in degrees
}

void loop() {
    // first, check if stopper was hit
    if (digitalRead(STOPPER_PIN) == LOW){
        Serial.println("STOPPER REACHED");

        /*
         * Choosing stop() vs startBrake():
         *
         * constant speed mode, they are the same (stop immediately)
         * linear (accelerated) mode with brake, the motor will go past the stopper a bit
         */

        stepper.stop();
        // stepper.startBrake();
    }

    if (digitalRead(UP_PIN) == HIGH){
        Serial.print("Speed UP ");
        RPM += 60;
        Serial.println(RPM);
        stepper.setRPM(RPM);
    }

    if (digitalRead(DOWN_PIN) == HIGH){
        Serial.print("Speed DOWN ");
        RPM -= 60;
        Serial.println(RPM);
        if(RPM >= 0)
        stepper.setRPM(RPM);
    }

    // motor control loop - send pulse and return how long to wait until next pulse
    unsigned wait_time_micros = stepper.nextAction();

    // 0 wait time indicates the motor has stopped
    if (wait_time_micros <= 0) {
        stepper.disable();       // comment out to keep motor powered
        delay(3600000);
    }

    // (optional) execute other code if we have enough time
    if (wait_time_micros > 100){
        // other code here
    }
}
