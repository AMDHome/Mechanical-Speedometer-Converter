


# Microcontroller Software Technical Manual

Here we will talk about the code in great depth.

## Microprocessor (ATMega328P)

Before we begin we need to talk about the hardware features that the microprocessor has built in. We will only mention the parts relevant to the codebase. Below are a list of terms specific to this section. These terms will also show up at the end of the document.

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

On our processor we have three timers (T0, T1, T2). Below is a flowchart of the processes that drive these three timers

<img src="https://github.com/AMDHome/Mechanical-Speedometer-Converter/blob/master/assets/High%20Level%20Diagram/Software%20Workflow%20Hardware.png?raw=true" alt="Hardware Processes Flowchart" width="50%" height="50%">


#### T0
This is a 8-bit timer that is used to count the number of holes that the motor encoder passes by. Every time it sees a hole the value in T0 gets incremented by 1.

#### T1
This is a 16-bit timer that is configured to operate in 10-bit mode.

This timer has a prescaler of 1 so every time the clock cycles the counter will increment by 1. Since it is operating in 10-bit mode it will reset to 0 after reaching 1023 `(2^10 - 1)`.

We use this timer to produce our PWM signal. We do this by setting a number between 0 and 1024 in one of the auxiliary registers `(OCR1A)` and every time the counter is incremented, it will check with the auxiliary register. If the values match then it will turn off Pin 15 on the microcontroller. The pins automatically turn on when the counter overflows to 0.

#### T2
This is a 8-bit timer that is used as our system clock. It has a prescaler of 64 (increments by 1 every 64 clock cycles). This is the clock that we use when we need to calculate the amount of time that has passed.

Each count in this timer is equal to 4 microseconds. You can obtain this number by dividing the prescaler by the clock frequency `(64 / 16,000,000 = 0.000004)`.

Since this is an 8-bit timer, the timer can count up to 255 `(2^8 - 1)`. This means that the total amount of time that the timer can count is 1.024 milliseconds `(4 µs * 256 = 1024 µs)`. To combat this short amount of time. Every time the counter counts to 1024 µs, an interrupt is called to record the overflow. This allows us to keep track of longer amounts of time. The overflow function `ISR(TIMER2_OVF_vect);` can be found at `wiring2.cpp:36`. This function also helps keep track of other things that we need to time, which will be discussed later in the interrupt section
</br>

### Analog Comparator
A comparator is a device that takes in two voltages and outputs a digital signal `(HIGH/LOW)`. Our processor has a comparator built into it on pins 12 and 13. We use this comparator because our speed sensor's schmitt trigger was built using a spare op-amp instead of a comparator. This means that the schmitt trigger output is an analog square wave that has some noise. We then just use the built in comparator to convert the signal to digital by checking to see if it goes above 3.3v.
</br>

## Code Layout
All of our code is put into 5 files. Below are the different parts of code that are in each file
- `controller.ino`: Setup and Main Loop
- `BTComms.h/cpp`: All bluetooth I/O is taken care by these two files
- `wiring2.h/cpp`: All timing functions are in this file. It is a modified version of arduino's original `wiring.cpp` and timing functions

## Main Code

### Overview
This is the most straightforward part of the code. When the device starts up it runs `Setup()` once and then it runs `Loop()` over and over again.

### `controller.ino`:`Setup();`
Here we set up the hardware that we will be using. We set up the timers and analog comparator discussed before by setting bits to their respective settings registers. Information for this can be found in the ATMega328P Manual. This manual can be found [here]([https://www.sparkfun.com/datasheets/Components/SMD/ATMega328.pdf]).

We also set the pins to the proper mode as well as load in previously stored values for the variables.
</br>

### `controller.ino`:`Loop();`
Here we run the bulk main processes. Below is a flowchart of the entire loop process:
![Main Loop](https://github.com/AMDHome/Mechanical-Speedometer-Converter/blob/master/assets/High%20Level%20Diagram/Software%20Workflow%20Main.png?raw=true)

Below is a description of the process in text form:
```The flowchart above includes the bluetooth processing portion. We have left that out of this section. You may find a description of the bluetooth processes in the bluetooth section below.```

1. Check bluetooth to see if any signals come in, if so go deal with the signal
    - See bluetooth section below for more details
2. Calculate current speed
    - Explanation of the equation will be in the equations section below.
3. Make sure the current speed can be displayed on the speedometer
    - If the current speed is too high then we just show the maximum speed
4. Calculate the target RPM we want the motor to spin at. 
    - If the current speed is 0 we just stop the motor.
    - If the app put us in a special mode (ie. Wizards), we also stop the motor to prevent undefined behavior
    - If we manually assign the targetRPM (only in Speedometer Wizard) then use the assigned value as the targetRPM
    - If neither of the cases above are true then it is just `current speed * speedometer ratio`. The extra constants are just unit adjustments so we can avoid using floats. More information in the equations section.
5. Calculate the current RPM 
    - Explanation of the equation will be in the equations section below.
6. Calculate PID and adjust PWM to control motor
7. Check if it needs to print out the debug logs
8. Stall for time until it needs to start the next iteration
    - This delay is added in because PID works best when each iteration is about the same time period apart
    - This delay can be removed to improve responsiveness; however, it may increase the difficulty of tuning the PID.

## Interrupts

### Overview
We use this section to run code alongside the main loop. These code blocks typically don't run very often and are much shorter then  the main loop. These code sections are also time sensitive and have a higher priority. They will interrupt the main loop whenever a certain event is triggered.

Here is a flowchart of the interrupts:

<img src="https://github.com/AMDHome/Mechanical-Speedometer-Converter/blob/master/assets/High%20Level%20Diagram/Software%20Workflow%20Interrupts.png?raw=true" alt="Interrupt Processes Flowchart" width="33%" height="33%">

### `controller.ino`:`ISR(ANALOG_COMP_vect);`
This interrupt is triggered when a magnet passes over our speed sensor. All it does is increment a counter that tells us how many magnets have passed in this time frame.
If we happen to be in the final drive wizard we will also increment a counter specifically for that wizard to record distance.

### `wiring2.ino`:`ISR(TIMER2_OVF_vect);`
This interrupt is triggered whenever Timer2 overflows (every 1.024 ms) and it does 4 things
1. It increments counters to keep track of longer periods of time
    - this is standard for arduino code and is usually done elsewhere 
2. Every 64 calls of this function we will save the values from the speed sensor counter and Timer0 (encoder data) into two seperate circular arrays.
    - This 64 value is kinda like a software prescaler for recording speed data.
    - This will happen once every 65.536ms or 15.26 samples a second.
        * This 15.26 number is important. From now on we will call this the **`sample rate`**
    - Why did we choose this value? it was arbitrary. We felt like collecting data from the sensor 15-ish times a second was enough and the numbers played nice with our equations. This number can be changed but the equations will need to be recalculated.
3. Reset the values we just read from to 0 to prepare for the next reading
4. If we are in the final drive wizard reduce the amount of samples that we still need to collect

## Wizards

### Overview
Because we implemented wizards in the app we need to set special states for the device to be in. This is because some of the values will be undefined when the wizards are ran and might damage the speedometer if we don't put them in these specialized states.

Here is an overview of how the wizards work on the side of the microcontroller.
1. Speedometer Ratio Wizard
    - In this wizard we expect the user to be the feedback for the calibration
    - On the app side:
        * The speedometer wizard allows the user to adjust the speedometer ratio until the speedometer reads a predefined speed.
        * The app will continuously calculate a target RPM based on the inputted speedometer ratio and the predefined speed and send it to the microcontroller
        * If the speedometer reading is too low, the user needs to increase the speedometer ratio. If it's too fast, they need to decrease it.
    - On the microcontroller
        * The bluetooth will read in a targetRPM from the app
        * It will then skip calculating the targetRPM as to not overwrite the value that was read in.
        * Using the targetRPM that was read in, it will use PID to adjust itself until it spins at that speed.
        * The user will then increase or decrease the value depending on what they need for their speedometer.
        * When the user is happy with the value, the new speedometer will be stored and the microcontroller will be taken out of the Speedometer Calibration mode

2. Final Drive Wizard
    - In this wizard we expect the user to drive and hold a constant speed so the microcontroller can sample the sensor's rate at a specific speed. The microcontroller will then take the rate and speed to calculate a final drive.
    - On the microcontroller
        * The targetRPM is set to 0 at all times so the speedometer will not run with a potentially faulty final drive value. The main loop will only be used for communicating over bluetooth. Everything else is handled by the two interrupt loops
        * The bluetooth will read in the start signal when the user has reached sufficient speed and start recording the number of sensor readings in a period of \~10 seconds `(153 samples = sample_rate * 10)`.
        * If the app restarts the countdown then it will send the start signal again at which the microcontroller will restart this process.
        * After 10 seconds the microcontroller will then wait for an average speed from the users phone.
        * With the number of readings it collected and the average speed, the microcontroller can now calculate the final drive. Once calculated it will send the value back to the phone to be displayed on the menu.
        * Equations for the final drive ratio will be below in the equations section


## Bluetooth
### Overview
During the main loop, the program will check the bluetooth module for any new data, We will now go through the bluetooth section of the main loop here.

Below is the entirety of the main loop again for ease of reference.
![Main Loop](https://github.com/AMDHome/Mechanical-Speedometer-Converter/blob/master/assets/High%20Level%20Diagram/Software%20Workflow%20Main.png?raw=true)

### Basic Communication Format
All commands sent to our microcontroller follow a very basic format. Below is an example of a command.
`S:18000000`

Format:
1. We start the command off with a capital letter. This is an identifier that tells us how to processes this command.
2. The second character is always a colon. This is here just for human readability for now. In the future, if you run out of identifiers, you can  increase the start of the command to 2 letters or more and use the colon as a delimiter.
3. The remainder of the command is the payload. The payload can have anything in it as long as it is less than 23 characters long. This is an arbitrary length that suits our purposes and can be increased in the future if you run out of room.

The program in its current state makes a 3 assumptions in the command that it receives:
1. The command follows the format above
2. The commands ends with a null character `'\0'`
3. Capital letters can only be at the start of the command or right after a colon. 

Any command added in the future that does not follow these assumptions will need the bluetooth functions recoded to support it. One such assumption that has already been coded in is the capital letter `R`. Since this letter appears in the tire size as the radius, we have coded it to not trigger the error checking
### Reading The Data In
We do all of our data receiving in the function `BTComms.cpp:recvData();`

This function reads in the available data one byte/character at a time. For each character read in, it does the following checks:
1. Capital letter:
    - If the byte that it just read in is a Capital letter, then it checks to makes sure that it is in a valid location (see assumption number 3 in the previous section).
    - If the capital letter does not meet the assumptions from before then it will assume that the ending of the previously read data was corrupted and that this capital letter is the start of a new command. It will then discard all previously read data and set this capital letter to be the first character read
2. NULL character `'\0'`:
    - If the byte that was just read in is a NULL byte, the program assumes that the command has terminated.
    - It will check the first character read to see if it is a capital letter. 
    - If both of these assumptions are met, it will then process the command.
    - If one or more of the assumptions are broken then it will assume the command is corrupted and delete all previously read data
3. Anything else:
    - If neither of those cases occour, then it will just contnue to read in data.
        - If there's nothing more to read, it will go back to running the main loop. When there is something else to read, the program will continue from where it left off.
        
### General Commands Format Table
Here is a list of all commands that are currently sent from the app, what the command does, the data type that we use to store the commands on the microcontroller, as well as some expected values.

| Command |                       Description                      |  Data Type  |         Expected Values*         | Example Command      | Notes |
|:-------:|--------------------------------------------------------|:-----------:|---------------------------------|----------------------|:-----:|
|    U    | Set **U**nits                                              |     Byte    | 0: KMH, 1: MPH                  | U:0                  |       |
|    M    | Set **M**ax Speed                                          |    Short    | 0 - 300                         | M:120                | 1     |
|    N    | Set **N**umber of Magnets                                  |     Byte    | 1 - 4                           | N:2                  |       |
|    F    | Set **F**inal Drive Ratio                                  |    Float    | 0 - 10,000,000                  | F:1000000            | 2     |
|    S    | Set **S**peedometer Ratio                                  |     Long    | 0 - 100,000,000                 | S:18000000           | 2     |
|    W    | Set **W**heel Size                                         | Long : String | 0 - 10,000,000 : Metric Tire Size | W:1263341:P205/65R15 | 3     |
|    P    | Put/Exit Microcontroller into S**p**eedometer Ratio Wizard |     Byte    | 0: Start, 1: Stop               | P:1                  |       |
|    T    | Set **T**arget RPM (For Speedometer Wizard)                |    Short    | 0 - 12,000                      | T:3000               |       |
|    L    | **L**oad Values (For App Startup)                          |     Byte    | 1                               | L:1                  | 4     |
|    D    | Start/Stop/Finish Final **D**rive Ratio Wizard             |  Byte/Long  | S: Start, <br> F: Cancel, <br>0 - 500    | D:S <br>D:F <br>D:400        | 5     |

Notes:
*   All values except for final drive are stored exactly the same as the way they are received (ie if an adjustment was made then the adjustment will be stored). Final drive is the only value we convert back to the original value.
1. Although We could technically store all realistic speed values in a byte (0 - 255) we chose to use a short as we will be multiplying it by 10 for our equations later
2. These numbers are multiplied by a million `(1,000,000)` so we don't have to deal with decimals. Transferring decimals over commands adds another thing to check for which increases runtime.
3. Wheel size is the only complex command that we have as there are two parts to the payload.
    - The First Part is a large number. This is the circumference of your tire in terms of miles and kilometers then multiplied by a billion (1,000,000,000). This way we can leverage the more powerful phone cpu to do some of the calculations for us.
        - The units that the first number is sent in is the same as the units that you set for the 
    - The Second Part is the tire size as regular text. This is what we send back to the phone when you load the app.
4. The load command is a special command. Once received, the microcontroller will send back a command to the phone in the following format. ```L:Units:Max_Speed:Magnets:Final_Drive_Ratio:Speedometer_Ratio:Wheel_Size_Text```
    - Ex ```L:0:120:2:1000000:18000000:P205/65R15```
5. This command has 3 different possible payloads:
    - If it receives an S it will put the device into the Final Drive Ratio Calibration Mode
    - If it receives an F it will take the device out of the Final Drive Ratio Calibration Mode
    - If it receives a number it will calculate the Final Drive Ratio using the average speed recieved and the distance it recorded (see equations below), and then take it out of the Final Drive Ratio Calibration Mode

### Debugging Commands Format Table
Here is a list of "Advanced" commands that the microcontroller will understand. These are commands that I have programmed in to use when creating and debugging the device itself. I didn't remove them as I thought they might be useful in the future for whatever reason. In order to use these commands you must use a Bluetooth Serial Terminal app like [this one]([https://play.google.com/store/apps/details?id=project.bluetoothterminal&hl=en_US]).


| Command | Description                      | Expected Values       | Example Command | Notes |
|:-------:|----------------------------------|-----------------------|-----------------|:-----:|
|    B    | Enable/Disable Debug Logs        | 0: Disable, 1: Enable | B:0             |       |
|    X    | Set P value in the PID Equations | 0 - (2^31 - 1)        | X:120           |  1,3  |
|    Y    | Set I value in the PID Equations | 0 - (2^31 - 1)        | Y:2             |  2,3  |
|    Z    | Set D value in the PID Equations | 0 - (2^31 - 1)        | Z:1             |  2,3  |

Notes:
1. This value is divided by 1,000 in our PID equations.
2. This value is divided by 10,000 in our PID equations.
3. This command will not store the PID across reboots. If you wish to keep your PID permanently you will need to modify the source code manually. I did not implement saving PID values into permanent storage as I don't think they should be modified in a production.

### Programming the Microcontroller
Now all of the general commands are sent and used via the app that was created for this project; however, if there is something that the app doesn't do/implement well, you can just a bluetooth serial terminal app to program it manually. You will then not be constrained by our app. Any bluetooth serial terminal app will work. The one that I use is linked above (and linked [here]([https://play.google.com/store/apps/details?id=project.bluetoothterminal&hl=en_US]) again for your convenience).

## Equations
### Overview
Here I will derive out all the equations that I used in the microcontrollers.

Some of the equations look weird and it is because floating points (decimals) are very costly (timewise) for this CPU. Because of this I will multiply an adjustment value in order to do the calculations. This value will be divided out near the end to preserve as many significant figures as possible. I know I probably should've used a fixed point library to do it for me instead of multiplying my own adjustment ratio, but the equations were simple enough and the library adds time and complexity to the runtime.

### On The Phone
#### Wheel Size:
We will use the generic equation to convert metric wheel sizes to circumference in inches.
<img src="https://latex.codecogs.com/gif.latex?Circ\;(in)=\left(\frac{x\cdot&space;y&space;\cdot2}{2540}&plus;z\right)\cdot&space;pi" title="Circ\;(in)=\left(\frac{x\cdot y \cdot2}{2540}+z\right)\cdot pi" alt="Wheel Size Equation"/>


This equation assumes that the first set of numbers is x, second set is y, and thrid set is z ```(Ex. In P205/55R15 -> x = 205, y = 55, z= 15)```

From there we will convert the circumference to the value we send. The value sent is the circumference in miles or kilometers (depending on the units selected), and then multiplied by a billion. We have the values differ here so that the microcontroller does not have to deal with units at all. If you look through the equations, they are all unit agnostic and our variables are Speed Per Hour (SPH) instead of MPH or KPH.

<img src="https://latex.codecogs.com/svg.latex?\inline&space;\dpi{300}&space;\large&space;\begin{align*}&space;Sent\;Value\;(km)&=Circ\;(in)\cdot\frac{2.54\;cm}{1\;in}\cdot\frac{1\;km}{100000\;cm}*1000000000\\&space;\\&space;&=\frac{Circ\;(in)\;\cdot2.54}{100000}*1000000000&space;\end{align*}" title="\large \begin{align*} Sent\;Value\;(km)&=Circ\;(in)\cdot\frac{2.54\;cm}{1\;in}\cdot\frac{1\;km}{100000\;cm}*1000000000\\ \\ &=\frac{Circ\;(in)\;\cdot2.54}{100000}*1000000000 \end{align*}" alt="Sent km Equation"/>

<br>

<img src="https://latex.codecogs.com/svg.latex?\inline&space;\dpi{300}&space;\large&space;\begin{align*}&space;Sent\;Value\;(mi)&=Circ\;(in)\cdot\frac{1\;mi}{63360\;in}*1000000000\\&space;\\&space;&=\frac{Circ\;(in)}{63360}*1000000000&space;\end{align*}" title="\large \begin{align*} Sent\;Value\;(mi)&=Circ\;(in)\cdot\frac{1\;mi}{63360\;in}*1000000000\\ \\ &=\frac{Circ\;(in)}{63360}*1000000000 \end{align*}" alt="Sent mi Equation"/>


### On The Microcontroller
#### Time For 1 Sample
So, I've talked a bit about the magic numbers 65.536ms and 15.26 samples a second. These numbers are used to record the speed data of both the vehicles current speed as well as the motors current RPM. To get these values we use the system clock speed of ```16,000,000MHz```, the hardware prescaler value for Timer2 ```64 Cycles/Increment```, the max value for Timer2 ```256 Increments/OVF```, and the software prescaler we set of  ```64 OVF / Sample```

<img src="https://latex.codecogs.com/svg.latex?\inline&space;\dpi{300}&space;\large&space;\begin{align*}&space;Sample\;Rate&=\frac{16000000\;cycles}{1\;s}\cdot\frac{1\;inc.}{64\;cycles}\cdot\frac{1\;ovf}{256\;inc}\cdot\frac{1\;sample}{64\;ovf}\\&space;\\&space;&=\frac{15625}{1024}\approx15.26\;(Samples/Second)&space;\end{align*}" title="\large \begin{align*} Sample\;Rate&=\frac{16000000\;cycles}{1\;s}\cdot\frac{1\;inc.}{64\;cycles}\cdot\frac{1\;ovf}{256\;inc}\cdot\frac{1\;sample}{64\;ovf}\\ \\ &=\frac{15625}{1024}\approx15.26\;(Samples/Second) \end{align*}" alt="Sample Rate Equation"/>

To get the time per sample just take the inverse of the value above.
On the microcontroller we will stick with the fractional values for maximum accuracy.

#### The "InRatio"
Everytime we calculate the speed of the vehicle, we need to use the Number of Magnets, Final Drive, and Wheel Size. Since all of these should be constants There's no point in calculating it every time; therefore, we have a variable called InRatio just for a precalculated value of these three constants to save time during calculations.

Since our generic formula for calculating vehicle speed is:
<img src="https://latex.codecogs.com/svg.latex?\inline&space;\dpi{300}&space;\large&space;\begin{align*}&space;Vehicle\;Speed&=&space;\frac{Number\;of\;Magnets\;Passed}{Unit\;of\;time}\cdot\frac{1\;Rotation\;(S.S.)}{Number\;of\;Magnets}\cdot\frac{Rotations\;(W)}{Rotations\;(S.S.)}\cdot\frac{Wheel\;Circ.\;(Dist.)}{1\;Rotations\;(W)}=\frac{Distance}{Unit\;of\;Time}&space;\end{align*}" title="\large \begin{align*} Vehicle\;Speed&= \frac{Number\;of\;Magnets\;Passed}{Unit\;of\;time}\cdot\frac{1\;Rotation\;(S.S.)}{Number\;of\;Magnets}\cdot\frac{Rotations\;(W)}{Rotations\;(S.S.)}\cdot\frac{Wheel\;Circ.\;(Dist.)}{1\;Rotations\;(W)}=\frac{Distance}{Unit\;of\;Time} \end{align*}" alt="Generic Speed Formula"/>

As we can see here the last three terms in our dimensional analysis are basically our Number of Magnets, Final Drive, and Wheel Size respectively. We will pull those three terms our and call it our inratio. 
<img src="https://latex.codecogs.com/svg.latex?\inline&space;\dpi{300}&space;\large&space;\begin{align*}&space;InRatio&=&space;\frac{1\;Rotation\;(S.S.)}{Number\;of\;Magnets}\cdot\frac{Rotations\;(W)}{Rotations\;(S.S.)}\cdot\frac{Wheel\;Circ.\;(Dist.)}{1\;Rotations\;(W)}\;/\;4=\frac{Wheel\;Circ.}{Number\;of\;Magnets\cdot&space;Final\;Drive}\;/\;4&space;\end{align*}" title="\large \begin{align*} InRatio&= \frac{1\;Rotation\;(S.S.)}{Number\;of\;Magnets}\cdot\frac{Rotations\;(W)}{Rotations\;(S.S.)}\cdot\frac{Wheel\;Circ.\;(Dist.)}{1\;Rotations\;(W)}\;/\;4=\frac{Wheel\;Circ.}{Number\;of\;Magnets\cdot Final\;Drive}\;/\;4 \end{align*}" alt="InRatio Formulia"/>

The InRatio is divided by 4 to make sure all numbers don't get too big later on.

#### Current Speed
The way we calculate the current speed is by taking 16 samples of data (~1 s) and using it to find the angular velocity. Once we have that all we need to do is multiply the wheel size and divide out the adjustments we made before. The samples of data tell us how many magnets have passed in each time period, and are stored in a circular array of size 16 called speedCtr.

Since this is where we calculate the actual speed of our vehicle we will need to divide by a billion to compensate for the adjustments on our wheel size (see wheel size equation). We will also multiply the result by 10 so we have a resolution of 0.1 SPH without worrying about the decimal places.

Starting from the equation above, we can pull out InRatio and use basic algebra to simplify the equation to:
<img src="https://latex.codecogs.com/svg.latex?\inline&space;\dpi{300}&space;\large&space;\begin{align*}&space;Current\;Speed\;Per\;Hour\;(SPH\cdot10)&=&space;\frac{Number\;of\;Magnets\;Passed}{Unit\;of\;time}\cdot\frac{1\;Rotation\;(S.S.)}{Number\;of\;Magnets}\cdot\frac{Rotations\;(W)}{Rotations\;(S.S.)}\cdot\frac{Wheel\;Circ.\;(Dist.)}{1\;Rotations\;(W)}\cdot\frac{1}{1000000000}\cdot10\\&space;\\&space;&=\frac{\sum&space;speedCtr[i]\;(magnets)}{Number\;of\;samples}\cdot\frac{15625\;samples}{1024\;s}\cdot\left(InRatio\cdot4\right)\cdot\frac{3600\;s}{1\;hr}\cdot\frac{1}{1000000000}\cdot10\\&space;\\&space;&=\frac{\sum&space;speedCtr[i]\;(magnets)\cdot&space;InRatio\cdot9}{Number\;of\;samples\cdot1024}\\&space;\end{align*}" title="\large \begin{align*} Current\;Speed\;Per\;Hour\;(SPH\cdot10)&= \frac{Number\;of\;Magnets\;Passed}{Unit\;of\;time}\cdot\frac{1\;Rotation\;(S.S.)}{Number\;of\;Magnets}\cdot\frac{Rotations\;(W)}{Rotations\;(S.S.)}\cdot\frac{Wheel\;Circ.\;(Dist.)}{1\;Rotations\;(W)}\cdot\frac{1}{1000000000}\cdot10\\ \\ &=\frac{\sum speedCtr[i]\;(magnets)}{Number\;of\;samples}\cdot\frac{15625\;samples}{1024\;s}\cdot\left(InRatio\cdot4\right)\cdot\frac{3600\;s}{1\;hr}\cdot\frac{1}{1000000000}\cdot10\\ \\ &=\frac{\sum speedCtr[i]\;(magnets)\cdot InRatio\cdot9}{Number\;of\;samples\cdot1024}\\ \end{align*}" alt="SPHr Equation Derivation"/>

We have left the Number of Samples as a variable instead of simplifying the number into the equation so that if you ever want to change the number of samples recorded all you have to do is change the variable `MAX_RECORD` at the top of `controller.ino` instead of having to recalculate the equation once more. 

We then do some [exponential smoothing]([https://en.wikipedia.org/wiki/Exponential_smoothing]) to compensate for the relatively low resolution of having so few magnets for our speed sensor.

Note: Decreasing the number of samples yields less resolution to the speed of the vehicle, but will improve response times. Increasing does the opposite. You can increase the number of magnets to compensate for the loss in resolution.

#### Target RPM (DC Motor)
This one is fairly straightforward. When we get our speed per hour all we have to do is multiply it by the speedometer ratio to get the required motor RPM.

<img src="https://latex.codecogs.com/svg.latex?\inline&space;\dpi{300}&space;\large&space;\begin{align*}&space;targetRPM\cdot10&=Current\;Speed\;(SPH)\cdot\frac{Number\;of\;RPMs}{1\;SPH}\cdot\frac{1}{1000000}&space;\end{align*}" title="\large \begin{align*} targetRPM\cdot10&=Current\;Speed\;(SPH)\cdot\frac{Number\;of\;RPMs}{1\;SPH}\cdot\frac{1}{1000000} \end{align*}" alt="targetRPM Equation Derivation"/>

We split the divide by a million into two divide by 1000 to ensure that the multiplication wont go out of bounds while retaining as much resolution as possible.

#### Current RPM (DC Motor)
The equation for this is fairly similar to the current speed equation, except we don't need to convert it to linear velocity. RPM stays at angular velocity. We will also multiply the value by 10 to get a resolution of 0.1 RPM's

<img src="https://latex.codecogs.com/svg.latex?\inline&space;\dpi{300}&space;\large&space;\begin{align*}&space;Motor\;RPM\cdot10&=&space;\frac{Number\;of\;Holes\;Passed}{Unit\;of\;time}\cdot\frac{1\;Rotation}{Number\;of\;Holes}\cdot10\\&space;\\&space;&=\frac{\sum&space;encoderCtr[i]\;(Holes)}{Number\;of\;samples}\cdot\frac{15625\;samples}{1024\;s}\cdot\frac{1\;Rotation}{20\;Holes}\cdot\frac{60\;s}{1\;min}\cdot10\\&space;\\&space;&=\frac{\sum&space;speedCtr[i]\;(Holes)\cdot46875}{Number\;of\;samples\cdot1024}\cdot10\\&space;\end{align*}" title="\large \begin{align*} Motor\;RPM\cdot10&= \frac{Number\;of\;Holes\;Passed}{Unit\;of\;time}\cdot\frac{1\;Rotation}{Number\;of\;Holes}\cdot10\\ \\ &=\frac{\sum encoderCtr[i]\;(Holes)}{Number\;of\;samples}\cdot\frac{15625\;samples}{1024\;s}\cdot\frac{1\;Rotation}{20\;Holes}\cdot\frac{60\;s}{1\;min}\cdot10\\ \\ &=\frac{\sum speedCtr[i]\;(Holes)\cdot46875}{Number\;of\;samples\cdot1024}\cdot10\\ \end{align*}" alt="currentRPM Equation Derivation"/>

The array size, and subsequently, number of samples for encoderCtr is 4. 

#### PID Equations
Our PID equations are pretty standard. Only difference is that, once again, to avoid decimals we multiply values by an adjustment. 
At the top of `controller.ino` you will find the pid values in whole numbers. If you scroll down to line 141-143, you can find these equations:
```C
    pid_p = KP * error / 1000;
    pid_i += KI * error / 10000;
    pid_d = KD * (error - oldErr) / 10000;
```
PID values are typically less than 1 so to compensate for it, we multiply and then divide the division in these lines of code will tell you the coefficients. (ex. KP is 11 so the P coefficient is actually 0.011) 

## Issues/Things to Note
While we try our best to make it perfect, there are some issues and intricacies that we found last minute. We will try to get the issues solved, but I cannot promise as I have no control over my teammates. Not all the things in this list are necessarily issues. Some of them are things to keep in mind as they don't really matter, but in a perfect world none of these things would exist.  ~Samuel

Notes:
1. You **must** set your units before setting your wheel size since the calculations are done via the phone based on the units. If you set your units after then the wheel size might be in the wrong units as it does not get recalculated/resent.
2. This might be due to my lack of experience with PID, but the motor for the speedometer seems to run slow when accelerating and run fast while decelerating. This means that if you slow down to a particular speed it will show a speed higher than what you are currently at, when you speed up the speed shown will be slower then you are actually going. This results in about a  +/- 5 MPH discrepancy to the displayed speed. No matter how much I tried to tune PID I could not figure it out.
3. Since PID is dependent on timing, you cannot tune the PID while printing the debug messages (this is why we had to cancel the meeting the day of our beta version). This is because when you print the debug messages, the loop slows down a lot (10's - 100's of times slower). This results in the tuned PID being way too aggressive after disabling the debug logs.
4. The delay that I added at the end of the main loop may not be necessary as the main loop runs at a fairly consistent speed. It is there because PID like the loop to be at a constant interval in order to be stable. Removing it will sacrifice PID stability (may over or underestimate) but will improve overall responsiveness. From my experience, the impact from removing it is negligible. I kept it in there as it seemed to be the correct way of doing things.
5. During the showcase we found that if you cancel out from the wizard instead of finishing it, the microcontroller gets stuck in that mode. I have asked the app people to send D:F and P:0 when they cancel out of the wizard, but i don't know if they will get to it. For now if it gets stuck in that mode just press the reset button on the microcontroller to have it restart and everything should work fine.
6. Right before the showcase we found out that the app will crash if the data sent from the microcontroller to the phone upon connecting is invalid/corrupted. This typically happens if the app crashes while sending the data to the microcontroller but there are other reasons why it could happen. To fix this you will either need to program it manually using a bluetooth serial terminal app  or just run the reset script that I will include in the repo. I have asked the app people to simply ignore corrupted data so this won't be an issue, but again I don't know if they will get to it.
7. Since we only have a maximum of 4 magnets for each speed sensor the resolution of the speed is somewhat estimated. We recommend for best performance to put 4 magnets if you are reading the speed on the axel or after the final drive gearing. If it is before the final drive gearing then having less magnets is fine.
8. If you decide to source your own magnets please do not put more than 20 magnets if reading from the axel, and no more than 4 if reading before the final drive gear change. This is due to the fact that the microcontroller can only handle values up to a certain size. If the numbers get too big weird things happen.