# optimizely-ab-android-sdk

## Architecture

This project has 5 modules. Each module has source in `<module>/src/main/java`
and test source in `<module>src/main/androidTest`. The build is configured
in the `build.gradle` for each module.  The `settings.gradle` in the project
root declares modules.  The `build.gradle` in the project root has build
config common for all modules.

1. Android SDK
  - Users who want all modules should just declare a dependency on this module
  - This is the outer module that depends on all other modules
  - Handles downloading the Optimizely Data File and building Optimizely objects
  - Delivers the built Optimizely object to listeners and caches it in memory
2. Event Handler
  - Handles dispatching events to the Optimizely Backend
  - Uses a Service so events can be sent without the app being reopened
  - Persists events in a SQLite3 database
  - Required to be implemented by the Optimizely Java core
3. User Experiment Record
  - Optional implementation for Optimizely Java core
  - Makes bucketing persistent
    - Once a user is bucketed in an variation they will remain in that variation
4. Shared
  - Common utils for all modules
5. Test App
  - Built against the source of all modules
  - Simple app showing how to use Android Optimizely

## Developing

### Command Line

1. Clone the repo
  * `git clone git@github.com:optimizely/optimizely-ab-android-sdk.git`
3. Create, or use an existing, Optimizely Custom Project 
  * put a file in `test-app/src/main/res/values/git_ignored_strings.xml`
  * Give it this contents `<resources><string name="optly_project_id">{CUSTOM_PROJECT_ID}</string></resources>`
4. Build the project (from the project root)
  * `./gradlew assemble`
5. Run tests for all modules
  * `./gradlew testAllModules`
  * A device or emulator must be connected
6. Install the test app onto all connected devices and emulators
  * `./gradlew test-app:installDebug`
  * The test app depends on all of the other project modules
  * The modules are built from source
  * Changes in any modules source will be applied to the test app on the next build
7.  Discover more gradle tasks
  * `./gradlew tasks`
  * To see the task of an individual module
    * `./gradlew user-experiment-record:tasks`

### Android Studio

Android Studio is an IDE that wraps gradle (and `adb`).  Everything you can do in Android Studio can be done from command line with gradle and the other android command line tools.  

You can import this project into Android Studio by opening Android Studio and selecting `Import Project` from the first dialog or from the `File` menu.  Simply select the project's root `build.gradle` file and Android Studio will do the rest.

Tests can be run by right clicking the file in the project pane or by clicking the method name in source and selecting run.  You will be prompted to create an AVD or connect a device if one isn't connected.  
