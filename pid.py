# Run using Python 2.7
import sys
import os
import math
import string
import pdb

pwm = 180 # Could be anything 0-1023
current = 20000 # Could be anything 0-120000, divide by 10 to get rpm
target = 30000 # Could be anything 0-120000, divide by 10 to get rpm
max = 1023 # PWM_MAX
min = 0 # PWM_MIN
oldErr = 0
pid_i = 0
kp = 31 # Proportional coefficient
ki = 18 # Integral coefficient
kd = 22 # Derivative coefficient
kff = 90
acceptableErr = 20 # 2 rpm
currRPM = []
pwmRec = []
rpmToPwm = 100
# Not a true simulation obviously, but close in spirit. Here pwm is tied in a predictable way to rpm and one could simply
# deduce the correct pwm using (target/current)*startingPWM = correctPWM. But this is not the way of the real world.
print("Starting RPM: " + str(current) + "   Target RPM: " + str(target) + "    Starting PWM: " + str(pwm) + "\n")
print("Current Motor RPM    Recommended PWM")
while abs(target - current) > acceptableErr:
    error = target - current
    ff = kff*target/100
    pid_p = kp*error/100
    pid_i += ki*error/100
    pid_d = kd*(error - oldErr)/100
    current = pid_p + pid_i + pid_d + ff
    pwm = current/rpmToPwm
    if (pwm > max):
        pwm = max
    elif (pwm < min):
        pwm = min
    oldErr = error
    currRPM.append(current)
    pwmRec.append(pwm)
    print "    " + str(int(current)) + "                  " + str(int(pwm))
print "\n" + str(len(pwmRec)) + " iterations"
