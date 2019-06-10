https://stackedit.io/app#
# Microcontroller Software Technical Manual

Here we will talk about the code in great depth.

## Microprocessor (ATMega328P)

Before we begin we need to talk about the hardware that the microprocessor has built in. We will only mention the parts relevant to the codebase. Below are a list of terms specific to this section. These terms will also show up at the end of the document.

Register - A hardware unit that can store a value of a specific size. Effectively acts like a variable.

Prescaler - A device that scales back the frequency of a signal. `Input_Frequency / Prescaler = Output_Frequency`
</br>

### Clock
Our Microprocessor has a 16 MHz clock connected to it (produce a signal with 16,000,000 cycles per second)
</br>

### Timer/Counter
A Timer/Counter is a register that counts up automatically when some sort of signal is provided to it. If the signal provided is of a consistent frequency it becomes a timer that counts up at set intervals. These counters do not take up time on the main CPU so they can run separately without disturbing the main code.

Each one of these Timer/Counters consists of the main register that stores the count as well as a prescaler and some auxiliary registers to program the counter.

From this point forward we will use the words Timer and Counter interchangeably to reference these devices

On our processor we have three timers (T0, T1, T2).


#### T0
This is a 8-bit timer that is used to count the number of holes that the motor encoder passes by. Every time it sees a hole the value in T0 gets incremented by 1.

#### T1
This is a 16-bit timer that is configured to operate in 10-bit mode.

This timer has a prescaler of 1 so every time the clock cycles the counter will increment by 1. Since it is operating in 10-bit mode it will reset to 0 after reaching 1023 `(2^10 - 1)`.

We use this timer to produce our PWM signal. We do this by setting a number between 0 and 1024 in one of the auxiliary registers and every time the counter is incremented, it will check with the auxiliary register. If the values match then it will turn off Pin 15 on the microcontroller. The pins automatically turn on when the counter overflows to 0.

#### T2
This is a 8-bit timer that is used as our system clock. It has a prescaler of 64 (increments by 1 every 64 clock cycles). This is the clock that we use when we need to calculate the amount of time that has passed.

Each count in this timer is equal to 4 microseconds. You can obtain this number by dividing the prescaler by the clock frequency `(64 / 16,000,000 = 0.000004)`.

Since this is an 8-bit timer, the timer can count up to 255 `(2^8 - 1)`. This means that the total amount of time that the timer can count is 1.024 milliseconds `(4 µs * 256 = 1024 µs)`. To combat this short amount of time. Every time the counter counts to 1024 µs, an interrupt is called to record the overflow. This allows us to keep track of longer amounts of time. The overflow function `ISR(TIMER2_OVF_vect);` can be found at `wiring2.cpp:36`. This function also helps keep track of other things that we need to time, which will be discussed later.
</br>

### Analog Comparator
A comparator is a device that takes in two voltages and outputs a digital signal `(HIGH/LOW)`. Our processor has a comparator built into it on pins 12 and 13. We use this comparator because our speed sensor's schmitt trigger was built using a spare op-amp instead of a comparator. This means that the schmitt trigger output is an analog square wave that has some noise. We then just use the built in comparator to convert the signal to digital by checking to see if it goes above 3.3v.
</br>

## Main Code

### Overview
This is the most straightforward part of the code. When the device starts up it runs `Setup()` once and then it runs `Loop()` over and over again.

### `controller.ino`:`Setup();`
Here we set up the hardware that we will be using. We set up the timers and analog comparator discussed before by setting bits to their respective settings registers. Information for this can be found in the ATMega328P Manual. This manual can be found [here]([https://www.sparkfun.com/datasheets/Components/SMD/ATMega328.pdf](https://www.sparkfun.com/datasheets/Components/SMD/ATMega328.pdf)).

We also set the pins to the proper mode as well as load in previously stored values for the variables.
</br>

### `controller.ino`:`Loop();`
Here we run the bulk main processes in the following order:

1. Check bluetooth to see if any signals come in, if so go deal with the signal
    - See bluetooth section below for more details
2. Calculate current speed
    - Explanation of the equation will be in the equations section below.
3. Make sure the current speed can be displayed on the speedometer
    - If the current speed is too high then we just show the maximum speed
4. Calculate the target RPM we want the motor to spin at. 
    - If the current speed is 0 we just stop the motor.
    - If the app put us in a special mode (ie. Wizards), we also stop the motor to prevent undefined behavior
    - If neither of the cases above are true then it is just `current speed * speedometer ratio`. The extra constants are just unit adjustments so we can avoid using floats. More information in the equations section.
5. Calculated the current RPM 
    - Explanation of the equation will be in the equations section below.
6. Calculate PID and adjust PWM to control motor
7. Check if it needs to print out the debug logs
8. Stall for time until it needs to start the next iteration
    - This delay is added in because PID works best when each iteration is about the same time period apart

## Interrupts

### Overview
We use this section to run code alongside the main loop. These code blocks typically don't run very often and are much shorter then  the main loop. These code sections also have a higher priority. They will interrupt the main loop whenever a certain event is triggered.

### `controller.ino`:`ISR(ANALOG_COMP_vect);`
This interrupt is triggered when a magnet passes over our speed sensor. All it does is increment a counter that tells us how many magnets have passed in this time frame.
If we happen to be in the final drive wizard we will also increment a counter specifically for that wizard.

### `wiring2.ino`:`ISR(TIMER2_OVF_vect);`
This interrupt is triggered whenever Timer2 overflows (every 1.024 ms) and it does 4 things
1. It increments counters to keep track of longer periods of time
    - this is standard for arduino code and is usually done elsewhere 
2. Every 64 calls of this function we will save the values from the speed sensor counter and Timer0 (encoder data)
    - This will happen once every 65.536ms or 15.26 samples a second.
        * This 15.26 number is important. From now on we will call this the **`sample rate`**
    - Why did we choose this value? it was arbitrary. We felt like collecting data from the sensor 15-ish times a second was enough and the numbers played nice with our equations.
3. Reset the values we just read from to 0 to prepare for the next reading
4. If we are in the final drive wizard reduce the amount of samples that we still need to collect

## Wizards

### Overview
Because we implemented wizards in the app we need to set special states for the device to be in. This is because some of the values will be undefined when the wizards are ran and might damage the speedometer if we don't put them in these specialized states.

Here is an overview of how the wizards work on the side of the microcontroller.
1. Speedometer Ratio Wizard
    - In this wizard we expect the user to be the feedback for the calibration
    - On the app side this speedometer allows the user to adjust the speedometer ratio on their phone until the speedometer shows a predefined speed
    - On the microcontroller
        * The bluetooth will read in a targetRPM from the app
        * It will then skip calculating the targetRPM as to not overwrite the value that was read in.
        * Using the targetRPM that was read in, it will use PID to adjust itself until it spins at that speed.
        * The user will then increase or decrease the value depending on what they need for their speedometer.

2. Final Drive Wizard
    - In this wizard we expect the user to hold a constant speed so the microcontroller can sample the sensor's rate at a specific speed. The microcontroller will then take the rate and speed to calculate a final drive.
    - On the microcontroller
        * The targetRPM is set to 0 at all times so the speedometer will not run with a potentially faulty final drive value. The main loop will only be used for communicating over bluetooth. Everything else is handled by the two interrupt loops
        * The bluetooth will read in the start signal when the user has reached sufficient speed and start recording the number of sensor readings in a period of \~10 seconds `(153 samples = sample_rate * 10)`.
        * After 10 seconds the microcontroller will then wait for an average speed from the users phone.
        * With the number of readings it collected and the average speed, the microcontroller can now calculate the final drive. Once calculated it will send the value back to the phone to be displayed on the menu.
        * Equations for the final drive ratio will be below in the equations section


## Bluetooth
### Overview
### Basic Communication Format
### Communication Format Table
### Debugging Format Table

## Equations
### Overview
### Common Equations and Values
