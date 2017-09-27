/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
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

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.app.job.JobWorkItem;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This is adapted from an example of implementing a {@link JobService} that dispatches work enqueued
 * to it.  The class shows how to interact with the service.  The JobWorkService uses the same intents that are used for pre-AndroidO.
 * It instantiates the service or intent service, sets up its context, and calls the service on the appropriate thread.  All the IntentService
 * or Service needs to do is implement a static public int JOB_ID.  Then, you can use the scheduler to schedule intents and they will run
 * as a job schedulers service or for pre-AndroidO as a AlarmService.
 */
//BEGIN_INCLUDE(service)
@RequiresApi(api = Build.VERSION_CODES.O)
public class JobWorkService extends JobService {
    public static final int ONE_MINUTE = 60 * 1000;
    private CommandProcessor mCurProcessor;
    private int startId = 1;
    Logger logger = LoggerFactory.getLogger("JobWorkService");
    /**
     * This is a task to dequeue and process work in the background.
     */
    final class CommandProcessor extends AsyncTask<Void, Void, Void> {
        private final JobParameters mParams;
        CommandProcessor(JobParameters params) {
            mParams = params;
        }
        @Override
        protected Void doInBackground(Void... params) {
            boolean cancelled;
            JobWorkItem work;
            /**
             * Iterate over available work.  Once dequeueWork() returns null, the
             * job's work queue is empty and the job has stopped, so we can let this
             * async task complete.
             */
            while (!(cancelled=isCancelled()) && (work=mParams.dequeueWork()) != null) {
                final String componentClass = work.getIntent().getComponent().getClassName();
                Class<?> clazz = null;
                logger.info("JobWorkService", "Processing work: " + work + ", component: " + componentClass);
                try {
                    clazz = Class.forName(componentClass);
                    Object service = clazz.newInstance();
                    setContext((Service) service);

                    if (service instanceof IntentService) {
                        IntentService intentService = (IntentService) service;
                        intentService.onCreate();
                        callOnHandleIntent(intentService, work.getIntent());
                    } else {
                        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                        final Service mainService = (Service)service;
                        final Intent manServiceIntent = work.getIntent();

                        mainHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                // run code
                                try {
                                    callOnStartCommand(mainService, manServiceIntent);
                                }
                                catch (Exception e) {
                                    logger.error("Problem running service {}", componentClass, e);
                                }
                            }
                        });

                    }
                } catch (Exception e) {
                    logger.error("JobSerivice", "Error creating ServiceWorkScheduled", e);
                }
                // Tell system we have finished processing the work.
                logger.info("JobWorkService", "Done with: " + work);
            }
            if (cancelled) {
                logger.error("JobWorkService", "CANCELLED!");
            }
            return null;
        }
    }
    @Override
    public void onCreate() {

    }

    @Override
    public void onDestroy() {
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        // Start task to pull work out of the queue and process it.
        mCurProcessor = new CommandProcessor(params);
        mCurProcessor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        // Allow the job to continue running while we process work.
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // Have the processor cancel its current work.
        mCurProcessor.cancel(true);
        // Tell the system to reschedule the job -- the only reason we would be here is
        // because the job needs to stop for some reason before it has completed all of
        // its work, so we would like it to remain to finish that work in the future.
        return true;
    }

    private void setContext(Service service) {
        callMethod(ContextWrapper.class, service, "attachBaseContext", new Class[] { Context.class }, getApplicationContext());
    }

    private void callOnStartCommand(Service service, Intent intent) {
        callMethod(Service.class, service, "onStartCommand", new Class[] { Intent.class, int.class, int.class}, intent, 0, 1);
    }

    private void callOnHandleIntent(IntentService intentService, Intent intent) {
        callMethod(IntentService.class, intentService, "onHandleIntent", new Class[] { Intent.class }, intent);
    }

    private void callMethod(Class clazz, Object object, String methodName, Class[] parameterTypes, Object... parameters ) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            method.invoke(object, parameters);

        } catch (NoSuchMethodException e) {
           logger.error("Error calling method " + methodName, e);
        } catch (InvocationTargetException e) {
            logger.error("Error calling method " + methodName, e);
        } catch (IllegalAccessException e) {
            logger.error("Error calling method " + methodName, e);
        }

    }
}
//END_INCLUDE(service)