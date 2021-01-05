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
import android.app.job.JobWorkItem;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.RequiresApi;

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
@Deprecated
public class JobWorkService extends JobService {
    public static final String INTENT_EXTRA_JWS_PERIODIC = "com.optimizely.ab.android.shared.JobService.Periodic";
    public static final int ONE_MINUTE = 60 * 1000;
    private CommandProcessor mCurProcessor;
    private int startId = 1;
    Logger logger = LoggerFactory.getLogger(JobWorkService.class);
    /**
     * This is a task to dequeue and process work in the background.
     */
    static final class CommandProcessor extends AsyncTask<Void, Void, Void> {
        private final JobParameters mParams;
        private final Logger logger;
        private final Context context;

        CommandProcessor(JobParameters params, Context context, Logger logger) {
            mParams = params; this.logger = logger; this.context = context;
        }
        @Override
        protected Void doInBackground(Void... params) {
            boolean cancelled;
            JobWorkItem work;
            /**
             * Iterate over available work.  Once dequeueWork() returns null, the
             * job's work queue is empty and the job has stopped, so we can let this
             * async task complete.
             *
             * Even if we are cancelled for any reason, it should still service all items in the queue if it can.
             *
             */
            while (!(cancelled = isCancelled())) {
                try {
                    // This is pertaining to this issue:
                    // https://issuetracker.google.com/issues/63622293
                    // The service was probabably destroyed but we didn't cancel the
                    // processor.  It causes an exception in dequeueWork.
                    // We are also now calling cancel in onDestroy
                    if ((work = mParams.dequeueWork()) == null) {
                        return null;
                    }
                }
                catch (Exception e) {
                    logger.error("Exception in JobWorkService:doInBackground mParams.dequeueWork() ", e);
                    return null;
                }

                final String componentClass = work.getIntent().getComponent().getClassName();
                Class<?> clazz = null;
                logger.info("Processing work: " + work + ", component: " + componentClass);
                try {
                    clazz = Class.forName(componentClass);
                    Object service = clazz.newInstance();
                    setContext((Service) service);

                    if ((cancelled = isCancelled())) {
                        logger.info("JobService was cancelled with items still in the queue.  Attempting to service all items");
                    }

                    if (service instanceof IntentService) {
                        IntentService intentService = (IntentService) service;
                        intentService.onCreate();
                        callOnHandleIntent(intentService, work.getIntent());
                        completeWork(mParams, work);
                    } else {
                        Handler mainHandler = new Handler(context.getApplicationContext().getMainLooper());
                        final Service mainService = (Service)service;
                        final JobWorkItem workItem = work;
                        final Intent manServiceIntent = work.getIntent();

                        mainHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                // run code
                                try {
                                    callOnStartCommand(mainService, manServiceIntent);
                                    completeWork(mParams, workItem);
                                }
                                catch (Exception e) {
                                    logger.error("Problem running service {}", componentClass, e);
                                }
                            }
                        });

                    }
                } catch (Exception e) {
                    logger.error("Error creating ServiceWorkScheduled", e);
                }
                // Tell system we have finished processing the work.
                logger.info("Done with: " + work);
            }
            if (cancelled) {
                logger.error("CANCELLED!");
            }
            return null;
        }
        private void setContext(Service service) {
            callMethod(ContextWrapper.class, service, "attachBaseContext", new Class[] { Context.class }, context.getApplicationContext());
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

        private void completeWork(JobParameters jobParameters, JobWorkItem jobWorkItem) {
            Intent intent = jobWorkItem.getIntent();
            if (intent != null && intent.hasExtra(INTENT_EXTRA_JWS_PERIODIC)) {
                logger.info("Periodic work item completed ");
                jobParameters.completeWork(jobWorkItem);
                //reschedule(jobWorkItem);
            }
            else {
                logger.info("work item completed");
                jobParameters.completeWork(jobWorkItem);

            }

        }

    }
    @Override
    public void onCreate() {

    }

    @Override
    public void onDestroy() {
        // This is pertaining to this issue:
        // https://issuetracker.google.com/issues/63622293
        // The service was probabably destroyed but we didn't cancel the
        // processor.
        if (mCurProcessor != null) {
            mCurProcessor.cancel(true);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        // Start task to pull work out of the queue and process it.
        mCurProcessor = new CommandProcessor(params, getApplicationContext(), this.logger);
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

    private void reschedule(JobWorkItem item) {
        ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler
                .PendingIntentFactory(getApplicationContext());
        ServiceScheduler serviceScheduler = new ServiceScheduler(getApplicationContext(), pendingIntentFactory,
                LoggerFactory.getLogger(ServiceScheduler.class));

        Intent intent = item.getIntent();

        serviceScheduler.schedule(intent, intent.getLongExtra(INTENT_EXTRA_JWS_PERIODIC, -1));

    }
}
//END_INCLUDE(service)