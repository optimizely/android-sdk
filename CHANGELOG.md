## 1.0.3

- Remove extraneous log message in `AsyncEventHandler`
- Add `jackson-annotations` as a compiled dependency

## 1.0.2

- Gracefully handle datafile that doesn't contain required fields

## 1.0.1

- Allow for configurability of `clientEngine` and `clientVersion` through `Optimizely.Builder`
- Remove ppid query string from V1 events

## 1.0.0

- Introduce support for Full Stack projects in Optimizely X with no breaking changes from previous version
- Update whitelisting to take precedence over audience condition evaluation
- Introduce more graceful exception handling in instantiation and core methods

## 0.1.71

- Add support for v2 backend endpoint and datafile

## 0.1.70

- Add a `UserExperimentRecord` interface
    - Implementors will get a chance to save and restore activations during bucketing
    - Can be used to make bucketing persistent or to keep a bucketing history
    - Pass implementations to `Optimizely.Builder#withUserExperimentRecord(UserExperimentRecord)` when creating `Optimizely` instances

## 0.1.68

- Beta release of the Java SDK for server-side testing