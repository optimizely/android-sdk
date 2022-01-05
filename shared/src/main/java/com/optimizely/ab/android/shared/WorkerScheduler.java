/****************************************************************************
 * Copyright 2021, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/
package com.optimizely.ab.android.shared;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.AbstractMap;
import java.util.concurrent.TimeUnit;

/**
 * WorkScheduler uses androidx work manager to schedule tasks. It replaces the deprecated ServiceScheduler.
 * The WorkScheduler takes a ListenableWorker and using the work manager schedules the task.  Because the work manager
 * does expose a way to persist jobs, you will need to use a rescheduler to reschedule tasks.
 *
 * This class eliminates the use of the deprecated Intents and JobSchedulers .
 */
public class WorkerScheduler {
    // when true, work requested only when connection is available.
    public static boolean requestOnlyWhenConnected = true;

    /**
     * Unschedule a scheduled service for a given worker id
     * @param context current application context
     * @param workerId work id to cancel
     */
    public static void unscheduleService(Context context, String workerId) {
        WorkManager.getInstance(context).cancelAllWorkByTag(workerId);
    }

    /**
     * Schedule a repeated service using the work scheduler from androidx.
     * @param context current application context
     * @param workerId worker id
     * @param clazz class based on ListenableWorker
     * @param data androidx.work.Data
     * @param interval the interval for the repeated service
     * @return An (WorkRequest, Operation) that can be used for tracing work state
     */
    public static AbstractMap.SimpleEntry<WorkRequest, Operation> scheduleService(Context context, String workerId, Class clazz, Data data, long interval) {
        unscheduleService(context, workerId);

        long minutes = interval < 15 ? 15 : interval;

        WorkRequest.Builder workRequestBuilder = new PeriodicWorkRequest.Builder(clazz, minutes, TimeUnit.MINUTES)
                .addTag(workerId)
                .setInputData(data)
                .setInitialDelay(minutes, TimeUnit.MINUTES);

        if (requestOnlyWhenConnected) {
             // requests only when connection is available (to control network connection failures)
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            workRequestBuilder.setConstraints(constraints);
        }

        WorkRequest workRequest = workRequestBuilder.build();
        Operation operation = WorkManager.getInstance(context).enqueue(workRequest);

        return new AbstractMap.SimpleEntry<>(workRequest, operation);
    }

    /**
     * This method should be pulled out to a worker helper class.  This method uses the
     * WorkManagerRequest
     * @param context - application context
     * @param workerId - the tag as well as unique identifier
     * @param clazz - worker class
     * @param data - input data for the worker
     * @param retryInterval - the dispatch retry interval in milli-seconds
     * @return An (WorkRequest, Operation) that can be used for tracing work state
     */
    public static AbstractMap.SimpleEntry<WorkRequest, Operation> startService(Context context, String workerId, Class clazz, Data data, Long retryInterval) {
        // Create a WorkRequest for your Worker and sending it input
        WorkRequest.Builder workRequestBuilder = new OneTimeWorkRequest.Builder(clazz)
                .setInputData(data)
                .addTag(workerId);

        if (requestOnlyWhenConnected) {
            // requests only when connection is available (to control network connection failures)
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            workRequestBuilder.setConstraints(constraints);
        }

        if (retryInterval > 0) {
            // NOTE: retry still enabled by default (LINEAR, 30secs) even when setBackoffCriteria is not called.
            workRequestBuilder.setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    retryInterval,
                    TimeUnit.MILLISECONDS);
        }

        WorkRequest workRequest = workRequestBuilder.build();
        Operation operation = WorkManager.getInstance(context).enqueue(workRequest);

        return new AbstractMap.SimpleEntry<>(workRequest, operation);
    }

}
