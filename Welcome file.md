<!DOCTYPE html>
<html>

<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Welcome file</title>
  <link rel="stylesheet" href="https://stackedit.io/style.css" />
</head>

<body class="stackedit">
  <div class="stackedit__left">
    <div class="stackedit__toc">
      
<ul>
<li><a href="#microcontroller-software-technical-manual">Microcontroller Software Technical Manual</a>
<ul>
<li><a href="#microprocessor-atmega328p">Microprocessor (ATMega328P)</a></li>
<li><a href="#code-layout">Code Layout</a></li>
<li><a href="#main-code">Main Code</a></li>
<li><a href="#interrupts">Interrupts</a></li>
<li><a href="#wizards">Wizards</a></li>
<li><a href="#bluetooth">Bluetooth</a></li>
<li><a href="#equations">Equations</a></li>
<li><a href="#issuesthings-to-note">Issues/Things to Note</a></li>
<li><a href="#glossary">Glossary</a></li>
</ul>
</li>
</ul>

    </div>
  </div>
  <div class="stackedit__right">
    <div class="stackedit__html">
      <h1 id="microcontroller-software-technical-manual">Microcontroller Software Technical Manual</h1>
<p>Here we will talk about the code in great depth.</p>
<h2 id="microprocessor-atmega328p">Microprocessor (ATMega328P)</h2>
<p>Before we begin we need to talk about the hardware features that the microprocessor has built in. We will only mention the parts relevant to the codebase.</p>
<h3 id="clock">Clock</h3>
<p>Our Microprocessor has a 16 MHz clock connected to it (produce a signal with 16,000,000 cycles per second)</p>
<h3 id="timercounter">Timer/Counter</h3>
<p>A Timer/Counter is a register that counts up automatically when some sort of signal is provided to it. If the signal provided is of a consistent frequency it becomes a timer that counts up at set intervals. These counters do not take up time on the main CPU so they can run separately without disturbing the main code.</p>
<p>Each one of these Timer/Counters consists of the main register that stores the count as well as a prescaler and some auxiliary registers to program the counter.</p>
<p>From this point forward we will use the words Timer and Counter interchangeably to reference these devices</p>
<p>On our processor we have three timers (T0, T1, T2). Below is a flowchart of the processes that drive these three timers</p>
<img src="https://github.com/AMDHome/Mechanical-Speedometer-Converter/blob/master/assets/High%20Level%20Diagram/Software%20Workflow%20Hardware.png?raw=true" alt="Hardware Processes Flowchart" width="60%" height="60%">
<h4 id="t0">T0</h4>
<p>This is a 8-bit timer that is used to count the number of holes that the motor encoder passes by. Every time it sees a hole the value in T0 gets incremented by 1.</p>
<h4 id="t1">T1</h4>
<p>This is a 16-bit timer that is configured to operate in 10-bit mode.</p>
<p>This timer has a prescaler of 1 so every time the clock cycles the counter will increment by 1. Since it is operating in 10-bit mode it will reset to 0 after reaching 1023 <code>(2^10 - 1)</code>.</p>
<p>We use this timer to produce our PWM signal. We do this by setting a number between 0 and 1024 in one of the auxiliary registers <code>(OCR1A)</code> and every time the counter is incremented, it will check with the auxiliary register. If the values match then it will turn off Pin 15 on the microcontroller. The pins automatically turn on when the counter overflows to 0.</p>
<h4 id="t2">T2</h4>
<p>This is a 8-bit timer that is used as our system clock. It has a prescaler of 64 (increments by 1 every 64 clock cycles). This is the clock that we use when we need to calculate the amount of time that has passed.</p>
<p>Each count in this timer is equal to 4 microseconds. You can obtain this number by dividing the prescaler by the clock frequency <code>(64 / 16,000,000 = 0.000004)</code>.</p>
<p>Since this is an 8-bit timer, the timer can count up to 255 <code>(2^8 - 1)</code>. This means that the total amount of time that the timer can count is 1.024 milliseconds <code>(4 µs * 256 = 1024 µs)</code>. To combat this short amount of time. Every time the counter counts to 1024 µs, an interrupt is called to record the overflow. This allows us to keep track of longer amounts of time. The overflow function <code>ISR(TIMER2_OVF_vect);</code> can be found at <code>wiring2.cpp:36</code>. This function also helps keep track of other things that we need to time, which will be discussed later in the interrupt section<br>
</p>
<h3 id="analog-comparator">Analog Comparator</h3>
<p>A comparator is a device that takes in two voltages and outputs a digital signal <code>(HIGH/LOW)</code>. Our processor has a comparator built into it on pins 12 and 13. We use this comparator because our speed sensor’s schmitt trigger was built using a spare op-amp instead of a comparator. This means that the schmitt trigger output is an analog square wave that has some noise. We then just use the built in comparator to convert the signal to digital by checking to see if it goes above 3.3v.<br>
</p>
<h2 id="code-layout">Code Layout</h2>
<p>All of our code is put into 5 files. Below are the different parts of code that are in each file</p>
<ul>
<li><code>controller.ino</code>: Setup and Main Loop</li>
<li><code>BTComms.h/cpp</code>: All bluetooth I/O is taken care by these two files</li>
<li><code>wiring2.h/cpp</code>: All timing functions are in this file. It is a modified version of arduino’s original <code>wiring.cpp</code> and timing functions<br>
</li>
</ul>
<h2 id="main-code">Main Code</h2>
<h3 id="overview">Overview</h3>
<p>This is the most straightforward part of the code. When the device starts up it runs <code>Setup()</code> once and then it runs <code>Loop()</code> over and over again.</p>

<h3 id="controller.inosetup"><code>controller.ino</code>:<code>Setup();</code></h3>
<p>Here we set up the hardware that we will be using. We set up the timers and analog comparator discussed before by setting bits to their respective settings registers. Information for this can be found in the ATMega328P Manual. This manual can be found <a href="https://www.sparkfun.com/datasheets/Components/SMD/ATMega328.pdf">here</a>.</p>
<p>We also set the pins to the proper mode as well as load in previously stored values for the variables.</p>

<h3 id="controller.inoloop"><code>controller.ino</code>:<code>Loop();</code></h3>
<p>Here we run the bulk main processes. Below is a flowchart of the entire loop process:</p>
<p><img src="https://github.com/AMDHome/Mechanical-Speedometer-Converter/blob/master/assets/High%20Level%20Diagram/Software%20Workflow%20Main.png?raw=true" alt="Main Loop"></p>
<p>Below is a description of the process in text form:<br>
<code>The flowchart above includes the bluetooth processing portion. We have left that out of this section. You may find a description of the bluetooth processes in the bluetooth section below.</code></p>
<ol>
<li>Check bluetooth to see if any signals come in, if so go deal with the signal
<ul>
<li>See bluetooth section below for more details</li>
</ul>
</li>
<li>Calculate current speed
<ul>
<li>Explanation of the equation will be in the equations section below.</li>
</ul>
</li>
<li>Make sure the current speed can be displayed on the speedometer
<ul>
<li>If the current speed is too high then we just show the maximum speed</li>
</ul>
</li>
<li>Calculate the target RPM we want the motor to spin at.
<ul>
<li>If the current speed is 0 we just stop the motor.</li>
<li>If the app put us in a special mode (ie. Wizards), we also stop the motor to prevent undefined behavior</li>
<li>If we manually assign the targetRPM (only in Speedometer Wizard) then use the assigned value as the targetRPM</li>
<li>If neither of the cases above are true then it is just <code>current speed * speedometer ratio</code>. The extra constants are just unit adjustments so we can avoid using floats. More information in the equations section.</li>
</ul>
</li>
<li>Calculate the current RPM
<ul>
<li>Explanation of the equation will be in the equations section below.</li>
</ul>
</li>
<li>Calculate PID and adjust PWM to control motor</li>
<li>Check if it needs to print out the debug logs</li>
<li>Stall for time until it needs to start the next iteration
<ul>
<li>This delay is added in because PID works best when each iteration is about the same time period apart</li>
<li>This delay can be removed to improve responsiveness; however, it may increase the difficulty of tuning the PID.<br>
</li>
</ul>
</li>
</ol>
<h2 id="interrupts">Interrupts</h2>
<h3 id="overview-1">Overview</h3>
<p>We use this section to run code alongside the main loop. These code blocks typically don’t run very often and are much shorter then  the main loop. These code sections are also time sensitive and have a higher priority. They will interrupt the main loop whenever a certain event is triggered.</p>
<p>Here is a flowchart of the interrupts:</p>
<img src="https://github.com/AMDHome/Mechanical-Speedometer-Converter/blob/master/assets/High%20Level%20Diagram/Software%20Workflow%20Interrupts.png?raw=true" alt="Interrupt Processes Flowchart" width="40%" height="40%">
<h3 id="controller.inoisranalog_comp_vect"><code>controller.ino</code>:<code>ISR(ANALOG_COMP_vect);</code></h3>
<p>This interrupt is triggered when a magnet passes over our speed sensor. All it does is increment a counter that tells us how many magnets have passed in this time frame.<br>
If we happen to be in the final drive wizard we will also increment a counter specifically for that wizard to record distance.</p>
<br>
<h3 id="wiring2.inoisrtimer2_ovf_vect"><code>wiring2.ino</code>:<code>ISR(TIMER2_OVF_vect);</code></h3>
<p>This interrupt is triggered whenever Timer2 overflows (every 1.024 ms) and it does 4 things</p>
<ol>
<li>It increments counters to keep track of longer periods of time
<ul>
<li>this is standard for arduino code and is usually done elsewhere</li>
</ul>
</li>
<li>Every 64 calls of this function we will save the values from the speed sensor counter and Timer0 (encoder data) into two seperate circular arrays.
<ul>
<li>This 64 value is kinda like a software prescaler for recording speed data.</li>
<li>This will happen once every 65.536ms or 15.26 samples a second.
<ul>
<li>This 15.26 number is important. From now on we will call this the <strong><code>sample rate</code></strong></li>
</ul>
</li>
<li>Why did we choose this value? it was arbitrary. We felt like collecting data from the sensor 15-ish times a second was enough and the numbers played nice with our equations. This number can be changed but the equations will need to be recalculated.</li>
</ul>
</li>
<li>Reset the values we just read from to 0 to prepare for the next reading</li>
<li>If we are in the final drive wizard reduce the amount of samples that we still need to collect<br>
</li>
</ol>
<h2 id="wizards">Wizards</h2>
<h3 id="overview-2">Overview</h3>
<p>Because we implemented wizards in the app we need to set special states for the device to be in. This is because some of the values will be undefined when the wizards are ran and might damage the speedometer if we don’t put them in these specialized states.</p>
<p>Here is an overview of how the wizards work on the side of the microcontroller.</p>
<ol>
<li>
<p>Speedometer Ratio Wizard</p>
<ul>
<li>In this wizard we expect the user to be the feedback for the calibration</li>
<li>On the app side:
<ul>
<li>The speedometer wizard allows the user to adjust the speedometer ratio until the speedometer reads a predefined speed.</li>
<li>The app will continuously calculate a target RPM based on the inputted speedometer ratio and the predefined speed and send it to the microcontroller</li>
<li>If the speedometer reading is too low, the user needs to increase the speedometer ratio. If it’s too fast, they need to decrease it.</li>
</ul>
</li>
<li>On the microcontroller
<ul>
<li>The bluetooth will read in a targetRPM from the app</li>
<li>It will then skip calculating the targetRPM as to not overwrite the value that was read in.</li>
<li>Using the targetRPM that was read in, it will use PID to adjust itself until it spins at that speed.</li>
<li>The user will then increase or decrease the value depending on what they need for their speedometer.</li>
<li>When the user is happy with the value, the new speedometer will be stored and the microcontroller will be taken out of the Speedometer Calibration mode</li>
</ul>
</li>
</ul>
</li>
<li>
<p>Final Drive Wizard</p>
<ul>
<li>In this wizard we expect the user to drive and hold a constant speed so the microcontroller can sample the sensor’s rate at a specific speed. The microcontroller will then take the rate and speed to calculate a final drive.</li>
<li>On the microcontroller
<ul>
<li>The targetRPM is set to 0 at all times so the speedometer will not run with a potentially faulty final drive value. The main loop will only be used for communicating over bluetooth. Everything else is handled by the two interrupt loops</li>
<li>The bluetooth will read in the start signal when the user has reached sufficient speed and start recording the number of sensor readings in a period of ~10 seconds <code>(153 samples = sample_rate * 10)</code>.</li>
<li>If the app restarts the countdown then it will send the start signal again at which the microcontroller will restart this process.</li>
<li>After 10 seconds the microcontroller will then wait for an average speed from the users phone.</li>
<li>With the number of readings it collected and the average speed, the microcontroller can now calculate the final drive. Once calculated it will send the value back to the phone to be displayed on the menu.</li>
<li>Equations for the final drive ratio will be below in the equations section<br>
</li>
</ul>
</li>
</ul>
</li>
</ol>
<h2 id="bluetooth">Bluetooth</h2>
<h3 id="overview-3">Overview</h3>
<p>During the main loop, the program will check the bluetooth module for any new data, We will now go through the bluetooth section of the main loop here.</p>
<p>Below is the entirety of the main loop again for ease of reference.</p>
<p><img src="https://github.com/AMDHome/Mechanical-Speedometer-Converter/blob/master/assets/High%20Level%20Diagram/Software%20Workflow%20Main.png?raw=true" alt="Main Loop"></p>
<h3 id="basic-communication-format">Basic Communication Format</h3>
<p>All commands sent to our microcontroller follow a very basic format. Below is an example of a command.<br>
<code>S:18000000</code></p>
<p>Format:</p>
<ol>
<li>We start the command off with a capital letter. This is an identifier that tells us how to processes this command.</li>
<li>The second character is always a colon. This is here just for human readability for now. In the future, if you run out of identifiers, you can  increase the start of the command to 2 letters or more and use the colon as a delimiter.</li>
<li>The remainder of the command is the payload. The payload can have anything in it as long as it is less than 23 characters long. This is an arbitrary length that suits our purposes and can be increased in the future if you run out of room.</li>
</ol>
<p>The program in its current state makes a 3 assumptions in the command that it receives:</p>
<ol>
<li>The command follows the format above</li>
<li>The commands ends with a null character <code>'\0'</code></li>
<li>Capital letters can only be at the start of the command or right after a colon.</li>
</ol>
<p>Any command added in the future that does not follow these assumptions will need the bluetooth functions recoded to support it. One such assumption that has already been coded in is the capital letter <code>R</code>. Since this letter appears in the tire size as the radius, we have coded it to not trigger the error checking</p>
<h3 id="reading-the-data-in">Reading The Data In</h3>
<p>We do all of our data receiving in the function <code>BTComms.cpp:recvData();</code></p>
<p>This function reads in the available data one byte/character at a time. For each character read in, it does the following checks:</p>
<ol>
<li>Capital letter:
<ul>
<li>If the byte that it just read in is a Capital letter, then it checks to makes sure that it is in a valid location (see assumption number 3 in the previous section).</li>
<li>If the capital letter does not meet the assumptions from before then it will assume that the ending of the previously read data was corrupted and that this capital letter is the start of a new command. It will then discard all previously read data and set this capital letter to be the first character read</li>
</ul>
</li>
<li>NULL character <code>'\0'</code>:
<ul>
<li>If the byte that was just read in is a NULL byte, the program assumes that the command has terminated.</li>
<li>It will check the first character read to see if it is a capital letter.</li>
<li>If both of these assumptions are met, it will then process the command.</li>
<li>If one or more of the assumptions are broken then it will assume the command is corrupted and delete all previously read data</li>
</ul>
</li>
<li>Anything else:
<ul>
<li>If neither of those cases occour, then it will just contnue to read in data.
<ul>
<li>If there’s nothing more to read, it will go back to running the main loop. When there is something else to read, the program will continue from where it left off.</li>
</ul>
</li>
</ul>
</li>
</ol>
<h3 id="general-commands-format-table">General Commands Format Table</h3>
<p>Here is a list of all commands that are currently sent from the app, what the command does, the data type that we use to store the commands on the microcontroller, as well as some expected values.</p>

<table>
<thead>
<tr>
<th align="center">Command</th>
<th>Description</th>
<th align="center">Data Type</th>
<th>Expected Values*</th>
<th>Example Command</th>
<th align="center">Notes</th>
</tr>
</thead>
<tbody>
<tr>
<td align="center">U</td>
<td>Set <strong>U</strong>nits</td>
<td align="center">Byte</td>
<td>0: KMH, 1: MPH</td>
<td>U:0</td>
<td align="center"></td>
</tr>
<tr>
<td align="center">M</td>
<td>Set <strong>M</strong>ax Speed</td>
<td align="center">Short</td>
<td>0 - 300</td>
<td>M:120</td>
<td align="center">1</td>
</tr>
<tr>
<td align="center">N</td>
<td>Set <strong>N</strong>umber of Magnets</td>
<td align="center">Byte</td>
<td>1 - 4</td>
<td>N:2</td>
<td align="center"></td>
</tr>
<tr>
<td align="center">F</td>
<td>Set <strong>F</strong>inal Drive Ratio</td>
<td align="center">Float</td>
<td>0 - 10,000,000</td>
<td>F:1000000</td>
<td align="center">2</td>
</tr>
<tr>
<td align="center">S</td>
<td>Set <strong>S</strong>peedometer Ratio</td>
<td align="center">Long</td>
<td>0 - 100,000,000</td>
<td>S:18000000</td>
<td align="center">2</td>
</tr>
<tr>
<td align="center">W</td>
<td>Set <strong>W</strong>heel Size</td>
<td align="center">Long : String</td>
<td>0 - 10,000,000 : Metric Tire Size</td>
<td>W:1263341:P205/65R15</td>
<td align="center">3</td>
</tr>
<tr>
<td align="center">P</td>
<td>Put/Exit Microcontroller into S<strong>p</strong>eedometer Ratio Wizard</td>
<td align="center">Byte</td>
<td>0: Start, 1: Stop</td>
<td>P:1</td>
<td align="center"></td>
</tr>
<tr>
<td align="center">T</td>
<td>Set <strong>T</strong>arget RPM (For Speedometer Wizard)</td>
<td align="center">Short</td>
<td>0 - 12,000</td>
<td>T:3000</td>
<td align="center"></td>
</tr>
<tr>
<td align="center">L</td>
<td><strong>L</strong>oad Values (For App Startup)</td>
<td align="center">Byte</td>
<td>1</td>
<td>L:1</td>
<td align="center">4</td>
</tr>
<tr>
<td align="center">D</td>
<td>Start/Stop/Finish Final <strong>D</strong>rive Ratio Wizard</td>
<td align="center">Byte/Long</td>
<td>S: Start, <br> F: Cancel, <br>0 - 500</td>
<td>D:S <br>D:F <br>D:400</td>
<td align="center">5</td>
</tr>
</tbody>
</table><p>Notes:</p>
<ul>
<li>All values except for final drive are stored exactly the same as the way they are received (ie if an adjustment was made then the adjustment will be stored). Final drive is the only value we convert back to the original value.</li>
</ul>
<ol>
<li>Although We could technically store all realistic speed values in a byte (0 - 255) we chose to use a short as we will be multiplying it by 10 for our equations later</li>
<li>These numbers are multiplied by a million <code>(1,000,000)</code> so we don’t have to deal with decimals. Transferring decimals over commands adds another thing to check for which increases runtime.</li>
<li>Wheel size is the only complex command that we have as there are two parts to the payload.
<ul>
<li>The First Part is a large number. This is the circumference of your tire in terms of miles and kilometers then multiplied by a billion (1,000,000,000). This way we can leverage the more powerful phone cpu to do some of the calculations for us.
<ul>
<li>The units that the first number is sent in is the same as the units that you set for the</li>
</ul>
</li>
<li>The Second Part is the tire size as regular text. This is what we send back to the phone when you load the app.</li>
</ul>
</li>
<li>The load command is a special command. Once received, the microcontroller will send back a command to the phone in the following format. <code>L:Units:Max_Speed:Magnets:Final_Drive_Ratio:Speedometer_Ratio:Wheel_Size_Text</code>
<ul>
<li>Ex <code>L:0:120:2:1000000:18000000:P205/65R15</code></li>
</ul>
</li>
<li>This command has 3 different possible payloads:
<ul>
<li>If it receives an S it will put the device into the Final Drive Ratio Calibration Mode</li>
<li>If it receives an F it will take the device out of the Final Drive Ratio Calibration Mode</li>
<li>If it receives a number it will calculate the Final Drive Ratio using the average speed recieved and the distance it recorded (see equations below), and then take it out of the Final Drive Ratio Calibration Mode</li>
</ul>
</li>
</ol>
<h3 id="debugging-commands-format-table">Debugging Commands Format Table</h3>
<p>Here is a list of “Advanced” commands that the microcontroller will understand. These are commands that I have programmed in to use when creating and debugging the device itself. I didn’t remove them as I thought they might be useful in the future for whatever reason. In order to use these commands you must use a Bluetooth Serial Terminal app like <a href="https://play.google.com/store/apps/details?id=project.bluetoothterminal&amp;hl=en_US">this one</a>.</p>

<table>
<thead>
<tr>
<th align="center">Command</th>
<th>Description</th>
<th>Expected Values</th>
<th>Example Command</th>
<th align="center">Notes</th>
</tr>
</thead>
<tbody>
<tr>
<td align="center">B</td>
<td>Enable/Disable Debug Logs</td>
<td>0: Disable, 1: Enable</td>
<td>B:0</td>
<td align="center"></td>
</tr>
<tr>
<td align="center">X</td>
<td>Set P value in the PID Equations</td>
<td>0 - (2^31 - 1)</td>
<td>X:120</td>
<td align="center">1,3</td>
</tr>
<tr>
<td align="center">Y</td>
<td>Set I value in the PID Equations</td>
<td>0 - (2^31 - 1)</td>
<td>Y:2</td>
<td align="center">2,3</td>
</tr>
<tr>
<td align="center">Z</td>
<td>Set D value in the PID Equations</td>
<td>0 - (2^31 - 1)</td>
<td>Z:1</td>
<td align="center">2,3</td>
</tr>
</tbody>
</table><p>Notes:</p>
<ol>
<li>This value is divided by 1,000 in our PID equations.</li>
<li>This value is divided by 10,000 in our PID equations.</li>
<li>This command will not store the PID across reboots. If you wish to keep your PID permanently you will need to modify the source code manually. I did not implement saving PID values into permanent storage as I don’t think they should be modified in a production.</li>
</ol>
<h3 id="programming-the-microcontroller">Programming the Microcontroller</h3>
<p>Now all of the general commands are sent and used via the app that was created for this project; however, if there is something that the app doesn’t do/implement well, you can just a bluetooth serial terminal app to program it manually. You will then not be constrained by our app. Any bluetooth serial terminal app will work. The one that I use is linked above (and linked <a href="https://play.google.com/store/apps/details?id=project.bluetoothterminal&amp;hl=en_US">here</a> again for your convenience).<br>
</p>
<h2 id="equations">Equations</h2>
<h3 id="overview-4">Overview</h3>
<p>Here I will derive out all the equations that I used in the microcontrollers.</p>
<p>Some of the equations look weird and it is because floating points (decimals) are very costly (timewise) for this CPU. Because of this I will multiply an adjustment value in order to do the calculations. This value will be divided out near the end to preserve as many significant figures as possible. I know I probably should’ve used a fixed point library to do it for me instead of multiplying my own adjustment ratio, but the equations were simple enough and the library adds time and complexity to the runtime.</p>
<h3 id="on-the-phone">On The Phone</h3>
<h4 id="wheel-size">Wheel Size:</h4>
<p>We will use the generic equation to convert metric wheel sizes to circumference in inches.</p>
<img src="https://latex.codecogs.com/gif.latex?Circ\;(in)=\left(\frac{x\cdot&amp;space;y&amp;space;\cdot2}{2540}+z\right)\cdot&amp;space;pi" title="Circ\;(in)=\left(\frac{x\cdot y \cdot2}{2540}+z\right)\cdot pi" alt="Wheel Size Equation">
<p>This equation assumes that the first set of numbers is x, second set is y, and thrid set is z<br>
<code>(Ex. In P205/55R15 -&gt; x = 205, y = 55, z= 15)</code></p>
<p>From there we will convert the circumference to the value we send. The value sent is the circumference in miles or kilometers (depending on the units selected), and then multiplied by a billion. We have the values differ here so that the microcontroller does not have to deal with units at all. If you look through the equations, they are all unit agnostic and our variables are Speed Per Hour (SPH) instead of MPH or KPH.</p>
<img src="https://latex.codecogs.com/svg.latex?\inline&amp;space;\dpi{300}&amp;space;\large&amp;space;\begin{align*}&amp;space;Sent\;Value\;(km)&amp;=Circ\;(in)\cdot\frac{2.54\;cm}{1\;in}\cdot\frac{1\;km}{100000\;cm}*1000000000\\&amp;space;\\&amp;space;&amp;=\frac{Circ\;(in)\;\cdot2.54}{100000}*1000000000&amp;space;\end{align*}" title="\large \begin{align*} Sent\;Value\;(km)&amp;=Circ\;(in)\cdot\frac{2.54\;cm}{1\;in}\cdot\frac{1\;km}{100000\;cm}*1000000000\\ \\ &amp;=\frac{Circ\;(in)\;\cdot2.54}{100000}*1000000000 \end{align*}" alt="Sent km Equation">

<img src="https://latex.codecogs.com/svg.latex?\inline&amp;space;\dpi{300}&amp;space;\large&amp;space;\begin{align*}&amp;space;Sent\;Value\;(mi)&amp;=Circ\;(in)\cdot\frac{1\;mi}{63360\;in}*1000000000\\&amp;space;\\&amp;space;&amp;=\frac{Circ\;(in)}{63360}*1000000000&amp;space;\end{align*}" title="\large \begin{align*} Sent\;Value\;(mi)&amp;=Circ\;(in)\cdot\frac{1\;mi}{63360\;in}*1000000000\\ \\ &amp;=\frac{Circ\;(in)}{63360}*1000000000 \end{align*}" alt="Sent mi Equation">
<h3 id="on-the-microcontroller">On The Microcontroller</h3>
<h4 id="time-for-1-sample">Time For 1 Sample</h4>
<p>So, I’ve talked a bit about the magic numbers 65.536ms and 15.26 samples a second. These numbers are used to record the speed data of both the vehicles current speed as well as the motors current RPM. To get these values we use the system clock speed of <code>16,000,000MHz</code>, the hardware prescaler value for Timer2 <code>64 Cycles/Increment</code>, the max value for Timer2 <code>256 Increments/OVF</code>, and the software prescaler we set of  <code>64 OVF / Sample</code></p>
<img src="https://latex.codecogs.com/svg.latex?\inline&amp;space;\dpi{300}&amp;space;\large&amp;space;\begin{align*}&amp;space;Sample\;Rate&amp;=\frac{16000000\;cycles}{1\;s}\cdot\frac{1\;inc.}{64\;cycles}\cdot\frac{1\;ovf}{256\;inc}\cdot\frac{1\;sample}{64\;ovf}\\&amp;space;\\&amp;space;&amp;=\frac{15625}{1024}\approx15.26\;(Samples/Second)&amp;space;\end{align*}" title="\large \begin{align*} Sample\;Rate&amp;=\frac{16000000\;cycles}{1\;s}\cdot\frac{1\;inc.}{64\;cycles}\cdot\frac{1\;ovf}{256\;inc}\cdot\frac{1\;sample}{64\;ovf}\\ \\ &amp;=\frac{15625}{1024}\approx15.26\;(Samples/Second) \end{align*}" alt="Sample Rate Equation">
<p>To get the time per sample just take the inverse of the value above.<br>
On the microcontroller we will stick with the fractional values for maximum accuracy.</p>
<h4 id="the-inratio">The “InRatio”</h4>
<p>Everytime we calculate the speed of the vehicle, we need to use the Number of Magnets, Final Drive, and Wheel Size. Since all of these should be constants There’s no point in calculating it every time; therefore, we have a variable called InRatio just for a precalculated value of these three constants to save time during calculations.</p>
<p>Since our generic formula for calculating vehicle speed is:</p>
<img src="https://latex.codecogs.com/svg.latex?\inline&amp;space;\dpi{300}&amp;space;\large&amp;space;\begin{align*}&amp;space;Vehicle\;Speed&amp;=&amp;space;\frac{Number\;of\;Magnets\;Passed}{Unit\;of\;time}\cdot\frac{1\;Rotation\;(S.S.)}{Number\;of\;Magnets}\cdot\frac{Rotations\;(W)}{Rotations\;(S.S.)}\cdot\frac{Wheel\;Circ.\;(Dist.)}{1\;Rotations\;(W)}=\frac{Distance}{Unit\;of\;Time}&amp;space;\end{align*}" title="\large \begin{align*} Vehicle\;Speed&amp;= \frac{Number\;of\;Magnets\;Passed}{Unit\;of\;time}\cdot\frac{1\;Rotation\;(S.S.)}{Number\;of\;Magnets}\cdot\frac{Rotations\;(W)}{Rotations\;(S.S.)}\cdot\frac{Wheel\;Circ.\;(Dist.)}{1\;Rotations\;(W)}=\frac{Distance}{Unit\;of\;Time} \end{align*}" alt="Generic Speed Formula">
<p>As we can see here the last three terms in our dimensional analysis are basically our Number of Magnets, Final Drive, and Wheel Size respectively. We will pull those three terms our and call it our inratio.</p>
<img src="https://latex.codecogs.com/svg.latex?\inline&amp;space;\dpi{300}&amp;space;\large&amp;space;\begin{align*}&amp;space;InRatio&amp;=&amp;space;\frac{1\;Rotation\;(S.S.)}{Number\;of\;Magnets}\cdot\frac{Rotations\;(W)}{Rotations\;(S.S.)}\cdot\frac{Wheel\;Circ.\;(Dist.)}{1\;Rotations\;(W)}\;/\;4=\frac{Wheel\;Circ.}{Number\;of\;Magnets\cdot&amp;space;Final\;Drive}\;/\;4&amp;space;\end{align*}" title="\large \begin{align*} InRatio&amp;= \frac{1\;Rotation\;(S.S.)}{Number\;of\;Magnets}\cdot\frac{Rotations\;(W)}{Rotations\;(S.S.)}\cdot\frac{Wheel\;Circ.\;(Dist.)}{1\;Rotations\;(W)}\;/\;4=\frac{Wheel\;Circ.}{Number\;of\;Magnets\cdot Final\;Drive}\;/\;4 \end{align*}" alt="InRatio Formulia">
<p>The InRatio is divided by 4 to make sure all numbers don’t get too big later on.</p>
<h4 id="current-speed">Current Speed</h4>
<p>The way we calculate the current speed is by taking 16 samples of data (~1 s) and using it to find the angular velocity. Once we have that all we need to do is multiply the wheel size and divide out the adjustments we made before. The samples of data tell us how many magnets have passed in each time period, and are stored in a circular array of size 16 called speedCtr.</p>
<p>Since this is where we calculate the actual speed of our vehicle we will need to divide by a billion to compensate for the adjustments on our wheel size (see wheel size equation). We will also multiply the result by 10 so we have a resolution of 0.1 SPH without worrying about the decimal places.</p>
<p>Starting from the equation above, we can pull out InRatio and use basic algebra to simplify the equation to:</p>
<img src="https://latex.codecogs.com/svg.latex?\inline&amp;space;\dpi{300}&amp;space;\large&amp;space;\begin{align*}&amp;space;Current\;Speed\;Per\;Hour\;(SPH\cdot10)&amp;=&amp;space;\frac{Number\;of\;Magnets\;Passed}{Unit\;of\;time}\cdot\frac{1\;Rotation\;(S.S.)}{Number\;of\;Magnets}\cdot\frac{Rotations\;(W)}{Rotations\;(S.S.)}\cdot\frac{Wheel\;Circ.\;(Dist.)}{1\;Rotations\;(W)}\cdot\frac{1}{1000000000}\cdot10\\&amp;space;\\&amp;space;&amp;=\frac{\sum&amp;space;speedCtr[i]\;(magnets)}{Number\;of\;samples}\cdot\frac{15625\;samples}{1024\;s}\cdot\left(InRatio\cdot4\right)\cdot\frac{3600\;s}{1\;hr}\cdot\frac{1}{1000000000}\cdot10\\&amp;space;\\&amp;space;&amp;=\frac{\sum&amp;space;speedCtr[i]\;(magnets)\cdot&amp;space;InRatio\cdot9}{Number\;of\;samples\cdot1024}\\&amp;space;\end{align*}" title="\large \begin{align*} Current\;Speed\;Per\;Hour\;(SPH\cdot10)&amp;= \frac{Number\;of\;Magnets\;Passed}{Unit\;of\;time}\cdot\frac{1\;Rotation\;(S.S.)}{Number\;of\;Magnets}\cdot\frac{Rotations\;(W)}{Rotations\;(S.S.)}\cdot\frac{Wheel\;Circ.\;(Dist.)}{1\;Rotations\;(W)}\cdot\frac{1}{1000000000}\cdot10\\ \\ &amp;=\frac{\sum speedCtr[i]\;(magnets)}{Number\;of\;samples}\cdot\frac{15625\;samples}{1024\;s}\cdot\left(InRatio\cdot4\right)\cdot\frac{3600\;s}{1\;hr}\cdot\frac{1}{1000000000}\cdot10\\ \\ &amp;=\frac{\sum speedCtr[i]\;(magnets)\cdot InRatio\cdot9}{Number\;of\;samples\cdot1024}\\ \end{align*}" alt="SPHr Equation Derivation">
<p>We have left the Number of Samples as a variable instead of simplifying the number into the equation so that if you ever want to change the number of samples recorded all you have to do is change the variable <code>MAX_RECORD</code> at the top of <code>controller.ino</code> instead of having to recalculate the equation once more.</p>
<p>We then do some <a href="%5Bhttps://en.wikipedia.org/wiki/Exponential_smoothing%5D">exponential smoothing</a> to compensate for the relatively low resolution of having so few magnets for our speed sensor.</p>
<p>Note: Decreasing the number of samples yields less resolution to the speed of the vehicle, but will improve response times. Increasing does the opposite. You can increase the number of magnets to compensate for the loss in resolution.</p>
<h4 id="target-rpm-dc-motor">Target RPM (DC Motor)</h4>
<p>This one is fairly straightforward. When we get our speed per hour all we have to do is multiply it by the speedometer ratio to get the required motor RPM.</p>
<img src="https://latex.codecogs.com/svg.latex?\inline&amp;space;\dpi{300}&amp;space;\large&amp;space;\begin{align*}&amp;space;targetRPM\cdot10&amp;=Current\;Speed\;(SPH)\cdot\frac{Number\;of\;RPMs}{1\;SPH}\cdot\frac{1}{1000000}&amp;space;\end{align*}" title="\large \begin{align*} targetRPM\cdot10&amp;=Current\;Speed\;(SPH)\cdot\frac{Number\;of\;RPMs}{1\;SPH}\cdot\frac{1}{1000000} \end{align*}" alt="targetRPM Equation Derivation">
<p><span class="katex--display"><span class="katex-display"><span class="katex"><span class="katex-mathml"><math><semantics><mrow><mi>t</mi><mi>a</mi><mi>r</mi><mi>g</mi><mi>e</mi><mi>t</mi><mi>R</mi><mi>P</mi><mi>M</mi><mo>⋅</mo><mn>10</mn><mo>=</mo><mi>C</mi><mi>u</mi><mi>r</mi><mi>r</mi><mi>e</mi><mi>n</mi><mi>t</mi>&amp;ThickSpace;<mi>S</mi><mi>p</mi><mi>e</mi><mi>e</mi><mi>d</mi>&amp;ThickSpace;<mo>(</mo><mi>S</mi><mi>P</mi><mi>H</mi><mo>)</mo><mo>⋅</mo><mfrac><mrow><mi>N</mi><mi>u</mi><mi>m</mi><mi>b</mi><mi>e</mi><mi>r</mi>&amp;ThickSpace;<mi>o</mi><mi>f</mi>&amp;ThickSpace;<mi>R</mi><mi>P</mi><mi>M</mi><mi>s</mi></mrow><mrow><mn>1</mn>&amp;ThickSpace;<mi>S</mi><mi>P</mi><mi>H</mi></mrow></mfrac><mo>⋅</mo><mfrac><mn>1</mn><mn>1000000</mn></mfrac></mrow><annotation encoding="application/x-tex">
targetRPM\cdot10=Current\;Speed\;(SPH)\cdot\frac{Number\;of\;RPMs}{1\;SPH}\cdot\frac{1}{1000000}
</annotation></semantics></math></span><span class="katex-html" aria-hidden="true"><span class="base"><span class="strut" style="height: 0.87777em; vertical-align: -0.19444em;"></span><span class="mord mathit">t</span><span class="mord mathit">a</span><span class="mord mathit" style="margin-right: 0.02778em;">r</span><span class="mord mathit" style="margin-right: 0.03588em;">g</span><span class="mord mathit">e</span><span class="mord mathit">t</span><span class="mord mathit" style="margin-right: 0.00773em;">R</span><span class="mord mathit" style="margin-right: 0.13889em;">P</span><span class="mord mathit" style="margin-right: 0.10903em;">M</span><span class="mspace" style="margin-right: 0.222222em;"></span><span class="mbin">⋅</span><span class="mspace" style="margin-right: 0.222222em;"></span></span><span class="base"><span class="strut" style="height: 0.64444em; vertical-align: 0em;"></span><span class="mord">1</span><span class="mord">0</span><span class="mspace" style="margin-right: 0.277778em;"></span><span class="mrel">=</span><span class="mspace" style="margin-right: 0.277778em;"></span></span><span class="base"><span class="strut" style="height: 1em; vertical-align: -0.25em;"></span><span class="mord mathit" style="margin-right: 0.07153em;">C</span><span class="mord mathit">u</span><span class="mord mathit" style="margin-right: 0.02778em;">r</span><span class="mord mathit" style="margin-right: 0.02778em;">r</span><span class="mord mathit">e</span><span class="mord mathit">n</span><span class="mord mathit">t</span><span class="mspace" style="margin-right: 0.277778em;"></span><span class="mord mathit" style="margin-right: 0.05764em;">S</span><span class="mord mathit">p</span><span class="mord mathit">e</span><span class="mord mathit">e</span><span class="mord mathit">d</span><span class="mspace" style="margin-right: 0.277778em;"></span><span class="mopen">(</span><span class="mord mathit" style="margin-right: 0.05764em;">S</span><span class="mord mathit" style="margin-right: 0.13889em;">P</span><span class="mord mathit" style="margin-right: 0.08125em;">H</span><span class="mclose">)</span><span class="mspace" style="margin-right: 0.222222em;"></span><span class="mbin">⋅</span><span class="mspace" style="margin-right: 0.222222em;"></span></span><span class="base"><span class="strut" style="height: 2.05744em; vertical-align: -0.686em;"></span><span class="mord"><span class="mopen nulldelimiter"></span><span class="mfrac"><span class="vlist-t vlist-t2"><span class="vlist-r"><span class="vlist" style="height: 1.37144em;"><span class="" style="top: -2.314em;"><span class="pstrut" style="height: 3em;"></span><span class="mord"><span class="mord">1</span><span class="mspace" style="margin-right: 0.277778em;"></span><span class="mord mathit" style="margin-right: 0.05764em;">S</span><span class="mord mathit" style="margin-right: 0.13889em;">P</span><span class="mord mathit" style="margin-right: 0.08125em;">H</span></span></span><span class="" style="top: -3.23em;"><span class="pstrut" style="height: 3em;"></span><span class="frac-line" style="border-bottom-width: 0.04em;"></span></span><span class="" style="top: -3.677em;"><span class="pstrut" style="height: 3em;"></span><span class="mord"><span class="mord mathit" style="margin-right: 0.10903em;">N</span><span class="mord mathit">u</span><span class="mord mathit">m</span><span class="mord mathit">b</span><span class="mord mathit">e</span><span class="mord mathit" style="margin-right: 0.02778em;">r</span><span class="mspace" style="margin-right: 0.277778em;"></span><span class="mord mathit">o</span><span class="mord mathit" style="margin-right: 0.10764em;">f</span><span class="mspace" style="margin-right: 0.277778em;"></span><span class="mord mathit" style="margin-right: 0.00773em;">R</span><span class="mord mathit" style="margin-right: 0.13889em;">P</span><span class="mord mathit" style="margin-right: 0.10903em;">M</span><span class="mord mathit">s</span></span></span></span><span class="vlist-s">​</span></span><span class="vlist-r"><span class="vlist" style="height: 0.686em;"><span class=""></span></span></span></span></span><span class="mclose nulldelimiter"></span></span><span class="mspace" style="margin-right: 0.222222em;"></span><span class="mbin">⋅</span><span class="mspace" style="margin-right: 0.222222em;"></span></span><span class="base"><span class="strut" style="height: 2.00744em; vertical-align: -0.686em;"></span><span class="mord"><span class="mopen nulldelimiter"></span><span class="mfrac"><span class="vlist-t vlist-t2"><span class="vlist-r"><span class="vlist" style="height: 1.32144em;"><span class="" style="top: -2.314em;"><span class="pstrut" style="height: 3em;"></span><span class="mord"><span class="mord">1</span><span class="mord">0</span><span class="mord">0</span><span class="mord">0</span><span class="mord">0</span><span class="mord">0</span><span class="mord">0</span></span></span><span class="" style="top: -3.23em;"><span class="pstrut" style="height: 3em;"></span><span class="frac-line" style="border-bottom-width: 0.04em;"></span></span><span class="" style="top: -3.677em;"><span class="pstrut" style="height: 3em;"></span><span class="mord"><span class="mord">1</span></span></span></span><span class="vlist-s">​</span></span><span class="vlist-r"><span class="vlist" style="height: 0.686em;"><span class=""></span></span></span></span></span><span class="mclose nulldelimiter"></span></span></span></span></span></span></span></p>
<p>We split the divide by a million into two divide by 1000 to ensure that the multiplication wont go out of bounds while retaining as much resolution as possible.</p>
<h4 id="current-rpm-dc-motor">Current RPM (DC Motor)</h4>
<p>The equation for this is fairly similar to the current speed equation, except we don’t need to convert it to linear velocity. RPM stays at angular velocity. We will also multiply the value by 10 to get a resolution of 0.1 RPM’s</p>
<img src="https://latex.codecogs.com/svg.latex?\inline&amp;space;\dpi{300}&amp;space;\large&amp;space;\begin{align*}&amp;space;Motor\;RPM\cdot10&amp;=&amp;space;\frac{Number\;of\;Holes\;Passed}{Unit\;of\;time}\cdot\frac{1\;Rotation}{Number\;of\;Holes}\cdot10\\&amp;space;\\&amp;space;&amp;=\frac{\sum&amp;space;encoderCtr[i]\;(Holes)}{Number\;of\;samples}\cdot\frac{15625\;samples}{1024\;s}\cdot\frac{1\;Rotation}{20\;Holes}\cdot\frac{60\;s}{1\;min}\cdot10\\&amp;space;\\&amp;space;&amp;=\frac{\sum&amp;space;speedCtr[i]\;(Holes)\cdot46875}{Number\;of\;samples\cdot1024}\cdot10\\&amp;space;\end{align*}" title="\large \begin{align*} Motor\;RPM\cdot10&amp;= \frac{Number\;of\;Holes\;Passed}{Unit\;of\;time}\cdot\frac{1\;Rotation}{Number\;of\;Holes}\cdot10\\ \\ &amp;=\frac{\sum encoderCtr[i]\;(Holes)}{Number\;of\;samples}\cdot\frac{15625\;samples}{1024\;s}\cdot\frac{1\;Rotation}{20\;Holes}\cdot\frac{60\;s}{1\;min}\cdot10\\ \\ &amp;=\frac{\sum speedCtr[i]\;(Holes)\cdot46875}{Number\;of\;samples\cdot1024}\cdot10\\ \end{align*}" alt="currentRPM Equation Derivation">
<p>The array size, and subsequently, number of samples for encoderCtr is 4.</p>
<h4 id="pid-equations">PID Equations</h4>
<p>Our PID equations are pretty standard. Only difference is that, once again, to avoid decimals we multiply values by an adjustment.<br>
At the top of <code>controller.ino</code> you will find the pid values in whole numbers. If you scroll down to line 141-143, you can find these equations:</p>
<pre class=" language-c"><code class="prism  language-c">    pid_p <span class="token operator">=</span> KP <span class="token operator">*</span> error <span class="token operator">/</span> <span class="token number">1000</span><span class="token punctuation">;</span>
    pid_i <span class="token operator">+</span><span class="token operator">=</span> KI <span class="token operator">*</span> error <span class="token operator">/</span> <span class="token number">10000</span><span class="token punctuation">;</span>
    pid_d <span class="token operator">=</span> KD <span class="token operator">*</span> <span class="token punctuation">(</span>error <span class="token operator">-</span> oldErr<span class="token punctuation">)</span> <span class="token operator">/</span> <span class="token number">10000</span><span class="token punctuation">;</span>
</code></pre>
<p>PID values are typically less than 1 so to compensate for it, we multiply and then divide the division in these lines of code will tell you the coefficients. (ex. KP is 11 so the P coefficient is actually 0.011)<br>
</p>
<h2 id="issuesthings-to-note">Issues/Things to Note</h2>
<p>While we try our best to make it perfect, there are some issues and intricacies that we found last minute. We will try to get the issues solved, but I cannot promise as I have no control over my teammates. Not all the things in this list are necessarily issues. Some of them are things to keep in mind as they don’t really matter, but in a perfect world none of these things would exist.  ~Samuel</p>
<p>Notes:</p>
<ol>
<li>You <strong>must</strong> set your units before setting your wheel size since the calculations are done via the phone based on the units. If you set your units after then the wheel size might be in the wrong units as it does not get recalculated/resent.</li>
<li>This might be due to my lack of experience with PID, but the motor for the speedometer seems to run slow when accelerating and run fast while decelerating. This means that if you slow down to a particular speed it will show a speed higher than what you are currently at, when you speed up the speed shown will be slower then you are actually going. This results in about a  +/- 5 MPH discrepancy to the displayed speed. No matter how much I tried to tune PID I could not figure it out.</li>
<li>Since PID is dependent on timing, you cannot tune the PID while printing the debug messages (this is why we had to cancel the meeting the day of our beta version). This is because when you print the debug messages, the loop slows down a lot (10’s - 100’s of times slower). This results in the tuned PID being way too aggressive after disabling the debug logs.</li>
<li>The delay that I added at the end of the main loop may not be necessary as the main loop runs at a fairly consistent speed. It is there because PID like the loop to be at a constant interval in order to be stable. Removing it will sacrifice PID stability (may over or underestimate) but will improve overall responsiveness. From my experience, the impact from removing it is negligible. I kept it in there as it seemed to be the correct way of doing things.</li>
<li>During the showcase we found that if you cancel out from the wizard instead of finishing it, the microcontroller gets stuck in that mode. I have asked the app people to send D:F and P:0 when they cancel out of the wizard, but i don’t know if they will get to it. For now if it gets stuck in that mode just press the reset button on the microcontroller to have it restart and everything should work fine.</li>
<li>Right before the showcase we found out that the app will crash if the data sent from the microcontroller to the phone upon connecting is invalid/corrupted. This typically happens if the app crashes while sending the data to the microcontroller but there are other reasons why it could happen. To fix this you will either need to program it manually using a bluetooth serial terminal app  or just run the reset script that I will include in the repo. I have asked the app people to simply ignore corrupted data so this won’t be an issue, but again I don’t know if they will get to it.</li>
<li>Since we only have a maximum of 4 magnets for each speed sensor the resolution of the speed is somewhat estimated. We recommend for best performance to put 4 magnets if you are reading the speed on the axel or after the final drive gearing. If it is before the final drive gearing then having less magnets is fine.</li>
<li>If you decide to source your own magnets please do not put more than 20 magnets if reading from the axel, and no more than 4 if reading before the final drive gear change. This is due to the fact that the microcontroller can only handle values up to a certain size. If the numbers get too big weird things happen.</li>
</ol>
<h2 id="glossary">Glossary</h2>
<p>Register - A hardware unit that can store a value of a specific size. Effectively acts like a variable.</p>
<p>Prescaler - A device that scales back the frequency of a signal. <code>Input_Frequency / Prescaler = Output_Frequency</code></p>

    </div>
  </div>
</body>

</html>
