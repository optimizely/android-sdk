# Optimizely Android X SDK Changelog
### 1.5.0
October 30, 2017

- Release 1.5.0

*New Features*

- Numeric metrics
- Client-side programmatic forced variations.


*Bug Fixes*

- Remove Espresso dependency
- Narrow proguard rules
- Last modified fixed so that multiple project files can be used.
- Call start listener if there is an exception.
- Example of overriding Gson and android-logger in test-app gradle file.
- Fix crash on API 17 (missing annotation).
- Support for Android O (please see developer docs for details). Basically, Android O and above will use JobScheduler and pre Android O will continue to use AlarmService.  This is done through a class called the JobWorkService which allows you to keep your Service and IntentService intact.  Developers can piggyback on this method and keep thier IntentServices and use the JobWorkService.

*Breaking Changes*

- Same as 1.4.0 see below.
- Need to add permissions to both receivers in your manifest if you plan on using the EventRescheduler or the DatafileRescheduler (see test_app manifest for example) https://github.com/optimizely/android-sdk/blob/master/test-app/src/main/AndroidManifest.xml
- Updated build tools and target to API 26
- Also for Android O, you must register for the SUPPLICANT_CONNECTION_CHANGE_ACTION intent filter in code (see the test-app for an example).

### 1.4.0
August 9, 2017

- Release 1.4.0

*Bug Fixes*

- Add consumerProguardFiles.
- Better javadocs.
- Cleanup any resource leaks.
- Better exception handling to avoid any crashes.
- Fix proguard rules.
- Fix logger issue.
- Allow EventRescheduler to work with wifi change intent filter (you don't have to include that intent filter).
- Remove unused imports.

*Breaking Changes*

- Must include intent filter for EventRescheduler and DatafileRescheduler in the application manifest if the developer wants to use them (see the test-app manifest for an example).
- Pass context into OptimizelyManager.Builder.
- UserProfileService added.
- Background processes are not running by default.
- Various handlers (EventHandler, DatafileHandler, ErrorHandler) can be overridden.

### 1.4.0-beta-RC2
August 9, 2017

- Release candidate for 1.4.0

*Bug Fixes*

- Add consumerProguardFiles.
- Better javadocs.
- Cleanup any resource leaks.
- Better exception handling to avoid any crashes.
- Fix proguard rules.
- Fix logger issue.
- Allow EventRescheduler to work with wifi change intent filter (you don't have to include that intent filter).
- Remove unused imports.

*Breaking Changes*

- Must include intent filter for EventRescheduler and DatafileRescheduler in the application manifest if the developer wants to use them (see the test-app manifest for an example).
- Pass context into OptimizelyManager.Builder.
- UserProfileService added.
- Background processes are not running by default.
- Various handlers (EventHandler, DatafileHandler, ErrorHandler) can be overridden.

### 1.4.0-beta-RC1
August 2, 2017

- Release candidate for 1.4.0

*Bug Fixes*

- Better javadocs.
- Cleanup any resource leaks.
- Better exception handling to avoid any crashes.
- Fix proguard rules.
- Fix logger issue.
- Allow EventRescheduler to work with wifi change intent filter (you don't have to include that intent filter).
- Remove unused imports.

*Breaking Changes*

- Must include intent filter for EventRescheduler and DatafileRescheduler in the application manifest if the developer wants to use them (see the test-app manifest for an example).
- Pass context into OptimizelyManager.Builder.
- UserProfileService added.
- Background processes are not running by default.
- Various handlers (EventHandler, DatafileHandler, ErrorHandler) can be overridden.

### 1.4.0-alpha-RC1
July 26, 2017

- Release candidate for 1.4.0

*Bug Fixes*

- Better javadocs.
- Cleanup any resource leaks.
- Better exception handling to avoid any crashes.
- Fix proguard rules
- Fix logger issue
- Allow EventRescheduler to work with wifi change intent filter (you don't have to include that intent filter).

*Breaking Changes*

- Must include intent filter for EventRescheduler and DatafileRescheduler in the application manifest if the developer wants to use them (see the test-app manifest for an example).
- Pass context into OptimizelyManager.Builder.
- UserProfileService added.
- Background processes are not running by default.
- Various handlers (EventHandler, DatafileHandler, ErrorHandler) can be overridden.


### 1.4.0-alpha
July 11, 2017

- Allow configure background tasks to run or not.

*Bug Fixes*

- Close cursor on SQLite.

*Breaking Changes*

- Must include intent filter for EventRescheduler and DatafileRescheduler in the application manifest if the developer wants to use them (see the test-app manifest for an example).
- Pass context into OptimizelyManager.Builder.
- UserProfileService added.
- Background processes are not running by default.
- Various handlers (EventHandler, DatafileHandler, ErrorHandler) can be overridden.

### 1.3.1
April 25, 2017

- Handle exceptions in top-level APIs

### 1.3.0
April 12, 2017

- Add getter for `ProjectConfig`

### 1.2.0
March 20, 2017

- Add event tags to the `track` API
- Deprecated `eventValue` parameter from the `track` API. Use event tags to pass in event value instead
- Update to java-core 1.6.0 (https://github.com/optimizely/java-sdk/blob/master/CHANGELOG.md#160)

### 1.1.0
February 17, 2017

- Support Android TV SDK client engine
- Update to java-core 1.5.0 (https://github.com/optimizely/java-sdk/blob/master/CHANGELOG.md#150)


### 1.0.0
January 23, 2017

- GA release

### 0.5.0
January 20, 2017

*Bug Fixes*

- Persist experiment and variation IDs instead of keys in the `AndroidUserProfile`

*Breaking Changes*

- Change live variable getter signature from `getVariableFloat` to `getVariableDouble`

### 0.4.1
December 28, 2016

*Bug Fixes*

- Add try catches around Cache.java and Client.java to handle exceptions gracefully
- Fixes crash with not being able to bind to DatafileService

### 0.4.0
December 15, 2016

*New Features*

- Add support for IP anonymization

*Breaking Changes*

- Rename `AndroidUserExperimentRecord` to `AndroidUserProfile`
- Change position of `activateExperiment` parameter in live variable getters

### 0.3.0
December 8, 2016

*New Features*

- Add support for live variables

### 0.2.2
November 30, 2016

*Bug Fixes*
- Update to java-core 1.0.3 which fixes crashes with Jackson annotations on ICS
- Use the old SQLiteDatabse constructor for compatibility with ICS

*Breaking Changes*
- Changed the initialization call from `start` to `initialize`
- `getOptimizely` now only returns the cached version of the client

### 0.2.1
November 4, 2016

*Bug Fixes*
- Ensures that the `OptimizelyStartedListener` is always called *once* and *only once* even if Optimizely fails to load a datafile from everywhere. It should be safe to launch parts of your app after the callback hits now.

### 0.2.0
October 28, 2016

*Breaking Changes*
- Renames `AndroidOptimizely` to `OptimizelyClient`

### 0.1.3
October 27, 2016

*Bug Fixes*
- Now service intervals can be changed after they are scheduled the first time.
  - If a service is scheduled when rescheduling the old service will be unscheduled.
- Now multiple `OptimizelyManager`instances can be created for multiple Optimizely X projects.
  - A manager builds `AndroidOptimizely` for the project id it was created with and only that that project id.
  - Could run one project in your activites and one in your services.
- Now shows user experiment record logs.
- Turns on more core logs.

*New Features*
- *Exponential Backoff.* Datafile download event dispatching now exmploy exponential backoff.
- *Preemptive Wifi Event Flushing.* If event flushing is scheduled and wifi becomes available Optmizely will preemptively attempt to flush events before the next interval occurs.  If flushing occurs the flushing will be rescheduled.
