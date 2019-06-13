

## How to download and install the app w/ Android Studio


To have the current version of the mobile application on a compatible Android phone you must have Android Studio downloaded. You can find the most recent version of Android Studio on its website,

[https://developer.android.com/studio/](https://developer.android.com/studio/)

Android Studio is the software used to make changes to the mobile application as well as install the mobile application. Once you have Android Studio downloaded and installed you can visit the Github page to download the mobile application itself; you can find the content here,

[https://github.com/AMDHome/Mechanical-Speedometer-Converter](https://github.com/AMDHome/Mechanical-Speedometer-Converter)

Github is a Version Control System (VCS) that is used to track and keep progress of a project. You will find on the Github page for the mobile application that it will have different branches and versions. Download the most up to date branch, typically master. You can download the mobile application with whatever method is preferred such as cloning the repository, but the simplest way would be to download the mobile application as a ZIP file and then extracting the ZIP file (although if done by this method, any changes to the project will not be tracked!).

You are able to open the extracted “app” folder with Android Studio. It is as this point that the mobile application user should plug in their phone to the computer and enable debugging mode, allowing for mobile application installation. The method to enable debugging mode may vary depending on the phone and the phone’s operating system, but a general procedure can be found here,

[https://www.kingoapp.com/root-tutorials/how-to-enable-usb-debugging-mode-on-android.htm](https://www.kingoapp.com/root-tutorials/how-to-enable-usb-debugging-mode-on-android.htm)

Once the “app” folder has been opened in Android Studio, there should be a green arrow in the top of the Android Studio software window that once clicked will prompt the computer user to select a phone to perform the installation. Hitting select and waiting for the load bar on the bottom to complete should complete the mobile application installation!

## Making changes to the mobile application w/ Android Studio

Following the installation guide to the point of opening the “app” folder with Android Studio, you will see a directory on the left tab once the project has been loaded. You will see many folders and files which can be modified, deleted, replaced, or supplemented with new code.

## 6/13/2019 Code Base

This part of the document will explain the existing code base as of 6/13/2019, so that future developers can get a better understanding of the code base. The first three listed items are general but important to the operation and function of the mobile application. The mechanical speedometer application becomes specific to the project’s implementation in the “meterconverter” folder

  

**Build.gradle**  - overlying settings of the program pertaining to build specifics

**AndroidManifest.xml**  - overlying settings of program pertaining to program specifics

**“layout” folder** - In this folder are files that determine the overall look of each page or screen of the mobile application

 - activities - These are the pages that take up the entirety of the phone screen. Different activities are swapped/started which brings up a new page on the phone. 
 - fragments - These are parts of pages.  It can be thought of as a sub-activity. Fragments are their own UI on top of the activity UI allowing for more versatile front end user interface. 
 
“**meterconverter” folder** - In this folder is the code specific to our mobile application.

## Splash Screen

 - SplashScreenSleep.java
 - SplashScreenActivity.java  

When the mobile application first launches it will undergo a splash screen showing the custom underground logo. 

 In SplashScreenSleep.java, the program will hold and lock the program in place for a brief moment before loading the next page. 

In SplashScreenActivity.java the program will load the ‘layout’ file or look of the page, which has the custom underground logo.

## Main Screen

 - MainActivity.java
	 - HomeFragment.java
	 - DataFragment.java
	 - CalibrationFragment.java

After the splash screen completes the mobile application goes to the main screen where all the settings and wizards can be found in the first tab implemented via the **HomeFragment.java** file. While there are two other tabs available they currently have no use.

In HomeFragment.java, the UI will be setup for all the buttons. This includes Bluetooth, calibration settings, wizards. HomeFragment.java also implements calibration setting communication which will be sent via Bluetooth connection. There is also code that allows to check the micro-controller for saved memory so that calibration values can be loaded if it has been previously inputted.  The buttons are organized by Bluetooth, car specifications, speedometer specifications, and speed sensor specifications.

DataFragment.java + CalibrationFragment.java have no real use currently.


## Bluetooth Connection

 - Btconnection.java
 
Typically the first thing to do in the main screen before anything else is that the user should connect to Bluetooth which is controlled by the **Btconnection.java**. Aftering click on the Devices button, it will prompt the user to turn on Bluetooth if not already on. Pressing Find Devices will refresh all Bluetooth connections, where one can be selected.

In Btconnection.java, the bluetooth connection will be setup, specifically a socket that allows for a serial connection between the mobile device and the micro controller. 

## Wizards

Some buttons in the main screen will take the user to a wizard setup. Essentially what the wizards do is guide the user through calibrating the mechanical speedometer converter as if manually inputting each calibration setting. The code that controls each wizard page is found in the **“MeterWizard”** folder.

 - **MeterWizardDriveCheck**, this screen will ask the user whether or not the drive ratio has been calibrated. If not it will go through the calibration, so that it can continue onto the speedometer ratio calibration.
 - **MeterWizardCalibrate**, this screen will take the max speed and divide it in half and ask the user to hold the targeted speed via GPS for a certain amount of time; this feature still needs to be tested.
 - **MeterWizardMagnent**, this screen sets up the calibration for the amount of magnets.
 - **MeterWizardRPM**, this screen sets up up the calibration for target rpm to reach the middle reading for the speedometer. The screen is equipped with increment and decrement buttons. 
 - **MeterWizardRatio**, this screen sets up the calibration for the maximum speedometer reading. 
 - **MeterWizardTireSize**, this screen sets up the calibration for tiresize via popular tire format, P###/##R##.
 - **MeterWizardUnit**, this screen sets up the calibration for the unit of the speedometer.

## General Code Snippets

**Button finding and setting onClick**
Here is the general format for finding a button by Id and then assigning to the button a screen transition. 


	    but_kph = findViewById(R.id.radio_kph);
        but_kph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {        
                Intent wizIntent = new Intent(MeterWizardUnit.this, MeterWizardRatio.class);
                startActivity(wizIntent);
            }
        });
**Loading a Screen**
Here is the general format for loading a screen with its UI/frontend.
        
        @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_wizard_meter_units);
**Bluetooth Serial Connection**	        
Here is the commands to write out and then read in from the Bluetooth connection for the mobile application.

    btSocket.getOutputStream().write("L:1\0".getBytes());
    btSocket.getInputStream().read();
    
    


