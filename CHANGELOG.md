# Optimizely Android X SDK Changelog

## 1.6.1
April 25, 2018

- Release 1.6.1

This is a patch release for 1.6.0 and 1.5.1 Optimizely SDKs.  

### Bug Fixes
* Fix for the following issue:
https://issuetracker.google.com/issues/63622293
Our github issue is [here](https://github.com/optimizely/android-sdk/issues/194).
The JobWorkService was probably destroyed but we didn't cancel the
processor. It causes an exception in dequeueWork in our JobWorkService.
We wrapped the dequeueWork with a try/catch and are also now cancelling the background task in onDestroy.

* Fix for possible error when loading logger via dagger (fall back logger provided).

* Load UserProfileService on synchronous start.  Also, cleanup UserProfileService cache in the background thread by removing experiments that are no longer in the datafile.

## 2.0.0-beta1

March 29th, 2018

This major release of the Optimizely SDK introduces APIs for Feature Management. It also introduces some breaking changes listed below.

### New Features
* Introduces the `isFeatureEnabled` API to determine whether to show a feature to a user or not.
```
Boolean enabled = optimizelyClient.isFeatureEnabled("my_feature_key", "user_1", userAttributes);
```

* You can also get all the enabled features for the user by calling the following method which returns a list of strings representing the feature keys:
```
ArrayList<String> enabledFeatures = optimizelyClient.getEnabledFeatures("user_1", userAttributes);
```

* Introduces Feature Variables to configure or parameterize your feature. There are four variable types: `Integer`, `String`, `Double`, `Boolean`.
```
String stringVariable = optimizelyClient.getFeatureVariableString("my_feature_key", "string_variable_key", "user_1");
Integer integerVariable = optimizelyClient.getFeatureVariableInteger("my_feature_key", "integer_variable_key", "user_1");
Double doubleVariable = optimizelyClient.getFeatureVariableDouble("my_feature_key", "double_variable_key", "user_1");
Boolean booleanVariable = optimizelyClient.getFeatureVariableBoolean("my_feature_key", "boolean_variable_key", "user_1");
```

### Breaking changes
* The `track` API with revenue value as a stand-alone parameter has been removed. The revenue value should be passed in as an entry of the event tags map. The key for the revenue tag is `revenue` and will be treated by Optimizely as the key for analyzing revenue data in results.
```
Map<String, Object> eventTags = new HashMap<String, Object>();

// reserved "revenue" tag
eventTags.put("revenue", 6432);

optimizelyClient.track("event_key", "user_id", userAttributes, eventTags);
```

* Live variable accessor methods have been removed and have been replaced with the feature variable methods mentioned above. Feature variables are scoped to a feature so you must supply the feature key in addition to the variable key to access them.

  - `getVariableBoolean` now becomes `getFeatureVariableBoolean`
  - `getVariableString` now becomes `getFeatureVariableString`
  - `getVariableInteger` now becomes `getFeatureVariableInteger`
  - `getVariableFloat` now becomes `getFeatureVariableDouble`

## 1.6.0
Febuary 3, 2018

- Release 1.6.0

This release adds support for bucketing id (By passing in `$opt_bucketing_id` in the attribute map, you can  override the user id as the bucketing variable. This is useful when wanting a set of users to share the same experience such as two players in a game).

This release also deprecates the old notification broadcaster in favor of a notification center that supports a wide range of notifications.  The notification listener is now registered for the specific notification type such as ACTIVATE and TRACK.  This is accomplished by allowing for a variable argument call to notify (a new var arg method added to the NotificationListener).  Specific abstract classes exist for the associated notification type (ActivateNotificationListener and TrackNotificationListener).  These abstract classes enforce the strong typing that exists in Java.  You may also add custom notification types and fire them through the notification center.  The notification center is implemented using this var arg approach in all Optimizely SDKs.

*New Features*

- Added `$opt_bucketing_id` in the attribute map for overriding bucketing using the user id.  It is available as a static string in DecisionService.ATTRIBUTE_BUCKETING_ID
- Optimizely notification center for activate and track notifications.

*Breaking change*
There is a new abstract method on NotificationListener notify(args...);

## 1.5.1
November 1, 2017

- Release 1.5.1

*New Features*

- Numeric metrics
- Client-side programmatic forced variations.
- Example of synchronous and asynchronous initialize in test-app

*Bug Fixes*

- Remove Espresso dependency
- Narrow proguard rules
- Last modified fixed so that multiple project files can be used.
- Call start listener if there is an exception.
- Example of overriding Gson and android-logger in test-app gradle file.
- Fix crash on API 17 (missing annotation).
- Support for Android O (please see developer docs for details). Basically, Android O and above will use JobScheduler and pre Android O will continue to use AlarmService.  This is done through a class called the JobWorkService which allows you to keep your Service and IntentService intact.  Developers can piggyback on this method and keep thier IntentServices and use the JobWorkService.
- Proguard rules were broken and were causing event payload to be stripped to single character keys.

*Breaking Changes*

- Same as 1.4.0 see below.
- Need to add permissions to both receivers in your manifest if you plan on using the EventRescheduler or the DatafileRescheduler (see test_app manifest for example) https://github.com/optimizely/android-sdk/blob/master/test-app/src/main/AndroidManifest.xml
- Updated build tools and target to API 26 which will cause proguard warnings if you are not using the latest build tools.
- Also for Android O, you must register for the SUPPLICANT_CONNECTION_CHANGE_ACTION intent filter in code (see the test-app for an example).

## 1.5.0
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

## 1.4.0
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

## 1.4.0-beta-RC2
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

## 1.4.0-beta-RC1
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

## 1.4.0-alpha-RC1
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


## 1.4.0-alpha
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

## 1.3.1
April 25, 2017

- Handle exceptions in top-level APIs

## 1.3.0
April 12, 2017

- Add getter for `ProjectConfig`

## 1.2.0
March 20, 2017

- Add event tags to the `track` API
- Deprecated `eventValue` parameter from the `track` API. Use event tags to pass in event value instead
- Update to java-core 1.6.0 (https://github.com/optimizely/java-sdk/blob/master/CHANGELOG.md#160)

## 1.1.0
February 17, 2017

- Support Android TV SDK client engine
- Update to java-core 1.5.0 (https://github.com/optimizely/java-sdk/blob/master/CHANGELOG.md#150)


## 1.0.0
January 23, 2017

- GA release

## 0.5.0
January 20, 2017

*Bug Fixes*

- Persist experiment and variation IDs instead of keys in the `AndroidUserProfile`

*Breaking Changes*

- Change live variable getter signature from `getVariableFloat` to `getVariableDouble`

## 0.4.1
December 28, 2016

*Bug Fixes*

- Add try catches around Cache.java and Client.java to handle exceptions gracefully
- Fixes crash with not being able to bind to DatafileService

## 0.4.0
December 15, 2016

*New Features*

- Add support for IP anonymization

*Breaking Changes*

- Rename `AndroidUserExperimentRecord` to `AndroidUserProfile`
- Change position of `activateExperiment` parameter in live variable getters

## 0.3.0
December 8, 2016

*New Features*

- Add support for live variables

## 0.2.2
November 30, 2016

*Bug Fixes*
- Update to java-core 1.0.3 which fixes crashes with Jackson annotations on ICS
- Use the old SQLiteDatabse constructor for compatibility with ICS

*Breaking Changes*
- Changed the initialization call from `start` to `initialize`
- `getOptimizely` now only returns the cached version of the client

## 0.2.1
November 4, 2016

*Bug Fixes*
- Ensures that the `OptimizelyStartedListener` is always called *once* and *only once* even if Optimizely fails to load a datafile from everywhere. It should be safe to launch parts of your app after the callback hits now.

## 0.2.0
October 28, 2016

*Breaking Changes*
- Renames `AndroidOptimizely` to `OptimizelyClient`

## 0.1.3
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
