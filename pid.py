#from __future__ import print_function
import sys
import os
import math
import string
import pdb

pwm = 180
current = 2000
target = 3000
max = 1023
min = 0
oldErr = 0
pid_i = 0
kp = .3
ki = 0.05
kd = .2
elapsedTime = 100
acceptableErr = 1
currRPM = []
pwmRec = []
rpmToPwm = 10
print("Starting RPM: " + str(current) + "   Target RPM: " + str(target) + "    Starting PWM: " + str(pwm) + "\n") 
print("Current Motor RPM    Recommended PWM")
while abs(target - current) > acceptableErr:
    error = target - current
    pid_p = kp*error
    pid_i = pid_i + ki*error
    pid_d = kd*((error - oldErr)/elapsedTime)
    PID = pid_p + pid_i + pid_d
    current += PID
    pwm += PID/rpmToPwm
    if (pwm > max):
        pwm = max
    elif (pwm < min):
        pwm = min
    oldErr = error
    currRPM.append(current)
    pwmRec.append(pwm)
    print ("    " + str(int(current)) + "                  " + str(int(pwm)))
print ("\n" + str(len(pwmRec)) + " iterations")
