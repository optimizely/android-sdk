# Optimizely Android SDK
Master<br/> 
[![Master Status](https://travis-ci.org/optimizely/android-sdk.svg?branch=master)](https://travis-ci.org/optimizely/android-sdk)
<br/>
Beta<br/>
[![Beta Status](https://travis-ci.org/optimizely/android-sdk.svg?branch=beta)](https://travis-ci.org/optimizely/android-sdk)
<br/>
Devel<br/> 
[![Devel Status](https://travis-ci.org/optimizely/android-sdk.svg?branch=devel)](https://travis-ci.org/optimizely/android-sdk)
<br/>
<br/>
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/gradle-extra-configurations-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)
## Overview

This repository houses the Android SDK for Optimizely's Full Stack product. To find out more check out the [documentation](https://developers.optimizely.com/x/solutions/sdks/introduction/index.html?language=android&platform=mobile).

This repo depends on the [Optimizely Java SDK](https://github.com/optimizely/java-sdk).

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
  - Delivers the built Optimizely object to listeners and caches it in memory
2. Event Handler
  - Handles dispatching events to the Optimizely backend
  - Uses a Service so events can be sent without the app being re-opened
  - Persists events in a SQLite3 database
  - Required to be implemented by the Optimizely Java core
3. User Profile
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
3. Create, or use an existing, Optimizely Android project
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
    * `./gradlew user-profile:tasks`

### Android Studio

Android Studio is an IDE that wraps gradle (and `adb`).  Everything you can do in Android Studio can be done from command line with gradle and the other android command line tools.  

You can import this project into Android Studio by opening Android Studio and selecting `Import Project` from the first dialog or from the `File` menu.  Simply select the project's root `build.gradle` file and Android Studio will do the rest.

Tests can be run by right clicking the file in the project pane or by clicking the method name in source and selecting run.  You will be prompted to create an AVD or connect a device if one isn't connected.  

## Releasing

The default branch is devel.  Feature branch PRs are automatically made against it. When PRs are reviewed and pass checks they should be squashed and merged into devel.  Devel will be built and tested for each commit.

Versions are managed via git tags.  Tags can be created from the command line or from the Github UI.

Snapshot builds are made off of the beta branch.  Travis will test all commits to this branch.  When a commit is tagged and pushed, Travis will build, test, *and*, ship the build to Bintray.  The version name used
is the name of the tag.  For snapshot builds the version should have `-SNAPSHOT` appended.  For example `0.1.2-SNAPSHOT`.  Multiple builds with the same version can be pushed to Bintray when using snapshot versions.
This keeps the version number from increasing too quickly for beta builds.  Grade and maven ensure that users are on the latest snapshot via timestamps.
There can be only one git tag per version name so snapshot tags may need to be moved.  For example `git tag -f -a 0.1.2` and `git push -f --tags`.  

Release builds are made off of the master branch.  Travis will test all commits to this branch.  Just like the beta branch, pushing a tag will trigger a build, tests, and release of the version of the tag to Bintray.
For example, to release version 0.1.2 you need to pull devel, checkout master, pull master, fast-forward master to devel, push master, then release 0.1.2 on Github, which creates a tag.  You could also run 
`git tag -a 0.1.2 -m 'Version 0.1.2`.  The argument to `-a` is the actual version name used on Bintray so it must be exact.  Then run `git push --tags` to trigger Travis.

*Note:* only Optimizely employees can push to master, beta, and devel branches.

## Contributing
Please see [CONTRIBUTING](CONTRIBUTING.md).

