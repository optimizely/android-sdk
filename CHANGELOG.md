# Optimizely Android X SDK Changelog
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
