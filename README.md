# Optimizely Android SDK
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/gradle-extra-configurations-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Build Status](https://travis-ci.org/optimizely/android-sdk.svg?branch=master)](https://travis-ci.org/optimizely/android-sdk)

## Overview

This repository houses the Android SDK for use with Optimizely Full Stack and Optimizely Rollouts. The Android SDK depends on the [Optimizely Java SDK](https://github.com/optimizely/java-sdk).

Optimizely Full Stack is A/B testing and feature flag management for product development teams. Experiment in any application. Make every feature on your roadmap an opportunity to learn. Learn more at https://www.optimizely.com/platform/full-stack/, or see the [documentation](https://docs.developers.optimizely.com/full-stack/docs).

Optimizely Rollouts is free feature flags for development teams. Easily roll out and roll back features in any application without code deploys. Mitigate risk for every feature on your roadmap. Learn more at https://www.optimizely.com/rollouts/, or see the [documentation](https://docs.developers.optimizely.com/rollouts/docs).

## Getting Started

### Using the SDK
See the [Mobile developer documentation](https://docs.developers.optimizely.com/full-stack/docs/install-sdk-android) to learn how to set
up an Optimizely X project and start using the SDK.

### Requirements
* Android API 14 or higher

### Installing the SDK
To add the android-sdk and all modules to your project, include the following in your app's `build.gradle`:

```
repositories {
  	jcenter()
}

dependencies {
	implementation 'com.optimizely.ab:android-sdk:3.4.+'
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

## Architecture

This project has 5 modules. Each module has source in `<module>/src/main/java`
and test source in `<module>src/main/androidTest`. The build is configured
in the `build.gradle` for each module.  The `settings.gradle` in the project
root declares modules.  The `build.gradle` in the project root has build
config common for all modules.

1. Android SDK
  - Users who want all modules should declare a dependency on this module
  - This is the outer module that depends on all other modules
  - Handles downloading the Optimizely datafile and building Optimizely objects
2. Event Handler
  - Handles dispatching events to the Optimizely backend
  - Uses a Service so events can be sent without the app being re-opened
  - Persists events in a SQLite3 database
  - Required to be implemented by the Optimizely Java core
3. User Profile
  - Optional implementation for Optimizely Java core
  - Makes bucketing persistent
    - Once a user is bucketed in a variation, they will remain in that variation
4. Shared
  - Common utils for all modules
5. Test App
  - Built against the source of all modules
  - Simple app showing how to use Android Optimizely

## Development

### Command Line

1. Clone the repo
  * `git clone git@github.com:optimizely/android-sdk.git`
2. Create, or use an existing, Optimizely Android project
3. Build the project (from the project root)
  * `./gradlew assemble`
4. Run tests for all modules
  * `./gradlew testAllModules`
  * A device or emulator must be connected
5. Install the test app onto all connected devices and emulators
  * `./gradlew test-app:installDebug`
  * The test app depends on all of the other project modules
  * The modules are built from source
  * Changes in any modules source will be applied to the test app on the next build
6.  Discover more gradle tasks
  * `./gradlew tasks`
  * To see the task of an individual module
    * `./gradlew user-profile:tasks`

### Android Studio

Android Studio is an IDE that wraps gradle (and `adb`).  Everything you can do in Android Studio can be done from command line with gradle and the other android command line tools.  

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

