/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
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

import android.app.IntentService
import android.app.Service
import android.app.job.JobParameters
import android.app.job.JobService
import android.app.job.JobWorkItem
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import com.optimizely.ab.android.shared.ServiceScheduler.PendingIntentFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException

/**
 * This is adapted from an example of implementing a [JobService] that dispatches work enqueued
 * to it.  The class shows how to interact with the service.  The JobWorkService uses the same intents that are used for pre-AndroidO.
 * It instantiates the service or intent service, sets up its context, and calls the service on the appropriate thread.  All the IntentService
 * or Service needs to do is implement a static public int JOB_ID.  Then, you can use the scheduler to schedule intents and they will run
 * as a job schedulers service or for pre-AndroidO as a AlarmService.
 */
//BEGIN_INCLUDE(service)
@RequiresApi(api = Build.VERSION_CODES.O)
@Deprecated("")
class JobWorkService() : JobService() {
    private var mCurProcessor: CommandProcessor? = null
    private val startId = 1
    var logger = LoggerFactory.getLogger(JobWorkService::class.java)

    /**
     * This is a task to dequeue and process work in the background.
     */
    internal class CommandProcessor(private val mParams: JobParameters, private val context: Context, private val logger: Logger) : AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg p0: Void?): Void? {
            var cancelled: Boolean
            var work: JobWorkItem
            /**
             * Iterate over available work.  Once dequeueWork() returns null, the
             * job's work queue is empty and the job has stopped, so we can let this
             * async task complete.
             *
             * Even if we are cancelled for any reason, it should still service all items in the queue if it can.
             *
             */
            while (!(isCancelled.also { cancelled = it })) {
                try {
                    // This is pertaining to this issue:
                    // https://issuetracker.google.com/issues/63622293
                    // The service was probabably destroyed but we didn't cancel the
                    // processor.  It causes an exception in dequeueWork.
                    // We are also now calling cancel in onDestroy
                    if ((mParams.dequeueWork().also { work = (it)!! }) == null) {
                        return null
                    }
                } catch (e: Exception) {
                    logger.error("Exception in JobWorkService:doInBackground mParams.dequeueWork() ", e)
                    return null
                }
                val componentClass = work.intent.component!!.className
                var clazz: Class<*>? = null
                logger.info("Processing work: $work, component: $componentClass")
                try {
                    clazz = Class.forName(componentClass)
                    val service = clazz.newInstance()
                    setContext(service as Service)
                    if ((isCancelled.also { cancelled = it })) {
                        logger.info("JobService was cancelled with items still in the queue.  Attempting to service all items")
                    }
                    if (service is IntentService) {
                        val intentService = service
                        intentService.onCreate()
                        callOnHandleIntent(intentService, work.intent)
                        completeWork(mParams, work)
                    } else {
                        val mainHandler = Handler(context.applicationContext.mainLooper)
                        val workItem = work
                        val manServiceIntent = work.intent
                        mainHandler.post(Runnable { // run code
                            try {
                                callOnStartCommand(service, manServiceIntent)
                                completeWork(mParams, workItem)
                            } catch (e: Exception) {
                                logger.error("Problem running service {}", componentClass, e)
                            }
                        })
                    }
                } catch (e: Exception) {
                    logger.error("Error creating ServiceWorkScheduled", e)
                }
                // Tell system we have finished processing the work.
                logger.info("Done with: $work")
            }
            if (cancelled) {
                logger.error("CANCELLED!")
            }
            return null
        }

        private fun setContext(service: Service) {
            callMethod(ContextWrapper::class.java, service, "attachBaseContext", arrayOf(Context::class.java), context.applicationContext)
        }

        private fun callOnStartCommand(service: Service, intent: Intent) {
            callMethod(Service::class.java, service, "onStartCommand", arrayOf(Intent::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType), intent, 0, 1)
        }

        private fun callOnHandleIntent(intentService: IntentService, intent: Intent) {
            callMethod(IntentService::class.java, intentService, "onHandleIntent", arrayOf(Intent::class.java), intent)
        }

        private fun callMethod(clazz: Class<*>, `object`: Any, methodName: String, parameterTypes: Array<Class<*>?>, vararg parameters: Any) {
            try {
                val method = clazz.getDeclaredMethod(methodName, *parameterTypes)
                method.isAccessible = true
                method.invoke(`object`, *parameters)
            } catch (e: NoSuchMethodException) {
                logger.error("Error calling method $methodName", e)
            } catch (e: InvocationTargetException) {
                logger.error("Error calling method $methodName", e)
            } catch (e: IllegalAccessException) {
                logger.error("Error calling method $methodName", e)
            }
        }

        private fun completeWork(jobParameters: JobParameters, jobWorkItem: JobWorkItem) {
            val intent = jobWorkItem.intent
            if (intent != null && intent.hasExtra(INTENT_EXTRA_JWS_PERIODIC)) {
                logger.info("Periodic work item completed ")
                jobParameters.completeWork(jobWorkItem)
                //reschedule(jobWorkItem);
            } else {
                logger.info("work item completed")
                jobParameters.completeWork(jobWorkItem)
            }
        }
    }

    override fun onCreate() {}
    override fun onDestroy() {
        // This is pertaining to this issue:
        // https://issuetracker.google.com/issues/63622293
        // The service was probabably destroyed but we didn't cancel the
        // processor.
        if (mCurProcessor != null) {
            mCurProcessor!!.cancel(true)
        }
    }

    override fun onStartJob(params: JobParameters): Boolean {
        // Start task to pull work out of the queue and process it.
        mCurProcessor = CommandProcessor(params, applicationContext, logger)
        mCurProcessor!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        // Allow the job to continue running while we process work.
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        // Have the processor cancel its current work.
        mCurProcessor!!.cancel(true)
        // Tell the system to reschedule the job -- the only reason we would be here is
        // because the job needs to stop for some reason before it has completed all of
        // its work, so we would like it to remain to finish that work in the future.
        return true
    }

    private fun reschedule(item: JobWorkItem) {
        val pendingIntentFactory = PendingIntentFactory(applicationContext)
        val serviceScheduler = ServiceScheduler(applicationContext, pendingIntentFactory,
                LoggerFactory.getLogger(ServiceScheduler::class.java))
        val intent = item.intent
        serviceScheduler.schedule(intent, intent.getLongExtra(INTENT_EXTRA_JWS_PERIODIC, -1))
    }

    companion object {
        @JvmField
        val INTENT_EXTRA_JWS_PERIODIC = "com.optimizely.ab.android.shared.JobService.Periodic"
        @JvmField
        val ONE_MINUTE = 60 * 1000
    }
}