4channer - 4chan android app
========

[Design document](https://docs.google.com/document/d/1hYCqC_53iYZ7e13pbmbQ3PTtUat7xwBiH4uzGpat-gM/edit#heading=h.jbxv5gqhprjt)

[4chan API](https://github.com/4chan/4chan-API)

Applications:
test-app - test application.

### How to build application
#### Terminal
1.  Set ```ANDROID_HOME``` env variable to your android-sdk folder
2.  Add ```$ANDROID_HOME/tools``` and ```$ANDROID_HOME/platform-tools``` to the ```PATH```
3.  Go to application folder, eg. ```$4CHANNER/test-app```
4.  ```ant debug```         <- to build apk file
5.  ```$ANDROID_HOME/platform-tools/adb.exe -d install -r ./bin/PhotoGrid-debug.apk```     <- installs apk to usb connected device
6.  ```ant clean```         <- to clean the project

#### Eclipse
1.  Download [Eclipse ADT](http://developer.android.com/sdk/index.html)
2.  Open Eclipse
3.  Import the following projects (```File > Import > Existing Android Code into Workspace```)
  - ```chanu/ActionBar-PullToRefresh```
  - ```chanu/Gallery2```
  - ```chanu/app```
4.  ```Right click the 4Channer project > Run As > Android Application``` to build and install the apk on your connected device or virtual machine
