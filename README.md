# Optimizely Android SDK Tutorial

This tutorial enables you to quickly get started in your development efforts to create an Android app with the Optimizely X Android SDK. This SDK package includes a Test App project that runs integration tests with the Android Espresso framework.

![test-app screens](./demo-app-flow.png)

The Test App works as follows:
* The splash screen initializes the Optimizely manager asynchronously, which starts the datafile download.
* Once the datafile is downloaded and the Optimizely manager is started, the Optimizely client is retrieved from the Optimizely manager and used to activate the experiment named `background_experiment`. This buckets the user and sends an impression event.
* The bucketed variation is then used to determine which Activity to display: `VariationAActivity` for **variation_a**, `VariationBActivity` for **variation_b**, or `ActivationErrorActivity` for the control.
* Each of the variation activities includes a button entitled *Test Conversion* that invokes the variation test.
* Clicking that button invokes `optimizelyClient.track()` and sends a conversion event for the event named `sample_conversion`.
* The application then navigates to the conversion page to provide confirmation that a conversion event has been sent.

## Prerequisites
* Android Studio
* Github account configured with [SSH keys](https://help.github.com/articles/connecting-to-github-with-ssh/)

## Quick start
This section shows you how to prepare, build, and run the sample application using both Android Studio and the command line.

### Android Studio
This section provides the steps to open and build the project in Android Studio.

1. Clone or download the **android-sdk** project.
2. Run Android Studio.
3. Select **Open an existing Android Studio project** from Android Studio's splash screen.
4. Navigate to the location where you downloaded the **android-sdk** project in Step 1 and click **Open**.
5. Select **Build** > **Rebuild Project**.
6. Expand the **test-app** sub project in the Project View.
7. Open **test-app** > **java** > **com.optimizely > MyApplication**.
8. Place a breakpoint on the following line within the **OnCreate** method:
```java
OptimizelyManager.Builder builder = OptimizelyManager.builder();
```
9. (Optional) Ensure an Android device is connected.
10. Select **Run** > **Run 'test-app'**. Select your Android device or the emulator.
11. Verify that execution reaches the break point.
12. Resume program execution and verify that the test app appears on the target Android device.

### Command Line
This section provides the steps to build the project and execute its various tests from the command line, and includes commands to discover additional tasks.

1. Ensure an Android device or emulator is connected.

2. Open a terminal window.

2. Clone the repo:
```shell
git clone git@github.com:optimizely/android-sdk.git
```

2. Build the project (from the project root):
```shell
./gradlew assemble
```

3. Run tests for all modules:
```shell
./gradlew testAllModules
```

4. Install the test app on all connected devices and emulators:
```shell
./gradlew test-app:installDebug
```

 **Note:** The test app depends on all of the other project modules. Changes in any modules source will be applied to the test app in the next build.

5. Discover more gradle tasks:
```shell
./gradlew tasks
```

6. Use the following command to see the task of an individual module:
```shell
./gradlew user-profile:tasks
```

## How the Test App was Created
The following subsections provide information about key aspects of the Test App and how it was put together:
* [Modules](#modules)
* [Manifest Permisisons](#manifest-permissions)
* [User Interface and Visual Assets](#user-interface-and-visual-assets)
* [Variation Activity Design](#variation-activity-design)

### Modules
This project has the following modules: 

1. **Android SDK**: contains the Optimizely X Android SDK with the following two primary responsibilities:
 * Handles downloading of the Optimizely datafile and building Optimizely objects.
 * Delivers the compiled Optimizely object to listeners and caches it in memory.

 Developers who want to include all modules in their projects should declare a dependency on this module, as this module contains dependencies on all other modules in the project. 

2. **Event Handler**: handles dispatching events to the Optimizely backend using a Service so that events can be sent without the app being re-opened. Events are persisted in a SQLite3 database. The Optimizely Android SDK core uses a default implementation provided in this module called `DefaultEventHandler`

3. **Datafile Handler**: handles the downloading and caching of the configuration datafile. The Optimizely Android SDK core uses a default implementation provided in this module called `DefaultDatafileHandler`.

4. **User Profile**: makes bucketing persistent, allowing the SDK to determine if a user has already been bucketed for an experiment. Once a user is bucketed in a variation, they remain in that variation. The Optimizely Android SDK core uses a default implementation provided in this module called `DefaultUserProfileService`.

5. **Shared**: contains common utility/helper code for use by all modules.

6. **Test App**: contains a simple app showing how to use the Android Optimizely SDK that was built using all of the modules.

Each module has source in **&lt;module>/src/main/java** and test source in **&lt;module>/src/androidTest**. The build is configured in the `build.gradle` for each module. The `settings.gradle` in the project root declares modules. The `build.gradle` in the project root has build config common for all modules.

For details about the APIs used to develop this sample, see the [documentation](https://docs.developers.optimizely.com/full-stack/docs).

### Manifest Permissions
In the `AndroidManifest.xml file` for the **test-app** project, `uses-permissions` settings are specified to disable the device's security keylock, add a wake lock to prevent the device from sleeping, receive the boot completed event so that events may be rescheduled on reboot, and access the Wi-Fi connectivity state. The manifest also includes two receivers that are used to schedule services.

```xml
<?xml version="1.0" encoding="utf-8"?>

<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="com.optimizely.ab.android.test_app">

    <uses-permission android:name="android.permission.DISABLE_KEYGUARD" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>

    <application
        android:name=".MyApplication"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <service
            android:name=".NotificationService"
            android:exported="false" />

        <activity
            android:name=".SplashScreenActivity"
            android:theme="@style/SplashTheme">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <activity android:name=".VariationAActivity" />
        <activity android:name=".EventConfirmationActivity" />
        <activity android:name=".ActivationErrorActivity" />
        <activity android:name=".VariationBActivity"></activity>

        <!--
           Add these lines to your manifest if you want the datafile download background services to schedule themselves again after a boot
           or package replace.
        -->
         <receiver
             android:name="com.optimizely.ab.android.datafile_handler.DatafileRescheduler"
             android:enabled="true"
             android:exported="false"
             android:permission="android.permission.RECEIVE_BOOT_COMPLETED">
             <intent-filter>
                 <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
                 <action android:name="android.intent.action.BOOT_COMPLETED" />
             </intent-filter>
         </receiver>
        <!--
          Add these lines to your manifest if you want the event handler background services to schedule themselves again after a boot
          or package replace.
        -->
        <receiver
            android:name="com.optimizely.ab.android.event_handler.EventRescheduler"
            android:enabled="true"
            android:exported="false"
            android:permission="android.permission.RECEIVE_BOOT_COMPLETED">
            <intent-filter>
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.net.wifi.supplicant.CONNECTION_CHANGE" />
            </intent-filter>
        </receiver>

    </application>

</manifest>
```

### User Interface and Visual Assets
The following activity layout files are in the **/res/layout** directory:

|Asset                   |Description                                                                                        |
|------------------------|---------------------------------------------------------------------------------------------------|
|`activity_activation_error.xml`|Displayed when an error occurs.|
|`activity_event_confirmation.xml`|Displayed when a test has been confirmed as sent by the app to Optimizely.|
|`activity_splash_screen.xml`|Displays a splash screen during load.|
|`activity_variation_a.xml`|Displays a screen for Variation A.|
|`activity_variation_b.xml`|Displays a screen for Variation B.|
|`fragment_conversion.xml`|Contains the **TEST CONVERSION** button embedded in the two variation activity screens.|


The following vector art files in the **/res/drawable** directory are used as background images for the various activities:

|Asset                   |Description                                                                                        |
|------------------------|---------------------------------------------------------------------------------------------------|
|`ic_background_confirmation.xml`|Contains the background image for the event confirmation screen.|
|`ic_background_error.xml`|Contains the background image for the error screen.|
|`ic_background_varia.xml`|Contains the background image for the Variation A activity.|
|`ic_background_varib_marina.xml`|Contains the background image for the Variation B activity.|
|`ic_optimizely_logo.xml`|Contains the Optimizely logo used on the splash screen activity.|

### Variation Activity Design
The activities for both variation screens consist of text indicating the variation and a button fragment to invoke the test as shown in this screen shot of the Variation A activity:

![Variation A Activity](./VariationAActivity.png)

|Component                        |Description                                                                                                                                 |
|---------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
|`tv_variation_a_text1`       |Contains the text identifying the variation.|
|`tv_variation_a_text2`       |Contains the word "variation".|
|`ConversionFragment`       |References the **TEST CONVERSION** button for inclusion on the screen.|

## Configure Resources
To configure A/B tests:
* [Create the Manager Instance](#create-the-manager-instance)
* [Initialize the Manager](#initialize-the-manager)

### Create the Manager Instance
The code samples in this section are in **test-app/java/com.optimizely.ab.android.test.app/MyApplication.java**.

The `OnCreate()` method in the main application class (`MyApplication`) obtains and uses a builder to create and store a reference to an `OptimizelyManager` object:

```java
...
import com.optimizely.ab.android.sdk.OptimizelyManager;
...

public class MyApplication extends Application {

    ...
    private OptimizelyManager optimizelyManager;

    @Override
    public void onCreate() {
        super.onCreate();

        OptimizelyManager.Builder builder = OptimizelyManager.builder();
        optimizelyManager =  builder.withEventDispatchInterval(60L * 10L)
            .withDatafileDownloadInterval(60L * 10L)
            .withSDKKey("6hmwpgZcRFp36wH5QLK8Sb")
            .build(getApplicationContext());
    }
    ...
}
```

The class makes the reference to the `OptimizelyManager` available to other classes via its `getOptimizelyManager()` helper method:
```java
public class MyApplication extends Application {

    ...

    public OptimizelyManager getOptimizelyManager() {
        return optimizelyManager;
    }
    ...
}
```

### Initialize the Manager
The code samples in this section are also located in **test-app/java/com.optimizely.ab.android.test.app/SplashScreenActivity.java**. [SplashScreenActivity.java](./test-app/java/com.optimizely.ab.android.test.app/SplashScreenActivity.java)

The `onCreate()` method in the `SplashScreenActivity` class acquires a reference to the application and then invokes its `getOptimizelyManager` method to obtain a reference to the `OptimizelyManager` object:

```java
public class SplashScreenActivity extends AppCompatActivity {

    ...
    private OptimizelyManager optimizelyManager;
    private MyApplication myApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // This could also be done via DI framework such as Dagger
        myApplication = (MyApplication) getApplication();
        optimizelyManager = myApplication.getOptimizelyManager();
    }

    ...
}

```

The `onCreate()` method in the `SplashScreenActivity` class initializes the OptimizelyManager instance:
public class SplashScreenActivity extends AppCompatActivity {
    ...
    @Override
    protected void onStart() {
        super.onStart();

        boolean INITIALIZE_ASYNCHRONOUSLY = false;

        ...

        /** Example of using Cache datafile to initialize optimizely client, if file is not present
         in Cache it will be initialized from Raw.datafile.
         **/
        if (!INITIALIZE_ASYNCHRONOUSLY) {
               optimizelyManager.initialize(myApplication, R.raw.datafile);
               optimizelyManager.getOptimizely().getNotificationCenter().addActivateNotificationListener((Experiment experiment, String s,  Map<String, String> map,  Variation variation,  LogEvent logEvent) -> {
                   System.out.println("got activation");
               });
               optimizelyManager.getOptimizely().getNotificationCenter().addTrackNotificationListener((String s, String s1, Map<String, String> map, Map<String, ?> map1, LogEvent logEvent) -> {

                   System.out.println("got track");
               });
               startVariation();
        } else {
            // Initialize Optimizely asynchronously
            optimizelyManager.initialize(this,R.raw.datafile, new OptimizelyStartListener() {

                @Override
                public void onStart(OptimizelyClient optimizely) {
                    startVariation();
                }
            });
        }

    }
    ...
}


The local boolean `INITIALIZE_ASYNCHRONOUSLY` is used to control whether initialization takes place asynchronously or synchronously. By default, `INITIALIZE_ASYNCHRONOUSLY` has been hard coded to false.

## Functionality
* [Perform an A/B Test](#perform-an-a-b-test)
* [Track the Experiment](#track-the-experiment)

### Perform an A/B Test
The `startVariation()` method in the `SplashScreenActivity` class generates a random user ID and then activates the `background_experiment` with that ID. When the Variation is returned, the code invokes Variation's `getKey()` method to determine if it's `variation_a` or `variation_b`. The code then stores the respective Variation Activity class in a new Intent and uses it to start the appropriate Variation Activity for display:

```
java
public class SplashScreenActivity extends AppCompatActivity {
    ...

    private void startVariation(){
        // this is the control variation, it will show if we are not able to determine which variation to bucket the user into
        Intent intent = new Intent(myApplication.getBaseContext(), ActivationErrorActivity.class);

        // Activate user and start activity based on the variation we get.
        // You can pass in any string for the user ID. In this example we just use a convenience method to generate a random one.
        String userId = myApplication.getAnonUserId();
        Variation backgroundVariation = optimizelyManager.getOptimizely().activate("background_experiment", userId);

        // Utility method for verifying event dispatches in our automated tests
        CountingIdlingResourceManager.increment(); // increment for impression event

        // variation is nullable so we should check for null values
        if (backgroundVariation != null) {
            // Show activity based on the variation the user got bucketed into
            if (backgroundVariation.getKey().equals("variation_a")) {
                intent = new Intent(myApplication.getBaseContext(), VariationAActivity.class);
            } else if (backgroundVariation.getKey().equals("variation_b")) {
                intent = new Intent(myApplication.getBaseContext(), VariationBActivity.class);
            }
        }
        startActivity(intent);
        ...
    }
}

```

### Track the Experiment
The code samples in this section are in **test-app/java/com.optimizely.ab.android.test.app/ConversionFragment.java**.

The `ConversionFragment` class is automatically created when `VariationAActivity` or `VariationBActivity` is created. During creation, the class's `onViewCreated()` method is invoked and sets up a listener for button click events:

```java
public class ConversionFragment extends Fragment {

    Button conversionButton;
    ...

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        conversionButton = (Button) view.findViewById(R.id.btn_variation_conversion);

        final MyApplication myApplication = (MyApplication) getActivity().getApplication();
        final OptimizelyManager optimizelyManager = myApplication.getOptimizelyManager();

        conversionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userId = myApplication.getAnonUserId();

                // This tracks a conversion event for the event named `sample_conversion`
                OptimizelyClient optimizely = optimizelyManager.getOptimizely();
                optimizely.track("sample_conversion", userId);

                // Utility method for verifying event dispatches in our automated tests
                CountingIdlingResourceManager.increment(); // increment for conversion event

                Intent intent = new Intent(myApplication.getBaseContext(), EventConfirmationActivity.class);
                startActivity(intent);
            }
        });
    }
}
```

 When the button is clicked and the handler is invoked, the code uses a reference to the `OptimizelyManager` manager object from the application to obtain a reference to the `OptimizelyClient` object. The code then invokes the [Track](https://docs.developers.optimizely.com/full-stack/docs/track) method to track events across the experiment. At the end of the event handler, an `EventConfirmationActivity` is started to inform the user that the test has started.











