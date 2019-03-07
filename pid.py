#from __future__ import print_function
import sys
import os
import math
import string
import pdb

pwm = 180 # Could be anything 0-1023
current = 2000 # Could be anything 0-12000
target = 3000 # Could be anything 0-12000
max = 1023
min = 0
oldErr = 0
pid_i = 0
kp = .3 # Proportional coefficient
ki = 0.05 # Integral coefficient 
kd = .2 # Derivative coefficient 
elapsedTime = 100
acceptableErr = 1
currRPM = []
pwmRec = []
rpmToPwm = 10
# Not a true simulation obviously, but close in spirit. Here pwm is tied in a predictable way to rpm and one could simply
# deduce the correct pwm using (target/current)*startingPWM = correctPWM. But this is not the way of the real world. 
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
