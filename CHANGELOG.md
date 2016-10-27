# Optimizely Android X SDK Changelog

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
