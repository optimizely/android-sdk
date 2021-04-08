/****************************************************************************
 * Copyright 2017-2021, Optimizely, Inc. and contributors                   *
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
package com.optimizely.ab.android.sdk

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.optimizely.ab.android.datafile_handler.DatafileHandler
import com.optimizely.ab.android.datafile_handler.DatafileLoadedListener
import com.optimizely.ab.android.datafile_handler.DatafileService
import com.optimizely.ab.android.datafile_handler.DefaultDatafileHandler
import com.optimizely.ab.android.event_handler.DefaultEventHandler
import com.optimizely.ab.android.sdk.OptimizelyManager.Companion.builder
import com.optimizely.ab.android.shared.DatafileConfig
import com.optimizely.ab.android.shared.ServiceScheduler
import com.optimizely.ab.android.shared.ServiceScheduler.PendingIntentFactory
import com.optimizely.ab.android.user_profile.DefaultUserProfileService
import com.optimizely.ab.android.user_profile.DefaultUserProfileService.StartCallback
import com.optimizely.ab.bucketing.UserProfileService
import com.optimizely.ab.config.DatafileProjectConfig
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.parser.ConfigParseException
import com.optimizely.ab.event.EventHandler
import com.optimizely.ab.event.EventProcessor
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.slf4j.Logger
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Tests for [OptimizelyManager]
 */
@RunWith(AndroidJUnit4::class)
class OptimizelyManagerTest {
    private val testProjectId = "7595190003"
    private val testSdkKey = "EQRZ12XAR22424"
    private var executor: ExecutorService? = null
    private var logger: Logger? = null
    private var optimizelyManager: OptimizelyManager? = null
    private var defaultDatafileHandler: DefaultDatafileHandler? = null
    private var defaultDatafile: String? = null
    private val minDatafile = """
         {
         experiments: [ ],
         version: "2",
         audiences: [ ],
         groups: [ ],
         attributes: [ ],
         projectId: "$testProjectId",
         accountId: "6365361536",
         events: [ ],
         revision: "1"
         }
         """.trimIndent()

    @Before
    @Throws(Exception::class)
    fun setup() {
        logger = Mockito.mock(Logger::class.java)
        executor = Executors.newSingleThreadExecutor()
        defaultDatafileHandler = Mockito.mock(DefaultDatafileHandler::class.java)
        val eventHandler: EventHandler = Mockito.mock(DefaultEventHandler::class.java)
        val eventProcessor = Mockito.mock(EventProcessor::class.java)
        optimizelyManager = builder(testProjectId)
                .withLogger(logger)
                .withDatafileDownloadInterval(3600L)
                .withDatafileHandler(defaultDatafileHandler)
                .withEventDispatchInterval(3600L)
                .withEventHandler(eventHandler)
                .withEventProcessor(eventProcessor)
                .build(InstrumentationRegistry.getInstrumentation().targetContext)
        defaultDatafile = optimizelyManager?.getDatafile(InstrumentationRegistry.getInstrumentation().targetContext, R.raw.datafile)
        val config = DatafileProjectConfig.Builder().withDatafile(defaultDatafile).build()
        Mockito.`when`(defaultDatafileHandler!!.getConfig()).thenReturn(config)
    }

    @Test
    fun initializeIntUseForcedVariation() {
        optimizelyManager!!.initialize(InstrumentationRegistry.getInstrumentation().targetContext, R.raw.datafile)
        Assert.assertTrue(optimizelyManager!!.optimizely.setForcedVariation("android_experiment_key", "1", "var_1"))
        val variation = optimizelyManager!!.optimizely.getForcedVariation("android_experiment_key", "1")
        Assert.assertEquals(variation!!.key, "var_1")
        Assert.assertTrue(optimizelyManager!!.optimizely.setForcedVariation("android_experiment_key", "1", null))
    }

    @Test
    fun initializeInt() {
        optimizelyManager!!.initialize(InstrumentationRegistry.getInstrumentation().targetContext, R.raw.datafile)
        Assert.assertEquals(optimizelyManager!!.isDatafileCached(InstrumentationRegistry.getInstrumentation().targetContext), false)
        Assert.assertEquals(optimizelyManager!!.datafileUrl, "https://cdn.optimizely.com/json/7595190003.json")
        Mockito.verify(optimizelyManager!!.datafileHandler).startBackgroundUpdates(Matchers.eq(InstrumentationRegistry.getInstrumentation().targetContext), Matchers.eq(DatafileConfig(testProjectId, null)), Matchers.eq(3600L), Matchers.any(DatafileLoadedListener::class.java))
        Assert.assertNotNull(optimizelyManager!!.optimizely)
        Assert.assertNotNull(optimizelyManager!!.datafileHandler)
    }

    @Test
    fun initializeSyncWithoutEnvironment() {
        /*
         * Scenario#1: when datafile is not Empty
         * Scenario#2: when datafile is Empty
         */
        optimizelyManager!!.initialize(InstrumentationRegistry.getInstrumentation().targetContext, R.raw.datafile)
        Assert.assertEquals(optimizelyManager!!.isDatafileCached(InstrumentationRegistry.getInstrumentation().targetContext), false)
        Assert.assertEquals(optimizelyManager!!.datafileUrl, "https://cdn.optimizely.com/json/7595190003.json")
        Assert.assertNotNull(optimizelyManager!!.optimizely)
        Assert.assertNotNull(optimizelyManager!!.datafileHandler)
        optimizelyManager!!.initialize(InstrumentationRegistry.getInstrumentation().targetContext, null as Int?)
        Mockito.verify(logger)?.error(Matchers.eq("Invalid datafile resource ID."))
    }

    @Test
    fun initializeSyncWithEnvironment() {
        val logger = Mockito.mock(Logger::class.java)
        val datafileHandler: DatafileHandler = Mockito.mock(DefaultDatafileHandler::class.java)
        val eventHandler: EventHandler = Mockito.mock(DefaultEventHandler::class.java)
        val eventProcessor = Mockito.mock(EventProcessor::class.java)
        val optimizelyManager = OptimizelyManager(testProjectId, testSdkKey, null, logger, 3600L, datafileHandler, null, 3600L,
                eventHandler, eventProcessor, null, null, null)
        /*
         * Scenario#1: when datafile is not Empty
         * Scenario#2: when datafile is Empty
        */optimizelyManager.initialize(InstrumentationRegistry.getInstrumentation().targetContext, R.raw.datafile)
        Assert.assertEquals(optimizelyManager.isDatafileCached(InstrumentationRegistry.getInstrumentation().targetContext), false)
        Assert.assertEquals(optimizelyManager.datafileUrl, String.format(DatafileConfig.defaultHost + DatafileConfig.environmentUrlSuffix, testSdkKey))
        Assert.assertNotNull(optimizelyManager.optimizely)
        Assert.assertNotNull(optimizelyManager.datafileHandler)
        optimizelyManager.initialize(InstrumentationRegistry.getInstrumentation().targetContext, null as Int?)
        Mockito.verify(logger).error(Matchers.eq("Invalid datafile resource ID."))
    }

    @Test
    fun initializeSyncWithEmptyDatafile() {
        //for this case to pass empty the data file or enter any garbage data given on R.raw.emptydatafile this path
        val context = Mockito.mock(Context::class.java)
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(context.applicationContext).thenReturn(appContext)
        Mockito.`when`(appContext.packageName).thenReturn("com.optly")
        Mockito.`when`(defaultDatafileHandler!!.config).thenReturn(null)
        optimizelyManager!!.initialize(InstrumentationRegistry.getInstrumentation().targetContext, R.raw.emptydatafile)
        Assert.assertFalse(optimizelyManager!!.optimizely.isValid)
    }

    //for this case to pass empty the data file or enter any garbage data given on R.raw.emptydatafile this path
    @get:Test
    val emptyDatafile: Unit
        get() {
            //for this case to pass empty the data file or enter any garbage data given on R.raw.emptydatafile this path
            val context = Mockito.mock(Context::class.java)
            val appContext = Mockito.mock(Context::class.java)
            Mockito.`when`(context.applicationContext).thenReturn(appContext)
            Mockito.`when`(appContext.packageName).thenReturn("com.optly")
            val datafile = optimizelyManager!!.getDatafile(InstrumentationRegistry.getInstrumentation().targetContext, R.raw.emptydatafile)
            Assert.assertNotNull(datafile, "")
        }

    /*
         * Scenario#1: when datafile is Cached
         *  Scenario#2: when datafile is not cached and raw datafile is not empty
        */
    @get:Test
    val datafile: Unit
        get() {
            /*
         * Scenario#1: when datafile is Cached
         *  Scenario#2: when datafile is not cached and raw datafile is not empty
        */
            Assert.assertEquals(optimizelyManager?.isDatafileCached(InstrumentationRegistry.getInstrumentation().targetContext), false)
            val datafile = optimizelyManager?.getDatafile(InstrumentationRegistry.getInstrumentation().targetContext, R.raw.datafile)
            Assert.assertEquals(optimizelyManager!!.datafileUrl, String.format("https://cdn.optimizely.com/json/%s.json", testProjectId))
            Assert.assertNotNull(datafile)
            Assert.assertNotNull(optimizelyManager!!.datafileHandler)
        }

    @Test
    fun initializeAsyncWithEnvironment() {
        val logger = Mockito.mock(Logger::class.java)
        val datafileHandler: DatafileHandler = Mockito.mock(DefaultDatafileHandler::class.java)
        val eventHandler: EventHandler = Mockito.mock(DefaultEventHandler::class.java)
        val eventProcessor = Mockito.mock(EventProcessor::class.java)
        val optimizelyManager = OptimizelyManager(testProjectId, testSdkKey, null, logger, 3600L, datafileHandler, null, 3600L,
                eventHandler, eventProcessor, null, null, null)

        /*
         * Scenario#1: when datafile is not Empty
         * Scenario#2: when datafile is Empty
        */Mockito.doAnswer { invocation ->
            (invocation.arguments[2] as DatafileLoadedListener).onDatafileLoaded(null)
            null
        }.`when`(optimizelyManager.datafileHandler).downloadDatafile(Matchers.any(Context::class.java), Matchers.any(DatafileConfig::class.java),
                Matchers.any(DatafileLoadedListener::class.java))
        val listener: OptimizelyStartListener = object : OptimizelyStartListener {
            override fun onStart(optimizely: OptimizelyClient?) {
                Assert.assertNotNull(optimizelyManager.optimizely)
                Assert.assertNotNull(optimizelyManager.datafileHandler)
                Assert.assertNull(optimizelyManager.optimizelyStartListener)
            }
        }
        optimizelyManager.initialize(InstrumentationRegistry.getInstrumentation().context, R.raw.datafile, listener)
        Mockito.verify(optimizelyManager.datafileHandler).startBackgroundUpdates(Matchers.any(Context::class.java), Matchers.eq(DatafileConfig(testProjectId, testSdkKey)), Matchers.eq(3600L), Matchers.any(DatafileLoadedListener::class.java))
        Assert.assertEquals(optimizelyManager.isDatafileCached(InstrumentationRegistry.getInstrumentation().targetContext), false)
        Assert.assertEquals(optimizelyManager.datafileUrl, String.format(DatafileConfig.defaultHost + DatafileConfig.environmentUrlSuffix, testSdkKey))
    }

    @Test
    fun initializeAsyncWithoutEnvironment() {
        /*
         * Scenario#1: when datafile is not Empty
         * Scenario#2: when datafile is Empty
         */
        optimizelyManager!!.initialize(InstrumentationRegistry.getInstrumentation().context, R.raw.datafile, object : OptimizelyStartListener {
            override fun onStart(optimizely: OptimizelyClient?) {
                Assert.assertNotNull(optimizelyManager!!.optimizely)
                Assert.assertNotNull(optimizelyManager!!.datafileHandler)
                Assert.assertNull(optimizelyManager!!.optimizelyStartListener)
            }
        })
        Assert.assertEquals(optimizelyManager!!.isDatafileCached(InstrumentationRegistry.getInstrumentation().targetContext), false)
        Assert.assertEquals(optimizelyManager!!.datafileUrl, "https://cdn.optimizely.com/json/7595190003.json")
    }

    @Test
    fun initializeWithEmptyDatafile() {
        val context = Mockito.mock(Context::class.java)
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(context.applicationContext).thenReturn(appContext)
        Mockito.`when`(appContext.packageName).thenReturn("com.optly")
        Mockito.`when`(defaultDatafileHandler!!.config).thenReturn(null)
        val emptyString = ""
        optimizelyManager!!.initialize(context, emptyString)
        Assert.assertFalse(optimizelyManager!!.optimizely.isValid)
    }

    @Test
    fun initializeWithMalformedDatafile() {
        val context = Mockito.mock(Context::class.java)
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(context.applicationContext).thenReturn(appContext)
        Mockito.`when`(appContext.packageName).thenReturn("com.optly")
        Mockito.`when`(defaultDatafileHandler!!.config).thenReturn(null)
        val emptyString = "malformed data"
        optimizelyManager!!.initialize(context, emptyString)
        Assert.assertFalse(optimizelyManager!!.optimizely.isValid)
    }

    @Test
    fun initializeWithNullDatafile() {
        val context = Mockito.mock(Context::class.java)
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(context.applicationContext).thenReturn(appContext)
        Mockito.`when`(appContext.packageName).thenReturn("com.optly")
        val emptyString: String? = null
        optimizelyManager!!.initialize(context, emptyString!!)
        Mockito.verify(logger)?.error(Matchers.eq("Invalid datafile"))
    }

    @Test
    fun initializeAsyncWithNullDatafile() {
        optimizelyManager!!.initialize(InstrumentationRegistry.getInstrumentation().context, object : OptimizelyStartListener {
            override fun onStart(optimizely: OptimizelyClient?) {
                Assert.assertNotNull(optimizely)
            }
        })
    }

    @Test
    fun load() {
        val context = Mockito.mock(Context::class.java)
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(context.applicationContext).thenReturn(appContext)
        Mockito.`when`(appContext.packageName).thenReturn("com.optly")
        val emptyString: String? = null
        optimizelyManager!!.initialize(context, emptyString!!)
        Mockito.verify(logger)?.error(Matchers.eq("Invalid datafile"))
    }

    @Test
    fun stop() {
        val context = Mockito.mock(Context::class.java)
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(context.applicationContext).thenReturn(appContext)
        optimizelyManager!!.datafileHandler.downloadDatafile(context, optimizelyManager!!.datafileConfig, null)
        optimizelyManager!!.stop(context)
        Assert.assertNull(optimizelyManager!!.optimizelyStartListener)
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    fun injectOptimizely() {
        val context = Mockito.mock(Context::class.java)
        val userProfileService = Mockito.mock(UserProfileService::class.java)
        val startListener = Mockito.mock(OptimizelyStartListener::class.java)
        optimizelyManager!!.optimizelyStartListener = startListener
        optimizelyManager!!.injectOptimizely(context, userProfileService, minDatafile)
        try {
            executor!!.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Timed out")
        }
        Mockito.verify(logger)?.info("Sending Optimizely instance to listener")
        Mockito.verify(startListener).onStart(Matchers.any(OptimizelyClient::class.java))
        Mockito.verify(optimizelyManager!!.datafileHandler).startBackgroundUpdates(Matchers.eq(context), Matchers.eq(DatafileConfig(testProjectId, null)), Matchers.eq(3600L), Matchers.any(DatafileLoadedListener::class.java))
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    fun injectOptimizelyWithDatafileLisener() {
        val context = Mockito.mock(Context::class.java)
        val userProfileService = Mockito.mock(UserProfileService::class.java)
        val startListener = Mockito.mock(OptimizelyStartListener::class.java)
        optimizelyManager!!.optimizelyStartListener = startListener
        optimizelyManager!!.injectOptimizely(context, userProfileService, minDatafile)
        try {
            executor!!.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Timed out")
        }
        Mockito.verify(optimizelyManager!!.datafileHandler).startBackgroundUpdates(Matchers.eq(context), Matchers.eq(DatafileConfig(testProjectId, null)), Matchers.eq(3600L), Matchers.any(DatafileLoadedListener::class.java))
        Mockito.verify(logger)?.info("Sending Optimizely instance to listener")
        Mockito.verify(startListener).onStart(Matchers.any(OptimizelyClient::class.java))
    }

    @Test
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun injectOptimizelyNullListener() {
        val context = Mockito.mock(Context::class.java)
        val packageManager = Mockito.mock(PackageManager::class.java)
        Mockito.`when`(context.packageName).thenReturn("com.optly")
        Mockito.`when`(context.applicationContext).thenReturn(context)
        Mockito.`when`(context.applicationContext.packageManager).thenReturn(packageManager)
        try {
            Mockito.`when`(packageManager.getPackageInfo("com.optly", 0)).thenReturn(Mockito.mock(PackageInfo::class.java))
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        val userProfileService = Mockito.mock(UserProfileService::class.java)
        val serviceScheduler = Mockito.mock(ServiceScheduler::class.java)
        val captor = ArgumentCaptor.forClass(Intent::class.java)
        val callbackArgumentCaptor = ArgumentCaptor.forClass(StartCallback::class.java)
        optimizelyManager!!.optimizelyStartListener = null
        optimizelyManager!!.injectOptimizely(context, userProfileService, minDatafile)
        val alarmManager = context
                .getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntentFactory = PendingIntentFactory(context)
        val intent = Intent(context, DatafileService::class.java)
        intent.putExtra(DatafileService.EXTRA_DATAFILE_CONFIG, optimizelyManager!!.datafileConfig?.toJSONString())
        serviceScheduler.schedule(intent, optimizelyManager!!.datafileDownloadInterval * 1000)
        try {
            executor!!.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Timed out")
        }
        Mockito.verify(logger)?.info("No listener to send Optimizely to")
        Mockito.verify(serviceScheduler).schedule(captor.capture(), Matchers.eq(TimeUnit.HOURS.toMillis(1L)))
        val intent2 = captor.value
        Assert.assertTrue(intent2.component!!.shortClassName.contains("DatafileService"))
        Assert.assertEquals(optimizelyManager!!.datafileConfig?.toJSONString(), intent2.getStringExtra(DatafileService.EXTRA_DATAFILE_CONFIG))
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    fun injectOptimizelyHandlesInvalidDatafile() {
        val context = Mockito.mock(Context::class.java)
        val packageManager = Mockito.mock(PackageManager::class.java)
        Mockito.`when`(context.packageName).thenReturn("com.optly")
        Mockito.`when`(context.applicationContext).thenReturn(context)
        Mockito.`when`(context.applicationContext.packageManager).thenReturn(packageManager)
        try {
            Mockito.`when`(packageManager.getPackageInfo("com.optly", 0)).thenReturn(Mockito.mock(PackageInfo::class.java))
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        val userProfileService = Mockito.mock(DefaultUserProfileService::class.java)
        val captor = ArgumentCaptor.forClass(Intent::class.java)
        val callbackArgumentCaptor = ArgumentCaptor.forClass(StartCallback::class.java)
        Mockito.`when`(defaultDatafileHandler!!.config).thenReturn(null)
        optimizelyManager!!.optimizelyStartListener = null
        optimizelyManager!!.injectOptimizely(context, userProfileService, "{}")
        try {
            executor!!.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Timed out")
        }
        Assert.assertFalse(optimizelyManager!!.optimizely.isValid)
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    fun injectOptimizelyDoesNotDuplicateCallback() {
        val context = Mockito.mock(Context::class.java)
        val packageManager = Mockito.mock(PackageManager::class.java)
        Mockito.`when`(context.packageName).thenReturn("com.optly")
        Mockito.`when`(context.applicationContext).thenReturn(context)
        Mockito.`when`(context.applicationContext.packageManager).thenReturn(packageManager)
        try {
            Mockito.`when`(packageManager.getPackageInfo("com.optly", 0)).thenReturn(Mockito.mock(PackageInfo::class.java))
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        val userProfileService = Mockito.mock(UserProfileService::class.java)
        val startListener = Mockito.mock(OptimizelyStartListener::class.java)
        optimizelyManager!!.optimizelyStartListener = startListener
        optimizelyManager!!.injectOptimizely(context, userProfileService, minDatafile)
        try {
            executor!!.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Assert.fail("Timed out")
        }
        Mockito.verify(logger)?.info("Sending Optimizely instance to listener")
        Mockito.verify(startListener).onStart(Matchers.any(OptimizelyClient::class.java))
    }

    // Init Sync Flows
    @Test
    fun initializeSyncWithUpdateOnNewDatafileDisabled() {
        val downloadToCache = true
        val updateConfigOnNewDatafile = false
        val pollingInterval = 0 // disable polling
        val datafileHandler = Mockito.spy(DefaultDatafileHandler())
        val logger = Mockito.mock(Logger::class.java)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval.toLong(), datafileHandler, null, 0,
                null, null, null, null, null)
        Mockito.doAnswer {
            val newDatafile = manager.getDatafile(context, R.raw.datafile_api)
            datafileHandler.saveDatafile(context, manager.datafileConfig, newDatafile)
            null
        }.`when`(manager.datafileHandler).downloadDatafile(Matchers.any(Context::class.java), Matchers.any(DatafileConfig::class.java), Matchers.any(DatafileLoadedListener::class.java))
        val client = manager.initialize(context, defaultDatafile, downloadToCache, updateConfigOnNewDatafile)
        try {
            executor!!.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            //
        }
        Assert.assertEquals(client.optimizelyConfig!!.revision, "7")
    }

    @Test
    fun initializeSyncWithUpdateOnNewDatafileEnabled() {
        val downloadToCache = true
        val updateConfigOnNewDatafile = true
        val pollingInterval = 0 // disable polling
        val datafileHandler = Mockito.spy(DefaultDatafileHandler())
        val logger = Mockito.mock(Logger::class.java)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval.toLong(), datafileHandler, null, 0,
                null, null, null, null, null)
        Mockito.doAnswer {
            val newDatafile = manager.getDatafile(context, R.raw.datafile_api)
            datafileHandler.saveDatafile(context, manager.datafileConfig, newDatafile)
            null
        }.`when`(manager.datafileHandler).downloadDatafile(Matchers.any(Context::class.java), Matchers.any(DatafileConfig::class.java), Matchers.any(DatafileLoadedListener::class.java))
        val client = manager.initialize(context, defaultDatafile, downloadToCache, updateConfigOnNewDatafile)
        try {
            executor!!.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            //
        }
        Assert.assertEquals(client.optimizelyConfig!!.revision, "241")
    }

    @Test
    fun initializeSyncWithDownloadToCacheDisabled() {
        val downloadToCache = false
        val updateConfigOnNewDatafile = true
        val pollingInterval = 0 // disable polling
        val datafileHandler = Mockito.spy(DefaultDatafileHandler())
        val logger = Mockito.mock(Logger::class.java)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval.toLong(), datafileHandler, null, 0,
                null, null, null, null, null)
        Mockito.doAnswer {
            val newDatafile = manager.getDatafile(context, R.raw.datafile_api)
            datafileHandler.saveDatafile(context, manager.datafileConfig, newDatafile)
            null
        }.`when`(manager.datafileHandler).downloadDatafile(Matchers.any(Context::class.java), Matchers.any(DatafileConfig::class.java), Matchers.any(DatafileLoadedListener::class.java))
        val client = manager.initialize(context, defaultDatafile, downloadToCache, updateConfigOnNewDatafile)
        try {
            executor!!.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            //
        }
        Assert.assertEquals(client.optimizelyConfig!!.revision, "7")
    }

    @Test
    fun initializeSyncWithUpdateOnNewDatafileDisabledWithPeriodicPollingEnabled() {
        val downloadToCache = true
        val updateConfigOnNewDatafile = false
        val pollingInterval = 30 // enable polling
        val datafileHandler = Mockito.spy(DefaultDatafileHandler())
        val logger = Mockito.mock(Logger::class.java)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval.toLong(), datafileHandler, null, 0,
                null, null, null, null, null)
        Mockito.doAnswer(
                Answer<Any?> { invocation: InvocationOnMock? ->
                    val newDatafile = manager.getDatafile(context, R.raw.datafile_api)
                    datafileHandler.saveDatafile(context, manager.datafileConfig, newDatafile)
                    null
                }).`when`(manager.datafileHandler).downloadDatafile(Matchers.any(Context::class.java), Matchers.any(DatafileConfig::class.java), Matchers.any(DatafileLoadedListener::class.java))
        val client = manager.initialize(context, defaultDatafile, downloadToCache, updateConfigOnNewDatafile)
        try {
            executor!!.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            //
        }

        // when periodic polling enabled, project config always updated on cache datafile update (regardless of "updateConfigOnNewDatafile" setting)
        Assert.assertEquals(client.optimizelyConfig!!.revision, "241") // wait for first download.
    }

    @Test
    fun initializeSyncWithUpdateOnNewDatafileEnabledWithPeriodicPollingEnabled() {
        val downloadToCache = true
        val updateConfigOnNewDatafile = true
        val pollingInterval = 30 // enable polling
        val datafileHandler = Mockito.spy(DefaultDatafileHandler())
        val logger = Mockito.mock(Logger::class.java)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval.toLong(), datafileHandler, null, 0,
                null, null, null, null, null)
        Mockito.doAnswer {
            val newDatafile = manager.getDatafile(context, R.raw.datafile_api)
            datafileHandler.saveDatafile(context, manager.datafileConfig, newDatafile)
            null
        }.`when`(manager.datafileHandler).downloadDatafile(Matchers.any(Context::class.java), Matchers.any(DatafileConfig::class.java), Matchers.any(DatafileLoadedListener::class.java))
        val client = manager.initialize(context, defaultDatafile, downloadToCache, updateConfigOnNewDatafile)
        try {
            executor!!.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            //
        }
        Assert.assertEquals(client.optimizelyConfig!!.revision, "241")
    }

    @Test
    fun initializeSyncWithUpdateOnNewDatafileDisabledWithPeriodicPollingDisabled() {
        val downloadToCache = true
        val updateConfigOnNewDatafile = false
        val pollingInterval = 0 // disable polling
        val datafileHandler = Mockito.spy(DefaultDatafileHandler())
        val logger = Mockito.mock(Logger::class.java)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval.toLong(), datafileHandler, null, 0,
                null, null, null, null, null)
        Mockito.doAnswer {
            val newDatafile = manager.getDatafile(context, R.raw.datafile_api)
            datafileHandler.saveDatafile(context, manager.datafileConfig, newDatafile)
            null
        }.`when`(manager.datafileHandler).downloadDatafile(Matchers.any(Context::class.java), Matchers.any(DatafileConfig::class.java), Matchers.any(DatafileLoadedListener::class.java))
        val client = manager.initialize(context, defaultDatafile, downloadToCache, updateConfigOnNewDatafile)
        try {
            executor!!.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            //
        }

        // when periodic polling enabled, project config always updated on cache datafile update (regardless of "updateConfigOnNewDatafile" setting)
        Assert.assertEquals(client.optimizelyConfig!!.revision, "7") // wait for first download.
    }

    @Test
    fun initializeSyncWithUpdateOnNewDatafileEnabledWithPeriodicPollingDisabled() {
        val downloadToCache = true
        val updateConfigOnNewDatafile = true
        val pollingInterval = 0 // disable polling
        val datafileHandler = Mockito.spy(DefaultDatafileHandler())
        val logger = Mockito.mock(Logger::class.java)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval.toLong(), datafileHandler, null, 0,
                null, null, null, null, null)
        Mockito.doAnswer {
            val newDatafile = manager.getDatafile(context, R.raw.datafile_api)
            datafileHandler.saveDatafile(context, manager.datafileConfig, newDatafile)
            null
        }.`when`(manager.datafileHandler).downloadDatafile(Matchers.any(Context::class.java), Matchers.any(DatafileConfig::class.java), Matchers.any(DatafileLoadedListener::class.java))
        val client = manager.initialize(context, defaultDatafile, downloadToCache, updateConfigOnNewDatafile)
        try {
            executor!!.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            //
        }
        Assert.assertEquals(client.optimizelyConfig!!.revision, "241")
    }

    @Test
    fun initializeSyncWithResourceDatafileNoCache() {
        val downloadToCache = true
        val updateConfigOnNewDatafile = true
        val pollingInterval = 30 // enable polling
        val datafileHandler = Mockito.spy(DefaultDatafileHandler())
        val logger = Mockito.mock(Logger::class.java)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = Mockito.spy(OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval.toLong(), datafileHandler, null, 0,
                null, null, null, null, null))
        datafileHandler.removeSavedDatafile(context, manager.datafileConfig)
        val client = manager.initialize(context, R.raw.datafile, downloadToCache, updateConfigOnNewDatafile)
        Mockito.verify(manager).initialize(Matchers.eq(context), Matchers.eq(defaultDatafile), Matchers.eq(downloadToCache), Matchers.eq(updateConfigOnNewDatafile))
    }

    @Test
    fun initializeSyncWithResourceDatafileNoCacheWithDefaultParams() {
        val pollingInterval = 30 // enable polling
        val datafileHandler = Mockito.spy(DefaultDatafileHandler())
        val logger = Mockito.mock(Logger::class.java)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = Mockito.spy(OptimizelyManager(testProjectId, testSdkKey, null, logger, pollingInterval.toLong(), datafileHandler, null, 0,
                null, null, null, null, null))
        datafileHandler.removeSavedDatafile(context, manager.datafileConfig)
        val client = manager.initialize(context, R.raw.datafile)
        Mockito.verify(manager).initialize(Matchers.eq(context), Matchers.eq(defaultDatafile), Matchers.eq(true), Matchers.eq(false))
    }

    // Utils
    fun mockProjectConfig(datafileHandler: DefaultDatafileHandler, datafile: String?) {
        var config: ProjectConfig? = null
        try {
            config = DatafileProjectConfig.Builder().withDatafile(datafile).build()
            Mockito.`when`(datafileHandler.config).thenReturn(config)
        } catch (e: ConfigParseException) {
            e.printStackTrace()
        }
    }
}