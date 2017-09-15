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

import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.app.job.JobWorkItem;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

/**
 * This is an example of implementing a {@link JobService} that dispatches work enqueued in
 * to it.  The class shows how to interact with the service.
 */
//BEGIN_INCLUDE(service)
@RequiresApi(api = Build.VERSION_CODES.O)
public class JobWorkService extends JobService {
    public static final int ONE_MINUTE = 60 * 1000;
    private NotificationManager mNM;
    private CommandProcessor mCurProcessor;
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
                String componentClass = work.getIntent().getComponent().getClassName();
                Class<?> clazz = null;
                Log.i("JobWorkService", "Processing work: " + work + ", component: " + componentClass);
                try {
                    clazz = Class.forName(componentClass);
                    Object intentService = clazz.newInstance();
                    if (intentService instanceof JobWorkScheduledService) {
                        JobWorkScheduledService serviceWorkScheduled = (JobWorkScheduledService) intentService;
                        serviceWorkScheduled.initialize(getApplicationContext());
                        serviceWorkScheduled.onWork(getApplicationContext(), work.getIntent());
                    }
                } catch (Exception e) {
                    Log.e("JobSerivice", "Error creating ServiceWorkScheduled", e);
                }
                // Tell system we have finished processing the work.
                Log.i("JobWorkService", "Done with: " + work);
            }
            if (cancelled) {
                Log.i("JobWorkService", "CANCELLED!");
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
}
//END_INCLUDE(service)