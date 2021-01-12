/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                            *
 * *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.android.shared

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.job.JobWorkItem
import android.content.*
import android.os.Build
import android.os.PersistableBundle
import androidx.annotation.RequiresApi
import com.optimizely.ab.android.shared.ServiceScheduler.PendingIntentFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Predicate

/**
 * Schedules [android.app.Service]es to run.
 *
 */
// TODO Unit test coverage
class ServiceScheduler
/**
 * @param context         an instance of [Context]
 * @param pendingIntentFactory an instance of [PendingIntentFactory]
 * @param logger               an instance of [Logger]
 */ constructor(private val context: Context, // @NonNull private final AlarmManager alarmManager;
                private val pendingIntentFactory: PendingIntentFactory, private val logger: Logger) {
    /**
     * Schedule a service starting [Intent] that starts on an interval
     *
     * Previously scheduled services matching this intent will be unscheduled.  They will
     * match even if the interval is different.
     *
     * For API 26 and higher, the JobScheduler is used.  APIs below 26 still use the AlarmService
     *
     * @param intent   an [Intent]
     * @param interval the interval in MS
     */
    fun schedule(intent: Intent, interval: Long) {
        if (interval < 1) {
            logger.error("Tried to schedule an interval less than 1")
            return
        }
        if (isScheduled(intent)) {
            unschedule(intent)
        }
        val pendingIntent: PendingIntent? = pendingIntentFactory.getPendingIntent(intent)
        setRepeating(interval, pendingIntent, intent)
        logger.info("Scheduled {}", intent.getComponent()!!.toShortString())
    }

    private fun setRepeating(interval: Long, pendingIntent: PendingIntent?, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val jobId: Int = getJobId(intent)
            if (jobId == -1) {
                logger.error("Problem getting scheduled job id")
                return
            }
            if (isScheduled(context, jobId)) {
                logger.info("Job already started")
                return
            }
            val jobScheduler: JobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val builder: JobInfo.Builder = JobInfo.Builder(jobId,
                    ComponentName(context.getApplicationContext(),
                            ScheduledJobService::class.java.getName()))
            builder.setPeriodic(interval, interval)
            builder.setPersisted(true)
            // we are only doing repeating on datafile service. it is a prefetch.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.setPrefetch(true)
            }
            builder.setBackoffCriteria(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS, JobInfo.BACKOFF_POLICY_LINEAR)
            intent.putExtra(JobWorkService.Companion.INTENT_EXTRA_JWS_PERIODIC, interval)
            val persistableBundle: PersistableBundle = PersistableBundle()
            for (key: String in intent.getExtras()!!.keySet()) {
                val `object`: Any? = intent.getExtras()!!.get(key)
                when (`object`!!.javaClass.getSimpleName()) {
                    "String" -> persistableBundle.putString(key, `object` as String?)
                    "long", "Long" -> persistableBundle.putLong(key, (`object` as Long?)!!)
                    else -> logger.info("No conversion for {}", `object`.javaClass.getSimpleName())
                }
            }
            persistableBundle.putString(ScheduledJobService.Companion.INTENT_EXTRA_COMPONENT_NAME, intent.getComponent()!!.getClassName())
            builder.setExtras(persistableBundle)
            try {
                if (jobScheduler.schedule(builder.build()) != JobScheduler.RESULT_SUCCESS) {
                    logger.error("ServiceScheduler", "Some error while scheduling the job")
                }
            } catch (e: Exception) {
                logger.error(String.format("Problem scheduling job %s", intent.getComponent()!!.toShortString()), e)
            }
        } else {
            val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, interval, interval, pendingIntent)
        }
    }

    private fun cancelRepeating(pendingIntent: PendingIntent?, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val jobScheduler: JobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val clazz: String = intent.getComponent()!!.getClassName()
            var id: Int? = null
            try {
                id = Class.forName(clazz).getDeclaredField("JOB_ID").get(null) as Int?
                // only cancel periodic services
                if (isScheduled(context, id)) {
                    jobScheduler.cancel((id)!!)
                }
                pendingIntent!!.cancel()
            } catch (e: Exception) {
                logger.error("Error in Cancel ", e)
            }
        } else {
            val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent!!.cancel()
        }
    }

    private fun getJobId(intent: Intent): Int {
        var clazz: String = "unknown"
        var id: Int? = null
        try {
            clazz = intent.getComponent()!!.getClassName()
            id = Class.forName(clazz).getDeclaredField("JOB_ID").get(null) as Int?
        } catch (e: Exception) {
            logger.error("Error getting JOB_ID from " + clazz, e)
        }
        return if (id == null) -1 else id
    }

    /**
     * Unschedule a scheduled [Intent]
     *
     * The [Intent] must equal the intent that was originally sent to [.schedule]
     * @param intent an service starting [Intent] instance
     */
    fun unschedule(intent: Intent?) {
        if (intent != null) {
            try {
                val pendingIntent: PendingIntent? = pendingIntentFactory.getPendingIntent(intent)
                cancelRepeating(pendingIntent, intent)
                logger.info("Unscheduled {}", if (intent.getComponent() != null) intent.getComponent()!!.toShortString() else "intent")
            } catch (e: Exception) {
                logger.debug("Failed to unschedule service", e)
            }
        }
    }

    /**
     * Is an [Intent] for a service scheduled
     * @param intent the intent in question
     * @return is it scheduled or not
     */
    fun isScheduled(intent: Intent?): Boolean {
        return pendingIntentFactory.hasPendingIntent(intent)
    }

    /**
     * Handles the complexities around PendingIntent flags
     *
     * We need to know if the PendingIntent has already been created to prevent pushing
     * the alarm back after the last event.
     *
     * Putting this in it's class makes mocking much easier when testing out [ServiceScheduler.schedule]
     */
    class PendingIntentFactory constructor(private val context: Context) {
        /**
         * Has a [PendingIntent] already been created for the provided [Intent]
         * @param intent an instance of [Intent]
         * @return true if a [PendingIntent] was already created
         */
        fun hasPendingIntent(intent: Intent?): Boolean {
            // FLAG_NO_CREATE will cause null to be returned if this Intent hasn't been created yet.
            // It does matter if you send a new instance or not the equality check is done via
            // the data, action, and component of an Intent.  Ours will always match.
            return getPendingIntent(intent, PendingIntent.FLAG_NO_CREATE) != null
        }

        /**
         * Gets a [PendingIntent] for an [Intent]
         * @param intent an instance of [Intent]
         * @return a [PendingIntent]
         */
        fun getPendingIntent(intent: Intent?): PendingIntent? {
            return getPendingIntent(intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        private fun getPendingIntent(intent: Intent?, flag: Int): PendingIntent? {
            return PendingIntent.getService(context, 0, (intent)!!, flag)
        }
    }

    companion object {
        /**
         * Start a service either through the context or enqueued to the JobService to be run in a minute.
         * For example, the BroadcastReceivers use this to handle all versions of the API.
         *
         * @param context - Application context
         * @param jobId - job id for the job to start if it is a job
         * @param intent - Intent you want to run.
         */
        fun startService(context: Context, jobId: Int, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val jobInfo: JobInfo = JobInfo.Builder(jobId,
                        ComponentName(context, JobWorkService::class.java)) // schedule it to run any time between 1 - 5 minutes
                        .setMinimumLatency(JobWorkService.Companion.ONE_MINUTE.toLong())
                        .setOverrideDeadline((5 * JobWorkService.Companion.ONE_MINUTE).toLong())
                        .build()
                val jobScheduler: JobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                if (jobScheduler.getAllPendingJobs().stream().filter(Predicate { job: JobInfo -> job.getId() == jobId && intent.getExtras()?.equals(job.getExtras()) == true }).count() > 0) {
                    // already pending job. don't run again
                    LoggerFactory.getLogger("ServiceScheduler").info("Job already pending")
                    return
                }
                val jobWorkItem: JobWorkItem = JobWorkItem(intent)
                try {
                    jobScheduler.enqueue(jobInfo, jobWorkItem)
                } catch (e: Exception) {
                    LoggerFactory.getLogger("ServiceScheduler").error("Problem enqueuing work item ", e)
                }
            } else {
                context.startService(intent)
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        private fun isScheduled(context: Context, jobId: Int?): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val jobScheduler: JobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                for (jobInfo: JobInfo in jobScheduler.getAllPendingJobs()) {
                    // we only don't allow rescheduling of periodic jobs.  jobs for individual
                    // intents such as events are allowed and can end up queued in the job service queue.
                    if (jobInfo.getId() == jobId && jobInfo.isPeriodic()) {
                        return true
                    }
                }
            }
            return false
        }
    }
}