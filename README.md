## JUnit4Android
JUnit4Android is an **Android library** project to build Android test runner applications for running **JUnit4 and JUnit3** tests and test suites.

While the Android development tools come with a TestRunner that can only run JUnit3 tests, with JUnit4Android you are free to choose which version
of JUnit you like. This is useful, if you want to use the new features of JUnit4 or you have existing JUnit4 test code that shall be run on Android.

I used JUnit4Android to successfully run the tests of the Spring Framework on Android - and there are many of them!

![JUnit4Android TestRunner](/dthommes/JUnit4Android/raw/master/doc/screenshot-testrunner.png)

### Check out the Sources
`git clone git://github.com/dthommes/JUnit4Android.git`

### License
JUnit4Android is released under version 2.0 of the
[Apache License](http://www.apache.org/licenses/LICENSE-2.0).

## Getting Started
### Prerequisites
JUnit4Android is an Android library project for Eclipse. So you should have Eclipse with the Android Development Tools (ADT) up and running.

### Checkout JUnit4Android
You need the sources of JUnit4Android to use it as an Android library. Check them out or download them with one of your preferred methods.
You can e.g. use a GIT Eclipse-Plugin or the command line like this:

`git clone git://github.com/dthommes/JUnit4Android.git`

### Import the JUnit4Android Eclipse Project
If you have fetched the sources via command line or downloaded them, you will have to add the resulting folder (JUnit4Android) as project
to your Eclipse workspace.
Use `File -> Import... -> Existing Projects into Workspace` and choose the JUnit4Android directory recently created to be imported as `JUnit4Android`
project into your workspace.

### Create an Android TestRunner application
You will use JUnit4Android to create your custom test runner application.

* **Create a new Android Project** (`File -> New -> Android Project`)
* Use a name of your choice.
* Select Android 2.2 or higher as Build Target (At the moment the library uses 2.2).
* Disable the `Create Activity` CheckBox (Junit4Android provides an Activity that you will use.)
* Click Finish to create the project.

* **Add the JUnit4Android Android library:** In the new project's properties (`Right Click on the project -> Properties`) open the `Android` item. In the pane below you can add Android Libraries.
  Press the `Add...` Button and choose `JUnit4Android` in the following popup to link Junit4Android to your project.
* **Add JUnit4Android Elements**: Open your project's `AndroidManifest.xml` and replace the content of the manifest element with that shown below (You can copy & paste
the code between the `JUnit4Android START` and `JUnit4Android END` comments).

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="org.foobar.tests"
    android:versionCode="1"
    android:versionName="1.0" >

    <!-- JUnit4Android START -->
    <uses-sdk android:minSdkVersion="8" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

    <application android:icon="@drawable/ic_launcher" android:label="@string/app_name" >
        <activity android:label="@string/app_name" android:name="org.junit4android.JunitTestRunnerActivity" >
            <intent-filter >
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <!-- Enter your test or test suite's class name here or remove the
                 following element to make <package-name>.AllTests the class to be run as test. -->
            <meta-data android:name="testClass" android:value="org.junit4android.tests.Junit4TestSuite" />
            <!-- Enable autorun by setting this property to 'true' -->
            <meta-data android:name="autorun" android:value="false" />
        </activity>
        <activity android:label="@string/app_name" android:name="org.junit4android.TestResultDetailActivity" />

    </application>
    <!-- JUnit4Android END -->

</manifest>
```

* **Add the JUnit4 library** to your build path:
In your project's Build Path Settings press `Add Library...`, choose `JUnit` and select the `JUnit4` library.

* To test whether everything is setup correctly, **run your Android application**. You should see JUnit4Android's JunitTestRunnerActivity
  being displayed. Use it's `Run Tests` Button to run the *JUnit4TestSuite* example from JUnit4Android's tests package. Some of these tests
  fail and some run successfully.

### Run your own tests

*Note:* JUnit4Android due to Android's limited package introspection capabilities **is not able to run tests by just pointing to a package
containing these tests**. At the moment, JUnit4Android can only execute a test or test suite of which the class name is given in the Android manifest.
So if you have more than one test, you will have to use a test suite to combine and run them with JUnit4Android.

* Link or copy your JUnit tests to your Android application project (e.g. by referencing the Eclipse project or the JAR containing these tests).
* If you want to run more than one test, optionally create a JUnit test suite combining these tests.
You can either look into JUnit4Android's `test-src` folder to find examples or copy the code given below to create a
JUnit4 test suite. Add your test classes within the `Suite.SuiteClasses` Annotation.

```java
package your.package;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({ YourFirstTest.class, YourSecondTest.class })
public class Junit4TestSuite {
}
```

* Enter the qualified name of your test or test suite in the Android manifest of your project.
Please find the meta-data element as shown below and enter the class name within the `value` attribute.

```
<meta-data android:name="testClass" android:value="your.package.YourTestSuite" />
```

**Note:** If you remove the meta-data element, JUnit4Android will automatically use `<package-name>.AllTests` as value for `testClass` where `<package-name>`
is your Android project's package. The JUnit4Android test runner will always display your chosen `testClass` in the first line.

* Additionally you can set the 'autorun' meta-data to 'true'. When starting the test application, it will automatically run all your tests without a need for interaction.

### Start Testing

Your custom TestRunner can now be used to run your tests. Feel free to change the testClass value in the manifest if you want to concentrate
on single tests during development.

### View Test Results in Eclipse

If you are using Eclipse, you can view your test results in Eclipse's builtin JUnit-View (and jump to the source files from there). JUnit4Android stores an XML file with the test results on the SD-Card (in the directory `junit4android/reports`). You can open this file using the
File Explorer View of Eclipse' DDMS perspective (comes with ADT). Just use the 'Open XML File...' button in the upper right corner of this view to open the XML file directly from your device.

**HAPPY TESTING!**

Daniel