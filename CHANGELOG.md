# Optimizely Java X SDK Changelog
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
