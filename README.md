# optimizely-ab-android-sdk
Master<br/> 
[![Master Status](https://travis-ci.com/optimizely/optimizely-ab-android-sdk.svg?token=gwpzBrYRAoACxHs4ThQT&branch=master)](https://travis-ci.com/optimizely/optimizely-ab-android-sdk)
<br/>
Beta<br/>
[![Beta Status](https://travis-ci.com/optimizely/optimizely-ab-android-sdk.svg?token=gwpzBrYRAoACxHs4ThQT&branch=beta)](https://travis-ci.com/optimizely/optimizely-ab-android-sdk)
<br/>
Devel<br/> 
[![Devel Status](https://travis-ci.com/optimizely/optimizely-ab-android-sdk.svg?token=gwpzBrYRAoACxHs4ThQT&branch=devel)](https://travis-ci.com/optimizely/optimizely-ab-android-sdk)
<br/>
<br/>
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/gradle-extra-configurations-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)
## Overview

Our Optimizely X Android solution allows you to easily run experiments anywhere in an Android application, even Services. The solution includes easy-to-use SDKs for experimenting in your code and tracking conversion events in Optimizely.

To find out more check out the [documentation](https://developers.optimizely.com/x/solutions/sdks/introduction/index.html?language=android&platform=mobile). 

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

## Releasing

The default branch is devel.  Feature branch PRs are automatically made against it. When PRs are reviewed and pass checks they should be squashed and merged into devel.  The version of the SDK in devel should always be the version of the next release plus `-SNAPSHOT`.  

If a beta, or snapshot, build needs to be published simply checkout beta and merge devel.  The beta branch should fast forward.  Push devel and Travis will start.  If the tests pass the build will be sent to our Maven repos on Bintray.  

When a release version needs to be published checkout the master branch and merge devel.  The master branch should be fast forwarded.  Remove the `-SNAPSHOT` from the version in `build.gradle` and commit directly onto master. Push master and if the tests pass Travis will publish the version to our Maven repos on Bintray.  if the version already exists on Bintray the upload will be rejected.  The commit that updates the version should also be tagged with the version number.

Once the next release has been published from the master branch the snapshot version in devel should be bumped to the next targeted version.

*Note:* only Optimizely employees can push to master, beta, and devel branches.

## Contributing
Please see [CONTRIBUTING](CONTRIBUTING.md).

