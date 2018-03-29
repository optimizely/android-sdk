# Optimizely Java X SDK Changelog

## 2.0.0-beta6

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

## 1.9.0

January 30, 2018

This release adds support for bucketing id (By passing in `$opt_bucketing_id` in the attribute map to override the user id as the bucketing variable. This is useful when wanting a set of users to share the same experience such as two players in a game).

This release also depricates the old notification broadcaster in favor of a notification center that supports a wide range of notifications.  The notification listener is now registered for the specific notification type such as ACTIVATE and TRACK.  This is accomplished by allowing for a variable argument call to notify (a new var arg method added to the NotificationListener).  Specific abstract classes exist for the associated notification type (ActivateNotification and TrackNotification).  These abstract classes enforce the strong typing that exists in Java.  You may also add custom notification types and fire them through the notification center.  The notification center is implemented using this var arg approach in all Optimizely SDKs.

### New Features

- Added `$opt_bucketing_id` in the attribute map for overriding bucketing using the user id.  It is available as a static string in DecisionService.ATTRIBUTE_BUCKETING_ID
- Optimizely notification center for activate and track notifications.

## 2.0.0 Beta 3
January 5, 2018

This is a patch release for 2.0.0 Beta. It contains a minor bug fix.

### Bug Fixes
SDK checks for null values in the Feature API parameters.
- If `isFeatureEnabled` is called with a null featureKey or a null userId, it will return false immediately.
- If any of `getFeatureVariable<Type>` are called with a null featureKey, variableKey, or userId, null will be returned immediately.

## 1.8.1
December 12, 2017

This is a patch release for 1.8.0.  It contains two bug fixes mentioned below.

### Bug Fixes
SDK returns NullPointerException when activating with unknown attribute.

Pooled connection times out if it is idle for a long time (AsyncEventHandler's HttpClient uses PoolingHttpClientConnectionManager setting a validate interval).

## 2.0.0 Beta 2
October 5, 2017

This release is a second beta release supporting feature flags and rollouts. It includes all the same new features and breaking changes as the last beta release.

### Bug Fixes
Fall back to default feature variable value when there is no variable usage in the variation a user is bucketed into. For more information see [PR #149](https://github.com/optimizely/java-sdk/pull/149).

## 2.0.0 Beta
September 29, 2017

This release is a beta release supporting feature flags and rollouts.

### New Features
#### Feature Flag Accessors
You can now use feature flags in the Java SDK. You can experiment on features and rollout features through the Optimizely UI.

- `isFeatureEnabled`
- `getFeatureVariableBoolean`
- `getFeatureVariableDouble`
- `getFeatureVariableInteger`
- `getFeatureVariableString`

### Breaking Changes

- Remove Live Variables accessors
  - `getVariableString`
  - `getVariableBoolean`
  - `getVariableInteger`
  - `getVariableDouble`
- Remove track with revenue as a parameter. Pass the revenue value as an event tag instead
  - `track(String, String, long)`
  - `track(String, String, Map<String, String>, long)`
- We will no longer run all unit tests in travis-ci against Java 7.
  We will still continue to set `sourceCompatibility` and `targetCompatibility` to 1.6 so that we build for Java 6.

## 1.8.0

August 29, 2017

This release adds support for numeric metrics and forced bucketing (in code as opposed to whitelisting via project file).

### New Features

- Added `setForcedVariation` and `getForcedVariation`
- Added any numeric metric to event metrics.

### Breaking Changes

- Nothing breaking from 1.7.0

## 1.7.0

July 12, 2017

This release will support Android SDK release 1.4.0

### New Features

- Added `UserProfileService` interface to allow for sticky bucketing

### Breaking Changes

- Removed `UserProfile` interface. Replaced with `UserProfileService` interface.
- Removed support for v1 datafiles.

## 2.0.0-alpha

May 19, 2017

### New Features

- Added `UserProfileService` interface to allow for sticky bucketing

### Breaking Changes

- Removed `UserProfile` interface. Replaced with `UserProfileService` interface.
- Removed support for v1 datafiles.

## 1.6.0

March 17, 2017

- Add event tags to `track` API and include in the event payload
- Deprecates the `eventValue` parameter from the `track` method. Should use event tags to pass in event value instead
- Gracefully handle a null attributes parameter
- Gracefully handle a null/empty datafile when using the Gson parser

## 1.5.0

February 16, 2017

- Support Android TV SDK client engine

## 1.4.1

February 1, 2017

- Default `null` status in datafile to `Not started`

## 1.4.0

January 31, 2017

- Add `sessionId` parameter to `activate` and `track` and include in event payload
- Append datafile `revision` to event payload
- Add support for "Launched" experiment status

## 1.3.0

January 17, 2017

- Add `onEventTracked` listener
- Change `getVariableFloat` to `getVariableDouble`
- Persist experiment and variation IDs instead of keys in the `UserProfile`

## 1.2.0

December 15, 2016

- Change position of `activateExperiment` parameter in the method signatures of `getVariableString`, `getVariableBoolean`, `getVariableInteger`, and `getVariableFloat`
- Change `UserExperimentRecord` to `UserProfile`
- Add support for IP anonymization
- Add `NotificationListener` for SDK events

## 1.1.0

December 8, 2016

- Add support for live variables

## 1.0.3

November 28, 2016

- Remove extraneous log message in `AsyncEventHandler`
- Add `jackson-annotations` as a compiled dependency

## 1.0.2

October 5, 2016

- Gracefully handle datafile that doesn't contain required fields

## 1.0.1

October 5, 2016

- Allow for configurability of `clientEngine` and `clientVersion` through `Optimizely.Builder`
- Remove ppid query string from V1 events

## 1.0.0

October 3, 2016

- Introduce support for Full Stack projects in Optimizely X with no breaking changes from previous version
- Update whitelisting to take precedence over audience condition evaluation
- Introduce more graceful exception handling in instantiation and core methods

## 0.1.71

September 19, 2016

- Add support for v2 backend endpoint and datafile

## 0.1.70

August 29, 2016

- Add a `UserExperimentRecord` interface
    - Implementors will get a chance to save and restore activations during bucketing
    - Can be used to make bucketing persistent or to keep a bucketing history
    - Pass implementations to `Optimizely.Builder#withUserExperimentRecord(UserExperimentRecord)` when creating `Optimizely` instances

## 0.1.68

July 26, 2016

- Beta release of the Java SDK for server-side testing
