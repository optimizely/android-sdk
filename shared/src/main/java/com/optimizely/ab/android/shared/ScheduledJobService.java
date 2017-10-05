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
import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.annotation.RequiresApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A JobService class used for scheduled just.  We recreate the intent and pass it to the appropriate service.
 */

//BEGIN_INCLUDE(service)
@RequiresApi(api = Build.VERSION_CODES.O)
public class ScheduledJobService extends JobService {
    public static final String INTENT_EXTRA_COMPONENT_NAME = "com.optimizely.ab.android.shared.JobService.ComponentName";
    public static final int ONE_MINUTE = 60 * 1000;
    private CommandProcessor mCurProcessor;
    private int startId = 1;
    Logger logger = LoggerFactory.getLogger(ScheduledJobService.class);
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
            /**
             * Iterate over available work.  Once dequeueWork() returns null, the
             * job's work queue is empty and the job has stopped, so we can let this
             * async task complete.
             *
             * Even if we are cancelled for any reason, it should still service all items in the queue if it can.
             *
             */
                logger.info("Processing schueduled service");
                try {
                    PersistableBundle persistableBundle = mParams.getExtras();
                    Class clazz = Class.forName(persistableBundle.getString(ScheduledJobService.INTENT_EXTRA_COMPONENT_NAME));
                    Object service = clazz.newInstance();
                    setContext((Service) service);

                    Intent intent = new Intent(getApplicationContext(), clazz );

                    for (String key : persistableBundle.keySet()) {
                        Object object = persistableBundle.get(key);
                        switch (object.getClass().getSimpleName()) {
                            case "String":
                                intent.putExtra(key, (String) object);
                                break;
                            case "long":
                            case "Long":
                                intent.putExtra(key, (Long) object);
                                break;
                            default:
                                logger.info("Extra key of type {}", object.getClass().getSimpleName());
                                break;
                        }
                    }

                    if (service instanceof IntentService) {
                        IntentService intentService = (IntentService) service;
                        intentService.onCreate();
                        callOnHandleIntent(intentService, intent);
                        jobFinished(mParams, false);
                    } else {
                        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                        final Service mainService = (Service)service;
                        final Intent manServiceIntent = intent;

                        mainHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                // run code
                                try {
                                    callOnStartCommand(mainService, manServiceIntent);
                                    jobFinished(mParams, false);
                                }
                                catch (Exception e) {
                                    logger.error("Problem running service ", e);
                                }
                            }
                        });

                    }
                } catch (Exception e) {
                    logger.error("Error creating ScheduledJobService", e);
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