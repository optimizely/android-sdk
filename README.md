# Optimizely Android SDK
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/gradle-extra-configurations-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Build Status](https://travis-ci.org/optimizely/android-sdk.svg?branch=master)](https://travis-ci.org/optimizely/android-sdk)

This repository houses the Android SDK for use with Optimizely Feature Experimentation and Optimizely Full Stack (legacy). The Android SDK depends on the [Optimizely Java SDK](https://github.com/optimizely/java-sdk).

Optimizely Feature Experimentation is an A/B testing and feature management tool for product development teams, letting you experiment at every step. Using Optimizely Feature Experimentation allows for every feature on your roadmap to be an opportunity to discover hidden insights. Learn more at [Optimizely.com](https://www.optimizely.com/products/experiment/feature-experimentation/), or see the [developer documentation](https://docs.developers.optimizely.com/experimentation/v4.0.0-full-stack/docs/welcome).

Optimizely Rollouts is [free feature flags](https://www.optimizely.com/free-feature-flagging/) for development teams. You can easily roll out and roll back features in any application without code deploys, mitigating risk for every feature on your roadmap.

## Get Started

Refer to the [Android SDK's developer documentation](https://docs.developers.optimizely.com/experimentation/v4.0.0-full-stack/docs/android-sdk) for detailed instructions on getting started with using the SDK.

### Requirements
* Android API 14 or higher

### Install the SDK
To add the android-sdk and all modules to your project, include the following in your app's `build.gradle`:

---
**NOTE**

Optimizely previously distributed the Android SDK through Bintray/JCenter. But, as of April 27, 2021, [Bintray/JCenter will become a read-only repository indefinitely](https://jfrog.com/blog/into-the-sunset-bintray-jcenter-gocenter-and-chartcenter/) and the publish repository has been migrated to [MavenCentral](https://mvnrepository.com/artifact/com.optimizely.ab/android-sdk) for the SDK version 3.10.1 and later. Older versions will still be available in JCenter.

---


```
repositories {
	mavenCentral()
  	jcenter()
}

dependencies {
	implementation 'com.optimizely.ab:android-sdk:3.13.4'
}
```

### Samples
A sample code for SDK initialization and experiments:

```
OptimizelyManager optimizelyManager = OptimizelyManager.builder()
            .withSDKKey("my_sdk_key")
            .withDatafileDownloadInterval(TimeUnit.MINUTES.toSeconds(15))
            .build(getApplicationContext());
            
optimizelyManager.initialize(this, null, (OptimizelyClient optimizely) -> {
	OptimizelyClient optimizely = optimizelyManager.getOptimizely();
	
	Variation variation = optimizely.activate("background_experiment", userId);
	
	optimizely.track("sample_conversion", userId);
});

```

## Use the Android SDK

See the Optimizely Feature Experimentation [developer documentation](https://docs.developers.optimizely.com/experimentation/v4.0-full-stack/docs/android-sdk) to learn how to set up your first Android project and use the SDK.

## Architecture

This project includes 5 library modules and a test app.

1. Android SDK
  - Users who want all modules should declare a dependency on this module.
  - This is the outer module that depends on all other modules.
  - This builds Optimizely objects and provides all public APIs.
2. Datafile Handler
  - This handles downloading the datafile from the Optimizely server.
3. Event Handler
  - This handles dispatching events to the Optimizely server.
  - This uses a Service so events can be sent without the app being re-opened.
  - Events remain persistent until dispatching completed successfully.
4. User Profile
  - Persistent bucketing
  - Once a user is bucketed in a variation, the user will remain in that variation.
5. Shared
  - Common utils for all modules
6. Test App
  - A simple app showing how to use the Optimizely Android SDK

## SDK Development

### Command Line

1. Clone the repo
  * `git clone git@github.com:optimizely/android-sdk.git`
2. Create, or use an existing, Optimizely Android project
3. Build the project (from the project root)
  * `./gradlew assemble`
4. Run tests for all modules
  * `./gradlew testAllModules`
  * A device or emulator must be connected.
5. Install the test app onto all connected devices and emulators
  * `./gradlew test-app:installDebug`
  * The test app depends on all of the other project modules.
  * The modules are built from source.
6.  Discover more gradle tasks
  * `./gradlew tasks`
  * To see the task of an individual module: `./gradlew user-profile:tasks`

### Android Studio

Android Studio is an IDE that wraps gradle. Everything you can do in Android Studio can be done from the command line tools.  

You can import this project into Android Studio by opening Android Studio and selecting `Import Project` from the first dialog or from the `File` menu.  Simply select the project's root `build.gradle` file and Android Studio will do the rest.

Tests can be run by right clicking the file in the project pane or by clicking the method name in source and selecting run.  You will be prompted to create an AVD or connect a device if one isn't connected.  

### Contributing

Please see [CONTRIBUTING](CONTRIBUTING.md).

### Credits

First-party code (under android-sdk/, datafile-handler/, event-handler/, shared/, user-profile/) is copyright Optimizely, Inc. and contributors, licensed under Apache 2.0

### Additional Code

This software incorporates code from the following open source projects:

**Optimizely.ab:core-api** [https://github.com/optimizely/java-sdk](https://github.com/optimizely/java-sdk)
License (Apache 2.0): [https://github.com/optimizely/java-sdk/blob/master/LICENSE](https://github.com/optimizely/java-sdk/blob/master/LICENSE)
Additional credits from java-sdk:[https://github.com/optimizely/java-sdk/blob/master/README.md](https://github.com/optimizely/java-sdk/blob/master/README.md)

**Android Logger** [https://github.com/noveogroup/android-logger](https://github.com/noveogroup/android-logger)
License (Public Domain): [https://github.com/noveogroup/android-logger/blob/master/LICENSE.txt](https://github.com/noveogroup/android-logger/blob/master/LICENSE.txt)

### Other Optimzely SDKs

- Agent - https://github.com/optimizely/agent

- C# - https://github.com/optimizely/csharp-sdk

- Flutter - https://github.com/optimizely/optimizely-flutter-sdk

- Go - https://github.com/optimizely/go-sdk

- Java - https://github.com/optimizely/java-sdk

- JavaScript - https://github.com/optimizely/javascript-sdk

- PHP - https://github.com/optimizely/php-sdk

- Python - https://github.com/optimizely/python-sdk

- React - https://github.com/optimizely/react-sdk

- Ruby - https://github.com/optimizely/ruby-sdk

- Swift - https://github.com/optimizely/swift-sdk
