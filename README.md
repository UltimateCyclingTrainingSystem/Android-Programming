                                            Android Development

•	The android app was done using the android studio and it consists of 5 main activities as follows:
1.	MainActivitycxc
•	Functionality: Scan for BLE devices and the selected devices are passed on to the Welcome activity.

2.	Welcome
•	Functionality: The user can choose to go to one of the other 4 activities.

3.	PowerChallenge
•	Functionality: Challenge mode where the average power of the cycler is calculated for a certain period of time (e.g. Timer for 5 minutes). 
•	The data (cadence and power) is being received from a microcontroller (Arduino  genuino) via BLE.
•	Heart rate data is also being received from a heart rate sensor via BLE.
•	The cadence and the instantaneous power are being visualized to the user as bar graphs along with the heart rate frequency which is represented as a number. The timer is also being shown on the screen with a start button to start it.

4.	Cycling
•	Functionality: Same as the PowerChallenge activity, the only difference is that the average power of the cycler is being calculated for a period of time from the point where the cycler presses the start button until he presses the stop button.

5.	HighScores
•	Functionality: Keeps track of the average power scores calculated from the PowerChallenge activity.
