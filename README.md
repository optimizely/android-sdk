# optimizely-ab-android-sdk

## Developing

### Command Line

1. Clone the repo
  * `git clone git@github.com:optimizely/optimizely-ab-android-sdk.git`
3. Create, or use an existing, Optimizely Custom Project 
  * put a file in `test-app/src/main/res/values/git_ignored_strings.xml`
  * Give it this contents `<resources><string name="optly_project_id">{CUSTOM_RPOJECT_ID}</string></resources>`
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
    * `/./gradlew user-experiment-record:tasks`

### Android Studio

Android Studio is an IDE that wraps gradle (and `adb`).  Everything you can do in Android Studio can be done from command line with gradle and the other android command line tools.  

You can import this project into Android Studio by opening Android Studio and selecting `Import Project` from the first dialog or from the `File` menu.  Simply select the project's root `build.gradle` file and Android Studio will do the rest.

Tests can be run by right clicking the file in the project pane or by clicking the method name in source and selecting run.  You will be prompted to create an AVD or connect a device if one isn't connected.  
  
