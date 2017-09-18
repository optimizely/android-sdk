/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobWorkItem;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.slf4j.Logger;

/**
 * Schedules {@link android.app.Service}es to run.
 * @hide
 */
// TODO Unit test coverage
public class ServiceScheduler {

   // @NonNull private final AlarmManager alarmManager;
    @NonNull private final PendingIntentFactory pendingIntentFactory;
    @NonNull private final Logger logger;
    @NonNull private final Context context;

    /**
     * @param context         an instance of {@link Context}
     * @param pendingIntentFactory an instance of {@link PendingIntentFactory}
     * @param logger               an instance of {@link Logger}
     * @hide
     */
    public ServiceScheduler(@NonNull Context context, @NonNull PendingIntentFactory pendingIntentFactory, @NonNull Logger logger) {
        this.pendingIntentFactory = pendingIntentFactory;
        this.logger = logger;
        this.context = context;
    }

    /**
     * Schedule a service starting {@link Intent} that starts on an interval
     *
     * Previously scheduled services matching this intent will be unscheduled.  They will
     * match even if the interval is different.
     *
     * @param intent   an {@link Intent}
     * @param interval the interval in MS
     * @hide
     */
    public void schedule(Intent intent, long interval) {
        if (interval < 1) {
            logger.error("Tried to schedule an interval less than 1");
            return;
        }

        if (isScheduled(intent)) {
            unschedule(intent);
        }

        PendingIntent pendingIntent = pendingIntentFactory.getPendingIntent(intent);

        setRepeating(interval, pendingIntent, intent);

        logger.info("Scheduled {}", intent.getComponent().toShortString());
    }


    private void setRepeating(long interval, PendingIntent pendingIntent, Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int jobId = getJobId(intent);
            if (jobId == -1) {
                logger.error("Problem getting job id");
                return;
            }

            JobScheduler jobScheduler = (JobScheduler)
                    context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            JobInfo.Builder builder = new JobInfo.Builder(jobId,
                    new ComponentName(context.getApplicationContext(),
                            JobWorkService.class.getName()));
            builder.setPeriodic(interval, interval);
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

            if (jobScheduler.enqueue(builder.build(), new JobWorkItem(intent)) <= 0) {
                Log.e("ServiceScheduler", "Some error while scheduling the job");
            }

        }
        else {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, interval, interval, pendingIntent);
        }
    }

    private void cancelRepeating(PendingIntent pendingIntent, Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            JobScheduler jobScheduler = (JobScheduler)
                    context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            String clazz = intent.getComponent().getClassName();
            Integer id = null;
            try {
                id = (Integer) Class.forName(clazz).getDeclaredField("JOB_ID").get(null);
                jobScheduler.cancel(id);
                pendingIntent.cancel();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        else {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private int getJobId(Intent intent) {
        String clazz = intent.getComponent().getClassName();
        Integer id = null;
        try {
            id = (Integer) Class.forName(clazz).getDeclaredField("JOB_ID").get(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return id == null ? -1 : id;
    }

    /**
     * Unschedule a scheduled {@link Intent}
     *
     * The {@link Intent} must equal the intent that was originally sent to {@link #schedule(Intent, long)}
     * @param intent an service starting {@link Intent} instance
     * @hide
     */
    public void unschedule(Intent intent) {
        if (intent != null) {
            try {
                PendingIntent pendingIntent = pendingIntentFactory.getPendingIntent(intent);
                cancelRepeating(pendingIntent, intent);
                logger.info("Unscheduled {}", intent.getComponent().toShortString());
            } catch (Exception e) {
                logger.debug("Failed to unschedule service", e);
            }
        }
    }

    /**
     * Is an {@link Intent} for a service scheduled
     * @param intent the intent in question
     * @return is it scheduled or not
     * @hide
     */
    public boolean isScheduled(Intent intent) {
        return pendingIntentFactory.hasPendingIntent(intent);
    }

    /**
     * Handles the complexities around PendingIntent flags
     *
     * We need to know if the PendingIntent has already been created to prevent pushing
     * the alarm back after the last event.
     *
     * Putting this in it's class makes mocking much easier when testing out {@link ServiceScheduler#schedule(Intent, long)}
     * @hide
     */
    public static class PendingIntentFactory {

        private Context context;

        public PendingIntentFactory(Context context) {
            this.context = context;
        }

        /**
         * Has a {@link PendingIntent} already been created for the provided {@link Intent}
         * @param intent an instance of {@link Intent}
         * @return true if a {@link PendingIntent} was already created
         * @hide
         */
        public boolean hasPendingIntent(Intent intent) {
            // FLAG_NO_CREATE will cause null to be returned if this Intent hasn't been created yet.
            // It does matter if you send a new instance or not the equality check is done via
            // the data, action, and component of an Intent.  Ours will always match.
            return getPendingIntent(intent, PendingIntent.FLAG_NO_CREATE) != null;
        }

        /**
         * Gets a {@link PendingIntent} for an {@link Intent}
         * @param intent an instance of {@link Intent}
         * @return a {@link PendingIntent}
         * @hide
         */
        public PendingIntent getPendingIntent(Intent intent) {
            return getPendingIntent(intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        private PendingIntent getPendingIntent(Intent intent, int flag) {
            return PendingIntent.getService(context, 0, intent, flag);
        }
    }

    public static void startService(Context context, Integer jobId, Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ServiceScheduler.getScheduled(context, jobId) != null) {
                return;
            }
            JobInfo jobInfo = new JobInfo.Builder(jobId,
                    new ComponentName(context, JobWorkService.class))
                    // schedule it to run any time between 1 - 5 minutes
                    .setMinimumLatency(JobWorkService.ONE_MINUTE)
                    .setOverrideDeadline(5 * JobWorkService.ONE_MINUTE)
                    .build();
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            jobScheduler.enqueue(jobInfo, new JobWorkItem(intent));

        }
        else {
            context.startService(intent);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static JobInfo getScheduled(Context context, Integer jobId) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == jobId) {
                return jobInfo;
            }
        }

        return null;
    }



}
