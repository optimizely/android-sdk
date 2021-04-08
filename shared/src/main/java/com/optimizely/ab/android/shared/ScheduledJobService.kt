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
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.Parcelable
import androidx.annotation.RequiresApi
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException

/**
 * A JobService class used for scheduled just.  We recreate the intent and pass it to the appropriate service.
 */
//BEGIN_INCLUDE(service)
@RequiresApi(api = Build.VERSION_CODES.O)
@Deprecated("")
class ScheduledJobService : JobService() {
    private var mCurProcessor: CommandProcessor? = null
    private val startId = 1
    var logger = LoggerFactory.getLogger(ScheduledJobService::class.java)

    /**
     * This is a task to dequeue and process work in the background.
     */
    internal inner class CommandProcessor(private val mParams: JobParameters) : AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg p0: Void?): Void? {
            /**
             * Iterate over available work.  Once dequeueWork() returns null, the
             * job's work queue is empty and the job has stopped, so we can let this
             * async task complete.
             *
             * Even if we are cancelled for any reason, it should still service all items in the queue if it can.
             *
             */
            logger.info("Processing schueduled service")
            try {
                val persistableBundle = mParams.extras
                val clazz = Class.forName(persistableBundle.getString(INTENT_EXTRA_COMPONENT_NAME)!!)
                val service = clazz.newInstance()
                setContext(service as Service)
                val intent = Intent(applicationContext, clazz)
                for (key in persistableBundle.keySet()) {
                    if (key === INTENT_EXTRA_COMPONENT_NAME) {
                        continue
                    }
                    val `object` = persistableBundle[key]
                    when (`object`!!.javaClass.simpleName) {
                        "String" -> intent.putExtra(key, `object` as String?)
                        "long", "Long" -> intent.putExtra(key, `object` as Long?)
                        else -> {
                            logger.info("Extra key of type {}", `object`.javaClass.simpleName)
                            if (`object` is Parcelable) {
                                intent.putExtra(key, `object` as Parcelable?)
                            }
                        }
                    }
                }
                if (service is IntentService) {
                    val intentService = service
                    intentService.onCreate()
                    callOnHandleIntent(intentService, intent)
                    jobFinished(mParams, false)
                } else {
                    val mainHandler = Handler(applicationContext.mainLooper)
                    mainHandler.post { // run code
                        try {
                            callOnStartCommand(service, intent)
                            jobFinished(mParams, false)
                        } catch (e: Exception) {
                            logger.error("Problem running service ", e)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Error creating ScheduledJobService", e)
            }
            return null
        }
    }

    override fun onCreate() {}
    override fun onDestroy() {}
    override fun onStartJob(params: JobParameters): Boolean {
        // Start task to pull work out of the queue and process it.
        mCurProcessor = CommandProcessor(params)
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

    private fun setContext(service: Service) {
        callMethod(ContextWrapper::class.java, service, "attachBaseContext", arrayOf(Context::class.java), applicationContext)
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

    companion object {
        const val INTENT_EXTRA_COMPONENT_NAME = "com.optimizely.ab.android.shared.JobService.ComponentName"
        const val ONE_MINUTE = 60 * 1000
    }
}