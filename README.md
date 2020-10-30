# Chanu - 4chan android app

[Design document](https://docs.google.com/document/d/1hYCqC_53iYZ7e13pbmbQ3PTtUat7xwBiH4uzGpat-gM/edit#heading=h.jbxv5gqhprjt)

[4chan API](https://github.com/4chan/4chan-API)

[Original Project](https://github.com/grzegorznittner/chanu)

### How to build application

#### Requirements

1.  Android Studio [Android Studio](http://developer.android.com/sdk/index.html)
2.  Android SDK Platform 29 (Android 10)
3.  Android SDK Build-tools 29.0.3

#### Branch

1.  git checkout android-studio

#### Terminal (using gradle)

1.  Run "./gradlew assemledebug" in the root folder of the project
2.  Navigate to app/build/outputs/apk for the apk to install

#### Android Studio

1.  Open Android Studio
2.  Import the following projects (`File > New > Import Project`)
3.  Select source folder and confirm
4.  Run 'app' to install Chanu onto your device
5.  ....
6.  Profit!

#### App Icon

I used the old one as a base, just added a solid color layer as a background, hope you'll like it, just for reference, the color i used is #00c853 [Green A700](https://material.io/resources/color/#!/?view.left=0&view.right=0&primary.color=00C853), the original logo is trimmed with 75% resize in Android Studio image asset
debug icon has same settings with #69F0AE [Green A200](https://material.io/resources/color/#!/?view.left=0&view.right=0&primary.color=69F0AE) as background instead
