## 0.1.71

- Add support for v2 backend endpoint and datafile

## 0.1.70

- Add a `UserExperimentRecord` interface
    - Implementors will get a chance to save and restore activations during bucketing
    - Can be used to make bucketing persistent or to keep a bucketing history
    - Pass implementations to `Optimizely.Builder#withUserExperimentRecord(UserExperimentRecord)` when creating `Optimizely` instances

## 0.1.68

- Beta release of the Java SDK for server-side testing