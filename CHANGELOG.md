# Optimizely Android X SDK Changelog

## 3.2.0
August 21st 2019

This minor release updates the SDK to use the Optimizely Java SDK 3.2.1 which includes the following:

### New Features:
* The default datafile manager now supports the Java SDK [#ProjectConfigManager](https://github.com/optimizely/java-sdk/blob/master/core-api/src/main/java/com/optimizely/ab/config/ProjectConfigManager.java)
    * The default datafile manager with polling enabled will pickup new ProjectConfigs without needing to re-initialize.
    * You can register for Datafile change notifications.

### Bug Fixes:
* Potential bug fix for ANRs where a service is started but there is a bad network connection. [#PR295](https://github.com/optimizely/android-sdk/pull/295)

### Deprecated
* DatafileHandler interface has been deprecated. In the future we will start using the ProjectConfigManager from the Java SDK mentioned above.


## 3.1.1
July 23rd, 2019

### Bug Fixes:
* SourceClear flagged jackson-databind 2.9.8 fixed in 2.9.9.1

## 3.1.0
May 13th, 2019

This minor release updates the SDK to use the Optimizely Java SDK 3.1.0 which includes the following:

### New Features:
* Introduced Decision notification listener to be able to record:
    * Variation assignments for users activated in an experiment.
    * Feature access for users.
    * Feature variable value for users.
* Added APIs to be able to conveniently add Decision notification handler (`addDecisionNotificationHandler`) and Track notification handler (`addTrackNotificationHandler`).

### Bug Fixes:
* Feature variable APIs return default variable value when featureEnabled property is false. ([#274](https://github.com/optimizely/java-sdk/pull/274))

### Deprecated
* Activate notification listener is deprecated as of this release. Recommendation is to use the new Decision notification listener. Activate notification listener will be removed in the next major release.
* `addActivateNotificationListener`, `addTrackNotificationListener` and `addNotificationListener` APIs on `NotificationCenter`.


## 3.0.1
April 23, 2019

This patch release fixes some git hub issues mentioned below.  

### Bug Fixes
* The Logger security exception is handled a little more cleanly for logging. ([#270](https://github.com/optimizely/android-sdk/pull/270)) 
* There was the possibility to start too many intents for event handling. ([#268](https://github.com/optimizely/android-sdk/pull/268))
* The proguard rules have been cleaned up and tested. ([#266](https://github.com/optimizely/android-sdk/pull/266))
* This also includes using Optimizely Java SDK 3.0.1.  The Java SDK patch allows for using the Optimizely Android aar with older versions of org.json which are included in the android framework.

## 3.0.0
February 14, 2019

The 3.0 release improves event tracking and supports additional audience targeting functionality.

### New Features:
* Event tracking:
  * The Track method now dispatches its conversion event _unconditionally_, without first determining whether the user is targeted by a known experiment that uses the event. This may increase outbound network traffic.
  * In Optimizely results, conversion events sent by 3.0 SDKs don't explicitly name the experiments and variations that are currently targeted to the user. Instead, conversions are automatically attributed to variations that the user has previously seen, as long as those variations were served via 3.0 SDKs or by other clients capable of automatic attribution, and as long as our backend actually received the impression events for those variations.
  * Altogether, this allows you to track conversion events and attribute them to variations even when you don't know all of a user's attribute values, and even if the user's attribute values or the experiment's configuration have changed such that the user is no longer affected by the experiment. As a result, **you may observe an increase in the conversion rate for previously-instrumented events.** If that is undesirable, you can reset the results of previously-running experiments after upgrading to the 3.0 SDK.
  * This will also allow you to attribute events to variations from other Optimizely projects in your account, even though those experiments don't appear in the same datafile.
  * Note that for results segmentation in Optimizely results, the user attribute values from one event are automatically applied to all other events in the same session, as long as the events in question were actually received by our backend. This behavior was already in place and is not affected by the 3.0 release.
* Support for all types of attribute values, not just strings.
  * All values are passed through to notification listeners.
  * Strings, booleans, and valid numbers are passed to the event dispatcher and can be used for Optimizely results segmentation. A valid number is a finite float, double, integer, or long in the inclusive range [-2⁵³, 2⁵³].
  * Strings, booleans, and valid numbers are relevant for audience conditions.
* Support for additional matchers in audience conditions:
  * An `exists` matcher that passes if the user has a non-null value for the targeted user attribute and fails otherwise.
  * A `substring` matcher that resolves if the user has a string value for the targeted attribute.
  * `gt` (greater than) and `lt` (less than) matchers that resolve if the user has a valid number value for the targeted attribute. A valid number is a finite float, double, integer, or long in the inclusive range [-2⁵³, 2⁵³].
  * The original (`exact`) matcher can now be used to target booleans and valid numbers, not just strings.
* Support for A/B tests, feature tests, and feature rollouts whose audiences are combined using `"and"` and `"not"` operators, not just the `"or"` operator.
* Datafile-version compatibility check: The SDK will remain uninitialized (i.e., will gracefully fail to activate experiments and features) if given a datafile version greater than 4.
* Updated Pull Request template and commit message guidelines.
* When given an invalid datafile, the Optimizely client object now instantiates into a no-op state instead of throwing a `ConfigParseException`. This matches the behavior of the other Optimizely SDKs.

### Breaking Changes:
* Java 7 is no longer supported.
* Conversion events sent by 3.0 SDKs don't explicitly name the experiments and variations that are currently targeted to the user, so these events are unattributed in raw events data export. You must use the new _results_ export to determine the variations to which events have been attributed.
* Previously, notification listeners were only given string-valued user attributes because only strings could be passed into various method calls. That is no longer the case. The `ActivateNotificationListener` and `TrackNotificationListener` interfaces now receive user attributes as `Map<String, ?>` instead of `Map<String, String>`.
* The Localytics integration now relies on the user profile service to attribute events to the user's variations.
* Drops support for the `getVariableBoolean`, `getVariableDouble`, `getVariableInteger`, and `getVariableString` APIs. Please migrate to the Feature Management APIs which were introduced in the 2.x releases.

### Bug Fixes:
* Experiments and features can no longer activate when a negatively targeted attribute has a missing, null, or malformed value.
  * Audience conditions (except for the new `exists` matcher) no longer resolve to `false` when they fail to find an legitimate value for the targeted user attribute. The result remains `null` (unknown). Therefore, an audience that negates such a condition (using the `"not"` operator) can no longer resolve to `true` unless there is an unrelated branch in the condition tree that itself resolves to `true`.
* Support for empty user IDs.
* SourceClear flagged jackson-databind 2.9.4 fixed in 2.9.8

## 2.1.4
February 8th, 2019

This is a patch release.

### Bug Fixes
* fix User Profile Service.  Don't trim the user profile service unless there are over 100 experiments in a users UPS. This will be configurable in the future.

## 2.1.3
December 6th, 2018

### Bug Fixes
The attributes map is now copied to ensure there is no concurrency issues.

## 3.0.0-RC2

November 20th, 2018

This is the release candidate for the 3.0 SDK, which includes a number of improvements to audience targeting along with a few bug fixes.

### New Features
* Support for number-valued and boolean-valued attributes. ([#213](https://github.com/optimizely/java-sdk/pull/213))
* Support for audiences with new match conditions for attribute values, including “substring” and “exists” matches for strings; “greater than”, “less than”, exact, and “exists” matches for numbers; and “exact”, and “exists” matches for booleans. 
* Built-in datafile version compatibility checks so that SDKs will not initialize with a newer datafile it is not compatible with. ([#209](https://github.com/optimizely/java-sdk/pull/209))
* Audience combinations within an experiment are unofficially supported in this release.

### Breaking Changes
* Previously, notification listeners filtered non-string attribute values from the data passed to registered listeners. To support our growing list of supported attribute values, we’ve changed this behavior. Notification listeners will now post any value type passed as an attribute. Therefore, the interface of the notification listeners has changed to accept a `Map<String, ?>`.
* Update to use Java 1.7 ([#208](https://github.com/optimizely/java-sdk/pull/208))

### Bug Fixes
* refactor: Performance improvements for JacksonConfigParser ([#209](https://github.com/optimizely/java-sdk/pull/209))
* refactor: typeAudience.combinations will not be string encoded like audience.combinations.  To handle this we created a new parsing type TypedAudience.
* fix for exact match when dealing with integers and doubles.  Created a new Numeric match type.
* make a copy of attributes passed in to avoid any concurrency problems. Addresses GitHub issue in Optimizely Andriod SDK.
* allow single root node for audience.conditions, typedAudience.conditions, and Experiment.audienceCombinations.
 
## 3.0.0-RC
November 9, 2018

This is a RC candidate for major release 3.0.0 with support of new audience match types.
### New Features
* Support for number-valued and boolean-valued attributes. ([#213](https://githu
b.com/optimizely/java-sdk/pull/213))
* Support for audiences with new match conditions for attribute values, including “substring” and “exists” matches for strings; “greater than”, “less than”, exact, and “exists” matches for numbers; and “exact”, and “exists” matches for booleans. 
* Built-in datafile version compatibility checks so that SDKs will not initialize with a newer datafile it is not compatible with. ([#209](https://github.com/op
timizely/java-sdk/pull/209))
* Audience combinations within an experiment are unofficially supported in this 
release.

### Breaking Changes
* Previously, notification listeners filtered non-string attribute values from t
he data passed to registered listeners. To support our growing list of supported attribute values, we’ve changed this behavior. Notification listeners will now post any value type passed as an attribute. Therefore, the interface of the notification listeners has changed to accept a `Map<String, ?>`.
* Update to use Java 1.7 ([#208](https://github.com/optimizely/java-sdk/pull/208))

## 2.1.2
November 8, 2018

This is a patch release.

### Bug Fixes
Fix job scheduler exception when scheduling a repeatable job in the background ([#236](https://github.com/optimizely/android-sdk/pull/236)).

## 2.1.1
October 3nd, 2018

This is a patch release.

### Bug Fixes
Filters out attributes with null values from the event payload (via upgrade to Optimizely Java SDK 2.1.3

Fix Optimizely builder to user DatafileConfig instead of deprecated project id.

Update packages for gson, Jackson, and slf4j.

Update credits

## 2.1.0
August 2nd, 2018

This release is the 2.x general availability launch of the Android SDK, which includes a number of significant new features that are now stable and fully supported. [Feature Management](https://developers.optimizely.com/x/solutions/sdks/reference/?language=android#feature-introduction) is now generally available, which introduces  new APIs and which replaces the SDK's variable APIs (`getVariableBoolean`, etc.) with the feature variable APIs (`getFeatureVariableBoolean`, etc.).  

The primary difference between the new Feature Variable APIs and the older, Variable APIs is that they allow you to link your variables to a Feature (a new type of entity defined in the Optimizely UI) and to a feature flag in your application. This in turn allows you to run Feature Tests and Rollouts on both your Features and Feature Variables. For complete details of the Feature Management APIs, see the "New Features" section below.

To learn more about Feature Management, read our [knowledge base article introducing the feature](https://help.optimizely.com/Set_Up_Optimizely/Develop_a_product_or_feature_with_Feature_Management).

### New Features
* Introduces the `isFeatureEnabled` API, a featue flag used to determine whether to show a feature to a user. The `isFeatureEnabled` should be used in place of the `activate` API to activate experiments running on features. Specifically, calling this API causes the SDK to evaluate all [Feature Tests](https://developers.optimizely.com/x/solutions/sdks/reference/?language=android#activate-feature-tests) and [Rollouts](https://developers.optimizely.com/x/solutions/sdks/reference/?language=android#activate-feature-rollouts) associated with the provided feature key.
```
Boolean enabled = optimizelyClient.isFeatureEnabled("my_feature_key", "user_1", userAttributes);
```

* Get all enabled features for a user by calling the following method, which returns a list of strings representing the feature keys:
```
ArrayList<String> enabledFeatures = optimizelyClient.getEnabledFeatures("user_1", userAttributes);
```

* Introduces Feature Variables to configure or parameterize your feature. There are four variable types: `Integer`, `String`, `Double`, `Boolean`. Note that unlike the Variable APIs, the Feature Variable APIs do not dispatch impression events.  Instead, first call `isFeatureEnabled` to activate your experiments, then retrieve your variables.
```
String stringVariable = optimizelyClient.getFeatureVariableString("my_feature_key", "string_variable_key", "user_1");
Integer integerVariable = optimizelyClient.getFeatureVariableInteger("my_feature_key", "integer_variable_key", "user_1");
Double doubleVariable = optimizelyClient.getFeatureVariableDouble("my_feature_key", "double_variable_key", "user_1");
Boolean booleanVariable = optimizelyClient.getFeatureVariableBoolean("my_feature_key", "boolean_variable_key", "user_1");
```

* Introduces SDK Keys, which allow you to use Environments with the Android SDK. Use an SDK Key to initialize your OptimizelyManager, and the SDK will retrieve the datafile for the environment associated with the SDK Key. This replaces initialization with Project ID.
```
OptimizelyManager optimizelyManager = OptimizelyManager.builder()
    .withSDKKey("SDK_KEY_HERE")
    .build(getApplication());
optimizelyManager.initialize(this, new OptimizelyStartListener() {
    @Override
    public void onStart(OptimizelyClient optimizely) {
        //user optimizely client here
    }
});
```

### Deprecations
* Version 2.1.0 deprecates the Variable APIs: `getVariableBoolean`, `getVariableFloat`, `getVariableInteger`, and `getVariableString` 

* Replace use of the Variable APIs with Feature Mangement's Feature Variable APIs, described above

* We will continue to support the Variable APIs until the 3.x release, but we encourage you to upgrade as soon as possible

* You will see a deprecation warning if using a 2.x SDK with the deprecated Variable APIs, but the APIs will continue to behave as they did in 1.x versions of the SDK

### Upgrading from 1.x

In order to begin using Feature Management, you must discontinue use of 1.x variables in your experiments.  First, pause and archive all experiments that use variables. Then, contact [Optimizely Support](https://optimizely.zendesk.com/hc/en-us/requests) in order to have your project converted from the 1.x SDK UI to the 2.x SDK UI. In addition to allowing for access to the Feature Management UI, upgrading to the 2.x SDK UI grants you access to [Environments](https://developers.optimizely.com/x/solutions/sdks/reference/?language=android#environments) and other new features.
* *Note*: All future feature development on the Android SDK will assume that your are using the 2.x SDK UI, so we encourage you to upgrade as soon as possible.


### Breaking changes
* The `track` API with revenue value as a stand-alone parameter has been removed. The revenue value should be passed in as an entry of the event tags map. The key for the revenue tag is `revenue` and will be treated by Optimizely as the key for analyzing revenue data in results.
```
Map<String, Object> eventTags = new HashMap<String, Object>();

// reserved "revenue" tag
eventTags.put("revenue", 6432);

optimizelyClient.track("event_key", "user_id", userAttributes, eventTags);

```

* We have removed deprecated classes with the `NotificationBroadcaster` in favor of the new API with the `NotificationCenter`. We have streamlined the API so that it is easily usable with Java Lambdas in *Java 1.8+*. We have also added some convenience methods to add these listeners. Finally, some of the API names have changed slightly (e.g. `clearAllNotifications()` is now `clearAllNotificationListeners()`)



## 2.0.0-beta6
August 1st, 2018

### Bug Fixes
* Bump to Java SDK [2.1.2](https://github.com/optimizely/java-sdk/releases/tag/2.1.2) which improves performance of API calls from.
* Bring back async init without the datafile. (#211)

## 2.0.0-beta5
July 23, 2018

### Bug Fixes
* Pull from the correct datafile location when using SDK Key. (#207)

## 2.0.0-beta4
June 22, 2018

### Bug Fixes
* Fix impression sent from feature experiment variation toggled off. (#205)

## 2.0.0-beta3

### New Features
* Introduce support for SDK Key so we can poll for datafiles from different environments.

### Deprecated
* Live Variables getters are now deprecated. Please use Feature Variables instead.

### Bug Fixes
* Fix for datafile download start when datafile has not changed or when starting synchronously.

## 2.0.0-beta2

May 17, 2018

**This "-beta2" replaces the rolled-back 2.0.x release because of usability issues uncovered during the 2.0.x launch. Please note that 2.0+ SDKs are incompatible with existing 1.x Mobile Optimizely projects. Before you use 2.0+ and Feature Management, please contact your Optimizely account team. If you are not upgrading to Feature Management, we recommend remaining on your current 1.x SDK.**

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

* We have removed deprecated classes with the `NotificationBroadcaster` in favor of the new API with the `NotificationCenter`. We have streamlined the API so that it is easily usable with Java Lambdas in *Java 1.8+*. We have also added some convenience methods to add these listeners. Finally, some of the API names have changed slightly (e.g. `clearAllNotifications()` is now `clearAllNotificationListeners()`)

### Bug Fixes
* Fix for the following issue:
https://issuetracker.google.com/issues/63622293
Our github issue is [here](https://github.com/optimizely/android-sdk/issues/194).
The JobWorkService was probably destroyed but we didn't cancel the
processor. It causes an exception in dequeueWork in our JobWorkService.
We wrapped the dequeueWork with a try/catch and are also now cancelling the background task in onDestroy.

* Fix for possible error when loading logger via dagger (fall back logger provided).

* Load UserProfileService on synchronous start.  Also, cleanup UserProfileService cache in the background thread by removing experiments that are no longer in the datafile.

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
