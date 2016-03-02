# PM2.5 

It is an android application for PM2.5 detection and reporting.

It contains features as below:

1.	Basic Http Restful services like Login, Register, Change password, email, ect.

2.	Pull and push PM 2.5 density and other environmental related information from and to our server.

3.	An algorithm for calculating the number of PM2.5 breathed in and plot 12 different graphs for visualization.

4.	Intelligent health reminder based on your environmental information and your health state.

5.	Personal state detections by using the mobile built-in sensor, such as estimating entrained-air volume when you are walking, running and playing sports.

6.	Map function for better viewing polluted environment, but it is only available in China Mainland.

7.	Embedded Devices Connection --- the affiliated wristband and 803 PM2.5 indoor detection device by using Bluetooth and UDP protocal.

8.	Cache mechanism for reducing redundant calculation and too many accesses to the database.

9.	A Thread pool for network operations by using Volley Framework.

## Development
Please add your own applied baidu map API KEY in the `pM/src/main/AndroidManifest.xml` like this:
```
<application
    android:name=".MyApplication"
    ...
    >

    ...

    <!--<meta-data-->
        <!--android:name="com.baidu.lbsapi.API_KEY"-->
        <!--android:value="In8U2gwdA6i5Q0lyDHne342u">-->
    <!--</meta-data>-->

    ...

</application>
```


## Licence: All Rights Reserved. 

It is ok for personal learning and discussion.

Please don't use the source code in any commercial projects.

Any questions or comments, please contact me liuhaodong0828@gmail.com

(please star it after you clone it and like it.「^_^」)
