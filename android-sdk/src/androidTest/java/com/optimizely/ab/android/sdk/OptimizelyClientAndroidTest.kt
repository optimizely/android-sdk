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

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.optimizely.ab.Optimizely
import com.optimizely.ab.android.event_handler.DefaultEventHandler
import com.optimizely.ab.android.sdk.OptimizelyDefaultAttributes.buildDefaultAttributesMap
import com.optimizely.ab.android.sdk.OptimizelyManager.Companion.builder
import com.optimizely.ab.android.sdk.OptimizelyManager.Companion.loadRawResource
import com.optimizely.ab.bucketing.Bucketer
import com.optimizely.ab.bucketing.DecisionService
import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.ProjectConfig
import com.optimizely.ab.config.Variation
import com.optimizely.ab.config.parser.JsonParseException
import com.optimizely.ab.event.EventHandler
import com.optimizely.ab.event.LogEvent
import com.optimizely.ab.internal.ReservedEventKey
import com.optimizely.ab.notification.*
import com.optimizely.ab.optimizelydecision.DecisionResponse
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption
import com.optimizely.ab.optimizelyjson.OptimizelyJSON
import junit.framework.Assert
import org.hamcrest.Matchers
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.slf4j.Logger
import java.io.IOException
import java.lang.reflect.Field
import java.util.*
import javax.annotation.Nonnull

@RunWith(Parameterized::class)
class OptimizelyClientAndroidTest(private val datafileVersion: Int, private val datafile: String) {
    private val logger = Mockito.mock(Logger::class.java)
    private var optimizely: Optimizely? = null
    private var eventHandler: EventHandler? = null
    private val bucketer = Mockito.mock(Bucketer::class.java)
    private val testProjectId = "7595190003"
    private fun setProperty(propertyName: String, o: Any?, property: Any?): Boolean {
        var done = true
        var configField: Field? = null
        try {
            configField = o!!.javaClass.getDeclaredField(propertyName)
            configField.isAccessible = true
            configField[o] = property
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
            done = false
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            done = false
        }
        return done
    }

    private fun setProjectConfig(o: Any?, config: ProjectConfig?): Boolean {
        return setProperty("projectConfig", o, config)
    }

    private fun spyOnConfig(): Boolean {
        val config = Mockito.spy(optimizely!!.projectConfig)
        var done = true
        try {
            val decisionField = optimizely!!.javaClass.getDeclaredField("decisionService")
            decisionField.isAccessible = true
            val decisionService = decisionField[optimizely] as DecisionService
            setProjectConfig(optimizely, config)
            setProjectConfig(decisionService, config)
            setProperty("bucketer", decisionService, bucketer)
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            done = false
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
            done = false
        }
        return done
    }

    @Test
    fun testGoodActivation() {
        if (datafileVersion == 4) {
            val optimizelyClient = OptimizelyClient(optimizely,
                    logger)
            val v = optimizelyClient.activate(FEATURE_MULTI_VARIATE_EXPERIMENT_KEY, GENERIC_USER_ID, Collections.singletonMap("house", "Gryffindor"))
            Assert.assertNotNull(v)
        } else if (datafileVersion == 3) {
            val optimizelyClient = OptimizelyClient(optimizely,
                    logger)
            val v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
            Assert.assertNotNull(v)
        }
    }

    @Test
    fun testGoodActivationWithListener() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        val callbackCalled = BooleanArray(1)
        val callbackVariation = arrayOfNulls<Variation>(1)
        callbackCalled[0] = false
        val notificationId = optimizelyClient.notificationCenter!!.addNotificationListener(NotificationCenter.NotificationType.Activate, object : ActivateNotificationListener() {
            override fun onActivate(experiment: Experiment, userId: String, attributes: MutableMap<String, *>, variation: Variation, event: LogEvent) {
                callbackCalled[0] = true
                callbackVariation[0] = variation
            }
        })
        val v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        if (datafileVersion == 3) {
            Assert.assertEquals(v, callbackVariation[0])
            Assert.assertEquals(true, callbackCalled[0])
            Assert.assertEquals(1, notificationId)
            Assert.assertTrue(optimizelyClient.notificationCenter!!.removeNotificationListener(notificationId))
        } else {
            Assert.assertNull(v)
            Assert.assertEquals(false, callbackCalled[0])
            Assert.assertEquals(1, notificationId)
            Assert.assertTrue(optimizelyClient.notificationCenter!!.removeNotificationListener(notificationId))
        }
    }

    @Test
    fun testBadActivationWithListener() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        val callbackCalled = BooleanArray(1)
        callbackCalled[0] = false
        val notificationId = optimizelyClient.notificationCenter!!.addNotificationListener(NotificationCenter.NotificationType.Activate, object : TrackNotificationListener() {
            override fun onTrack(eventKey: String, userId: String, attributes: MutableMap<String, *>, eventTags: MutableMap<String, *>, event: LogEvent) {
                callbackCalled[0] = true
            }
        })
        val v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertEquals(false, callbackCalled[0])
        Assert.assertTrue(notificationId <= 0)
        Assert.assertFalse(optimizelyClient.notificationCenter!!.removeNotificationListener(notificationId))
    }

    @Test
    fun testGoodForcedActivation() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_1")
        // bucket will always return var_1
        var v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertEquals(v!!.key, "var_1")
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertTrue(didSetForced)
        v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertNotNull(v)
        Assert.assertEquals(v!!.key, "var_2")
        v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertEquals(v!!.key, "var_2")
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertTrue(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testGoodForceAActivationAttribute() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_1")
        val attributes = HashMap<String, String?>()
        // bucket will always return var_1
        var v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, attributes)
        Assert.assertEquals(v!!.key, "var_1")
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertTrue(didSetForced)
        v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, attributes)
        Assert.assertNotNull(v)
        Assert.assertEquals(v!!.key, "var_2")
        v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertEquals(v!!.key, "var_2")
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertTrue(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testGoodActivationAttribute() {
        if (datafileVersion == 4) {
            val optimizelyClient = OptimizelyClient(optimizely,
                    logger)
            val attributes = HashMap<String, String?>()
            attributes["house"] = "Gryffindor"
            val v = optimizelyClient.activate(FEATURE_MULTI_VARIATE_EXPERIMENT_KEY, GENERIC_USER_ID, attributes)
            Assert.assertNotNull(v)
        } else if (datafileVersion == 3) {
            val optimizelyClient = OptimizelyClient(optimizely,
                    logger)
            val attributes = HashMap<String, String?>()
            val v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, attributes)
            Assert.assertNotNull(v)
        }
    }

    private var expectedAttributes: Map<String, *>? = null
    @Test
    fun testGoodActivationWithTypedAttribute() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val attributeString = "house"
        val attributeBoolean = "booleanKey"
        val attributeInteger = "integerKey"
        val attributeDouble = "doubleKey"
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        val attributes = HashMap<String, Any?>()
        attributes[attributeString] = "Gryffindor"
        attributes[attributeBoolean] = true
        attributes[attributeInteger] = 3
        attributes[attributeDouble] = 3.123
        val notificationId = optimizelyClient.notificationCenter!!.addNotificationListener(NotificationCenter.NotificationType.Activate, object : ActivateNotificationListener() {
            override fun onActivate(experiment: Experiment, userId: String, attributes: MutableMap<String, *>, variation: Variation, event: LogEvent) {
                expectedAttributes = HashMap(attributes)
            }
        })
        val v = optimizelyClient.activate(FEATURE_MULTI_VARIATE_EXPERIMENT_KEY, GENERIC_USER_ID, attributes)
        org.junit.Assert.assertThat(expectedAttributes as Map<String, String?>?, Matchers.hasEntry(attributeString, "Gryffindor"))
        org.junit.Assert.assertThat(expectedAttributes as Map<String, Boolean?>?, Matchers.hasEntry(attributeBoolean, true))
        org.junit.Assert.assertThat(expectedAttributes as Map<String, Int?>?, Matchers.hasEntry(attributeInteger, 3))
        org.junit.Assert.assertThat(expectedAttributes as Map<String, Double?>?, Matchers.hasEntry(attributeDouble, 3.123))
        Assert.assertNotNull(v)
    }

    @Test
    fun testGoodActivationBucketingId() {
        val optimizelyClient = OptimizelyClient(optimizely, logger)
        val attributes = HashMap<String, String?>()
        val bucketingId = "1"
        val experiment = optimizelyClient.projectConfig!!.experimentKeyMapping[FEATURE_ANDROID_EXPERIMENT_KEY]
        attributes[BUCKETING_ATTRIBUTE] = bucketingId
        val v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, attributes)
        Mockito.verify(bucketer).bucket(experiment!!, bucketingId, optimizely!!.projectConfig!!)
    }

    @Test
    fun testBadForcedActivationAttribute() {
        val optimizelyClient = OptimizelyClient(null, logger)
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertFalse(didSetForced)
        var v = optimizelyClient.activate(GENERIC_USER_ID, GENERIC_USER_ID, HashMap<String, String?>())
        Mockito.verify(logger).warn("Optimizely is not initialized, could not activate experiment {} " +
                "for user {} with attributes", GENERIC_USER_ID, GENERIC_USER_ID)
        Assert.assertNull(v)
        v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertNull(v)
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertFalse(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testBadActivationAttribute() {
        val optimizelyClient = OptimizelyClient(null, logger)
        optimizelyClient.activate(GENERIC_USER_ID, GENERIC_USER_ID, HashMap<String, String?>())
        Mockito.verify(logger).warn("Optimizely is not initialized, could not activate experiment {} " +
                "for user {} with attributes", GENERIC_USER_ID, GENERIC_USER_ID)
    }

    @Test
    fun testGoodForcedTrack() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertTrue(didSetForced)
        val config = optimizely!!.projectConfig
        optimizelyClient.track("test_event", GENERIC_USER_ID)
        Mockito.verifyZeroInteractions(logger)
        val logEventArgumentCaptor = ArgumentCaptor.forClass(LogEvent::class.java)
        try {
            Mockito.verify(eventHandler)?.dispatchEvent(logEventArgumentCaptor.capture())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val logEvent = logEventArgumentCaptor.value

        // id of var_2
        // the new event backend accepts both camel case and snake case
        // https://logx.optimizely.com/v1/events
        Assert.assertTrue(logEvent.body.contains("\"enrich_decisions\":true"))
        val v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertEquals(v!!.key, "var_2")
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertTrue(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testGoodTrack() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        optimizelyClient.track("test_event", GENERIC_USER_ID)
        Mockito.verifyZeroInteractions(logger)
    }

    @Test
    fun testBadTrackWithListener() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        val numberOfCalls = BooleanArray(1)
        numberOfCalls[0] = false
        val notificationId = optimizelyClient.notificationCenter!!.addNotificationListener(NotificationCenter.NotificationType.Activate,
                object : TrackNotificationListener() {
                    override fun onTrack(eventKey: String, userId: String, attributes: MutableMap<String, *>, eventTags: MutableMap<String, *>, event: LogEvent) {
                        numberOfCalls[0] = true
                    }
                })
        optimizelyClient.track("test_event", GENERIC_USER_ID)
        Assert.assertTrue(notificationId <= 0)
        Assert.assertFalse(optimizelyClient.notificationCenter!!.removeNotificationListener(notificationId))
        Assert.assertEquals(false, numberOfCalls[0])
        Mockito.verifyZeroInteractions(logger)
    }

    @Test
    fun testGoodTrackWithListener() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        val numberOfCalls = BooleanArray(1)
        numberOfCalls[0] = false
        val notificationId = optimizelyClient.notificationCenter!!.addNotificationListener(NotificationCenter.NotificationType.Track,
                object : TrackNotificationListener() {
                    override fun onTrack(eventKey: String, userId: String, attributes: MutableMap<String, *>, eventTags: MutableMap<String, *>, event: LogEvent) {
                        numberOfCalls[0] = true
                    }
                })
        optimizelyClient.track("test_event", GENERIC_USER_ID)
        Assert.assertTrue(notificationId > 0)
        Assert.assertTrue(optimizelyClient.notificationCenter!!.removeNotificationListener(notificationId))
        if (datafileVersion == 3) {
            Assert.assertEquals(true, numberOfCalls[0])
        } else {
            Assert.assertEquals(true, numberOfCalls[0])
        }
        Mockito.verifyZeroInteractions(logger)
    }

    @Test
    fun testGoodTrackBucketing() {
        val optimizelyClient = OptimizelyClient(optimizely, logger)
        val attributes: MutableMap<String, String?> = HashMap()
        val bucketingId = "1"
        val experiment = optimizelyClient.projectConfig!!.getExperimentsForEventKey("test_event")[0]
        attributes[BUCKETING_ATTRIBUTE] = bucketingId
        optimizelyClient.track("test_event", "userId", attributes)
        Mockito.verifyZeroInteractions(logger)
    }

    @Test
    fun testBadTrack() {
        val optimizelyClient = OptimizelyClient(null, logger)
        optimizelyClient.track("test_event", GENERIC_USER_ID)
        Mockito.verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}", "test_event", GENERIC_USER_ID)
    }

    @Test
    fun testBadForcedTrack() {
        val optimizelyClient = OptimizelyClient(null, logger)
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertFalse(didSetForced)
        optimizelyClient.track("test_event", GENERIC_USER_ID)
        Mockito.verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}",
                "test_event",
                GENERIC_USER_ID)
        val v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertNull(v)
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertFalse(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testGoodForcedTrackAttribute() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        val attributes = HashMap<String, String?>()
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertTrue(didSetForced)
        val config = optimizely!!.projectConfig
        optimizelyClient.track("test_event", GENERIC_USER_ID, attributes)
        Mockito.verifyZeroInteractions(logger)
        val logEventArgumentCaptor = ArgumentCaptor.forClass(LogEvent::class.java)
        try {
            Mockito.verify(eventHandler)?.dispatchEvent(logEventArgumentCaptor.capture())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val logEvent = logEventArgumentCaptor.value

        // the new event backend accepts both camel case and snake case
        // https://logx.optimizely.com/v1/events
        // id of var_2
        Assert.assertTrue(logEvent.body.contains("\"enrich_decisions\":true") ||
                logEvent.body.contains("\"variation_id\":\"8505434669\""))
        val v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertEquals(v!!.key, "var_2")
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertTrue(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testGoodTrackAttribute() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        val attributes = HashMap<String, String?>()
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertTrue(didSetForced)
        val config = optimizely!!.projectConfig
        optimizelyClient.track("test_event", GENERIC_USER_ID, attributes)
        Mockito.verifyZeroInteractions(logger)
        val v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertEquals(v!!.key, "var_2")
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertTrue(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testBadTrackAttribute() {
        val optimizelyClient = OptimizelyClient(null, logger)
        val attributes = HashMap<String, String?>()
        optimizelyClient.track("event1", GENERIC_USER_ID, attributes)
        Mockito.verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with attributes", "event1", GENERIC_USER_ID)
    }

    @Test
    fun testBadForcedTrackAttribute() {
        val optimizelyClient = OptimizelyClient(null, logger)
        val attributes = HashMap<String, String?>()
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertFalse(didSetForced)
        optimizelyClient.track("event1", GENERIC_USER_ID, attributes)
        Mockito.verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with attributes", "event1", GENERIC_USER_ID)
        val v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertNull(v)
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertFalse(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testGoodForcedTrackEventVal() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertTrue(didSetForced)
        val config = optimizely!!.projectConfig
        optimizelyClient.track("test_event",
                GENERIC_USER_ID, emptyMap<String, String>(),
                Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L))
        Mockito.verifyZeroInteractions(logger)
        val logEventArgumentCaptor = ArgumentCaptor.forClass(LogEvent::class.java)
        try {
            Mockito.verify(eventHandler)?.dispatchEvent(logEventArgumentCaptor.capture())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val logEvent = logEventArgumentCaptor.value

        // the new event backend accepts both camel case and snake case
        // https://logx.optimizely.com/v1/events
        // id of var_2
        Assert.assertTrue(logEvent.body.contains("\"enrich_decisions\":true"))
        val v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertEquals(v!!.key, "var_2")
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertTrue(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testGoodTrackEventVal() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        optimizelyClient.track("test_event",
                GENERIC_USER_ID, emptyMap<String, String>(),
                Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L))
        Mockito.verifyZeroInteractions(logger)
    }

    @Test
    fun testBadTrackEventVal() {
        val optimizelyClient = OptimizelyClient(null, logger)
        optimizelyClient.track("event1",
                GENERIC_USER_ID, emptyMap<String, String>(),
                Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}" +
                " with attributes and event tags", "event1", GENERIC_USER_ID)
    }

    @Test
    fun testBadForcedTrackEventVal() {
        val optimizelyClient = OptimizelyClient(null, logger)
        val attributes = HashMap<String, String?>()
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertFalse(didSetForced)
        optimizelyClient.track("event1",
                GENERIC_USER_ID,
                attributes,
                Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}" +
                " with attributes and event tags", "event1", GENERIC_USER_ID)
        val v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertNull(v)
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertFalse(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testGoodTrackAttributeEventVal() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        val attributes = HashMap<String, String?>()
        optimizelyClient.track("test_event", GENERIC_USER_ID, attributes,
                Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L))
        Mockito.verifyZeroInteractions(logger)
    }

    @Test
    fun testGoodForcedTrackAttributeEventVal() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        val attributes = HashMap<String, String?>()
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertTrue(didSetForced)
        val config = optimizelyClient.projectConfig
        optimizelyClient.track("test_event",
                GENERIC_USER_ID,
                attributes,
                Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L))
        Mockito.verifyZeroInteractions(logger)
        val logEventArgumentCaptor = ArgumentCaptor.forClass(LogEvent::class.java)
        try {
            Mockito.verify(eventHandler)?.dispatchEvent(logEventArgumentCaptor.capture())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val logEvent = logEventArgumentCaptor.value

        // the new event backend accepts both camel case and snake case
        // https://logx.optimizely.com/v1/events
        // id of var_2
        Assert.assertTrue(logEvent.body.contains("\"enrich_decisions\":true"))
        val v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertEquals(v!!.key, "var_2")
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertTrue(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testBadTrackAttributeEventVal() {
        val optimizelyClient = OptimizelyClient(null, logger)
        val attributes = HashMap<String, String?>()
        optimizelyClient.track("event1", GENERIC_USER_ID, attributes,
                Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}" +
                " with attributes and event tags", "event1", GENERIC_USER_ID)
    }

    @Test
    fun testBadForcedTrackAttributeEventVal() {
        val optimizelyClient = OptimizelyClient(null, logger)
        val attributes = HashMap<String, String?>()
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertFalse(didSetForced)
        optimizelyClient.track("event1", GENERIC_USER_ID, attributes,
                Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}" +
                " with attributes and event tags", "event1", GENERIC_USER_ID)
        val v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertNull(v)
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertFalse(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testTrackWithEventTags() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        val attributes = HashMap<String, String?>()
        attributes["foo"] = "bar"
        val eventTags = HashMap<String?, Any?>()
        eventTags["foo"] = 843
        optimizelyClient.track("test_event", GENERIC_USER_ID, attributes, eventTags)
        Mockito.verifyZeroInteractions(logger)
    }

    @Test
    fun testForcedTrackWithEventTags() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        val attributes = HashMap<String, String?>()
        attributes["foo"] = "bar"
        val eventTags = HashMap<String?, Any?>()
        eventTags["foo"] = 843
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertTrue(didSetForced)
        val config = optimizely!!.projectConfig
        optimizelyClient.track("test_event", GENERIC_USER_ID, attributes, eventTags)
        val logEventArgumentCaptor = ArgumentCaptor.forClass(LogEvent::class.java)
        try {
            Mockito.verify(eventHandler)?.dispatchEvent(logEventArgumentCaptor.capture())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val logEvent = logEventArgumentCaptor.value

        // the new event backend accepts both camel case and snake case
        // https://logx.optimizely.com/v1/events
        // id of var_2
        Assert.assertTrue(logEvent.body.contains("\"enrich_decisions\":true"))
        Mockito.verifyZeroInteractions(logger)
        val v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertEquals(v!!.key, "var_2")
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertTrue(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testGoodGetVariation1() {
        if (datafileVersion == 4) {
            val optimizelyClient = OptimizelyClient(optimizely,
                    logger)
            val v = optimizelyClient.getVariation(FEATURE_MULTI_VARIATE_EXPERIMENT_KEY, GENERIC_USER_ID, Collections.singletonMap("house", "Gryffindor"))
            Assert.assertNotNull(v)
        } else if (datafileVersion == 3) {
            val optimizelyClient = OptimizelyClient(optimizely,
                    logger)
            val v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
            Assert.assertNotNull(v)
        }
    }

    @Test
    fun testGoodGetVariationBucketingId() {
        val optimizelyClient = OptimizelyClient(optimizely, logger)
        val experiment = optimizelyClient.projectConfig!!.experimentKeyMapping["android_experiment_key"]
        val bucketingId = "1"
        val attributes: MutableMap<String, String?> = HashMap()
        attributes[BUCKETING_ATTRIBUTE] = bucketingId
        val v = optimizelyClient.getVariation("android_experiment_key", "userId", attributes)
        Mockito.verify(bucketer).bucket(experiment!!, bucketingId, optimizely!!.projectConfig!!)
    }

    @Test
    fun testGoodGetVariation1Forced() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertTrue(didSetForced)
        var v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertEquals(v!!.key, "var_2")
        v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertEquals(v!!.key, "var_2")
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertTrue(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testBadGetVariation1() {
        val optimizelyClient = OptimizelyClient(null, logger)
        val v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertNull(v)
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {}", FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
    }

    @Test
    fun testBadGetVariation1Forced() {
        val optimizelyClient = OptimizelyClient(null, logger)
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertFalse(didSetForced)
        var v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertNull(v)
        v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertNull(v)
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {}", FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertFalse(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testGoodGetVariationAttribute() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        val attributes = HashMap<String, String?>()
        optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, attributes)
        Mockito.verifyZeroInteractions(logger)
    }

    @Test
    fun testGoodForcedGetVariationAttribute() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        val attributes = HashMap<String, String?>()
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertTrue(didSetForced)
        var v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertEquals(v!!.key, "var_2")
        v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, attributes)
        Mockito.verifyZeroInteractions(logger)
        Assert.assertEquals(v!!.key, "var_2")
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertTrue(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testBadGetVariationAttribute() {
        val optimizelyClient = OptimizelyClient(null, logger)
        val attributes = HashMap<String, String?>()
        optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, attributes)
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {} with attributes", FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
    }

    @Test
    fun testBadForcedGetVariationAttribute() {
        val optimizelyClient = OptimizelyClient(null, logger)
        val attributes = HashMap<String, String?>()
        var didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2")
        Assert.assertFalse(didSetForced)
        var v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        Assert.assertNull(v)
        v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, attributes)
        Assert.assertNull(v)
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {} with attributes", FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID)
        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null)
        Assert.assertFalse(didSetForced)
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
    }

    @Test
    fun testGoodGetProjectConfig() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        val config = optimizelyClient.projectConfig
        Assert.assertNotNull(config)
    }

    @Test
    fun testGoodGetProjectConfigForced() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        val config = optimizelyClient.projectConfig
        Assert.assertNotNull(config)
        Assert.assertTrue(optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_1"))
        Assert.assertEquals(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID), config!!.experimentKeyMapping[FEATURE_ANDROID_EXPERIMENT_KEY]!!.variations[0])
        Assert.assertTrue(optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null))
    }

    @Test
    fun testBadGetProjectConfig() {
        val optimizelyClient = OptimizelyClient(null, logger)
        optimizelyClient.projectConfig
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get project config")
    }

    @Test
    fun testBadGetProjectConfigForced() {
        val optimizelyClient = OptimizelyClient(null, logger)
        optimizelyClient.projectConfig
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get project config")
        Assert.assertFalse(optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_1"))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not set forced variation")
        Assert.assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get forced variation")
    }

    @Test
    fun testIsValid() {
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        Assert.assertTrue(optimizelyClient.isValid)
    }

    @Test
    fun testIsInvalid() {
        val optimizelyClient = OptimizelyClient(null, logger)
        Assert.assertFalse(optimizelyClient.isValid)
    }

    @Test
    fun testDefaultAttributes() {
        val context = Mockito.mock(Context::class.java)
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(context.applicationContext).thenReturn(appContext)
        Mockito.`when`(appContext.packageName).thenReturn("com.optly")
        val optimizelyClient = OptimizelyClient(null, logger)
        optimizelyClient.defaultAttributes = buildDefaultAttributesMap(context, logger)
        val map = optimizelyClient.defaultAttributes
        org.junit.Assert.assertEquals(map.size.toLong(), 4)
    }

    //Feature variation Testing
    //Test when optimizelyClient initialized with valid optimizely and without attributes
    @Test
    fun testGoodIsFeatureEnabledWithoutAttribute() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )

        //Scenario#1 without attributes: Assert false because user is not meeting audience condition
        Assert.assertFalse(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID))
    }

    @Test
    fun testGoodIsFeatureEnabledWithForcedVariations() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        val activatedExperiment = optimizelyClient.projectConfig!!.experimentKeyMapping[FEATURE_MULTI_VARIATE_EXPERIMENT_KEY]
        val forcedVariation = activatedExperiment!!.variations[1]
        optimizelyClient.setForcedVariation(
                activatedExperiment.key,
                GENERIC_USER_ID,
                forcedVariation.key
        )
        Assert.assertTrue(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID
        ))
        Assert.assertTrue(optimizelyClient.setForcedVariation(
                activatedExperiment.key,
                GENERIC_USER_ID,
                null
        ))
        Assert.assertNull(optimizelyClient.getForcedVariation(
                activatedExperiment.key,
                GENERIC_USER_ID
        ))
        Assert.assertFalse(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID
        ))
    }

    //Test when optimizelyClient initialized with valid optimizely and with attributes
    @Test
    fun testGoodIsFeatureEnabledWithAttribute() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )

        //Scenario#2 with valid attributes
        Assert.assertTrue(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ))
        Mockito.verifyZeroInteractions(logger)
        Assert.assertFalse(optimizelyClient.isFeatureEnabled(
                "InvalidFeatureKey",
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ))
    }

    //Test when optimizelyClient initialized with invalid optimizely;
    @Test
    fun testBadIsFeatureEnabledWithAttribute() {
        val optimizelyClient = OptimizelyClient(null, logger)

        //Scenario#1 without attributes
        Assert.assertFalse(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID
        ))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not enable feature {} for user {}",
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID
        )
    }

    @Test
    fun testBadIsFeatureEnabledWithoutAttribute() {
        val optimizelyClient = OptimizelyClient(null, logger)

        //Scenario#2 with attributes
        Assert.assertFalse(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not enable feature {} for user {} with attributes",
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID
        )
    }

    /**
     * Verify [Optimizely.getEnabledFeatures] calls into
     * [Optimizely.isFeatureEnabled] for each featureFlag
     * return List of FeatureFlags that are enabled
     */
    @Test
    fun testGetEnabledFeaturesWithValidUserID() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        val enabledFeatures = optimizelyClient.getEnabledFeatures(GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor"))
        Assert.assertFalse(enabledFeatures!!.isEmpty())
    }

    /**
     * Verify [Optimizely.getEnabledFeatures] calls into
     * [Optimizely.isFeatureEnabled] for each featureFlag
     * here user id is not valid because its not bucketed into any variation so it will
     * return empty List of enabledFeatures
     */
    @Test
    fun testGetEnabledFeaturesWithInValidUserIDandValidAttributes() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        val enabledFeatures = optimizelyClient.getEnabledFeatures("InvalidUserID",
                Collections.singletonMap("house", "Gryffindor"))
        Assert.assertTrue(enabledFeatures!!.isEmpty())
    }

    /**
     * Verify [Optimizely.getEnabledFeatures] calls into
     * [Optimizely.isFeatureEnabled] for each featureFlag
     * here Attributes are not valid because its not meeting any audience condition so
     * return empty List of enabledFeatures
     */
    @Test
    fun testGetEnabledFeaturesWithValidUserIDAndInvalidAttributes() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        val enabledFeatures = optimizelyClient.getEnabledFeatures(GENERIC_USER_ID,
                Collections.singletonMap("invalidKey", "invalidVal"))
        Assert.assertTrue(enabledFeatures!!.isEmpty())
    }

    /**
     * Verify [Optimizely.isFeatureEnabled]
     * returns True
     * when the user is bucketed into a variation for the feature.
     * The user is also bucketed into an experiment
     * and featureEnabled is also set to true
     */
    @Test
    fun testIsFeatureEnabledWithFeatureEnabledTrue() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )

        //with valid attributes
        Assert.assertTrue(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ))
        Mockito.verifyZeroInteractions(logger)
    }

    /**
     * Verify using forced variation to force the user into the fourth variation of experiment
     * FEATURE_MULTI_VARIATE_EXPERIMENT_KEY in which FeatureEnabled is set to
     * false so [Optimizely.isFeatureEnabled]  will return false
     */
    @Test
    fun testIsFeatureEnabledWithfeatureEnabledFalse() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        val activatedExperiment = optimizelyClient.projectConfig!!.experimentKeyMapping[FEATURE_MULTI_VARIATE_EXPERIMENT_KEY]
        val forcedVariation = activatedExperiment!!.variations[3]
        optimizelyClient.setForcedVariation(
                activatedExperiment.key,
                GENERIC_USER_ID,
                forcedVariation.key
        )
        Assert.assertFalse(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ))
    }

    /**
     * Verify using forced variation to force the user into the third variation of experiment
     * FEATURE_MULTI_VARIATE_EXPERIMENT_KEY in which FeatureEnabled is not set so by default it should return
     * false so [Optimizely.isFeatureEnabled]  will return false
     */
    @Test
    fun testIsFeatureEnabledWithfeatureEnabledNotSet() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        val activatedExperiment = optimizelyClient.projectConfig!!.experimentKeyMapping[FEATURE_MULTI_VARIATE_EXPERIMENT_KEY]
        val forcedVariation = activatedExperiment!!.variations[2]
        optimizelyClient.setForcedVariation(
                activatedExperiment.key,
                GENERIC_USER_ID,
                forcedVariation.key
        )
        Assert.assertFalse(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ))
    }

    //=======Feature Variables Testing===========
    /* FeatureVariableBoolean
     * Scenario#1 Without attributes in which user
     * was not bucketed into any variation for feature flag will return default value which is true in config
     */
    @Test
    fun testGoodGetFeatureVariableBooleanWithoutAttr() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )

        //Scenario#1 Without attributes
        Assert.assertTrue(optimizelyClient.getFeatureVariableBoolean(
                BOOLEAN_FEATURE_KEY,
                BOOLEAN_VARIABLE_KEY,
                GENERIC_USER_ID
        )!!)
    }

    //FeatureVariableBoolean Scenario#2 With attributes
    @Test
    fun testGoodGetFeatureVariableBooleanWithAttr() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        Assert.assertTrue(optimizelyClient.getFeatureVariableBoolean(
                BOOLEAN_FEATURE_KEY,
                BOOLEAN_VARIABLE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("key", "value")
        )!!)
        Mockito.verifyZeroInteractions(logger)
    }

    //FeatureVariableBoolean Scenario#3 if feature not found
    @Test
    fun testGoodGetFeatureVariableBooleanWithInvalidFeature() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        Assert.assertNull(optimizelyClient.getFeatureVariableBoolean(
                "invalidFeatureKey",
                "invalidVariableKey",
                GENERIC_USER_ID
        ))
    }

    //FeatureVariableBoolean Scenario#4 if variable not found
    @Test
    fun testGoodGetFeatureVariableBooleanWithInvalidVariable() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        Assert.assertNull(optimizelyClient.getFeatureVariableBoolean(
                BOOLEAN_FEATURE_KEY,
                "invalidVariableKey",
                GENERIC_USER_ID
        ))
    }

    @Test
    fun testBadGetFeatureVariableBoolean() {
        val optimizelyClient = OptimizelyClient(null, logger)

        //Scenario#1 without attributes
        Assert.assertNull(optimizelyClient.getFeatureVariableBoolean(
                BOOLEAN_FEATURE_KEY,
                BOOLEAN_VARIABLE_KEY,
                GENERIC_USER_ID
        ))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} boolean for user {}",
                BOOLEAN_FEATURE_KEY,
                BOOLEAN_VARIABLE_KEY,
                GENERIC_USER_ID
        )

        //Scenario#2 with attributes
        Assert.assertNull(optimizelyClient.getFeatureVariableBoolean(
                BOOLEAN_FEATURE_KEY,
                BOOLEAN_VARIABLE_KEY,
                GENERIC_USER_ID, emptyMap<String?, String>()))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} boolean for user {} with attributes",
                BOOLEAN_FEATURE_KEY,
                BOOLEAN_VARIABLE_KEY,
                GENERIC_USER_ID
        )
    }

    /* FeatureVariableDouble
     * Scenario#1 Without attributes in which user
     * was not bucketed into any variation for feature flag will return default value which is 14.99 in config
     */
    @Test
    fun testGoodGetFeatureVariableDoubleWithoutAttr() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val expectedDoubleDefaultFeatureVariable = 14.99
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        Assert.assertEquals(expectedDoubleDefaultFeatureVariable, optimizelyClient.getFeatureVariableDouble(
                DOUBLE_FEATURE_KEY,
                DOUBLE_VARIABLE_KEY,
                GENERIC_USER_ID
        ))
    }

    //FeatureVariableDouble Scenario#2 With attributes
    @Test
    fun testGoodGetFeatureVariableDoubleWithAttr() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val expectedDoubleFeatureVariable = 3.14
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        Assert.assertEquals(expectedDoubleFeatureVariable, optimizelyClient.getFeatureVariableDouble(
                DOUBLE_FEATURE_KEY,
                DOUBLE_VARIABLE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ))
        Mockito.verifyZeroInteractions(logger)
    }

    //FeatureVariableDouble Scenario#3 if feature not found
    @Test
    fun testGoodGetFeatureVariableDoubleInvalidFeatueKey() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        Assert.assertNull(optimizelyClient.getFeatureVariableDouble(
                "invalidFeatureKey",
                "invalidVariableKey",
                GENERIC_USER_ID
        ))
    }

    //FeatureVariableDouble Scenario#4 if variable not found
    @Test
    fun testGoodGetFeatureVariableDoubleInvalidVariableKey() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        Assert.assertNull(optimizelyClient.getFeatureVariableDouble(
                DOUBLE_FEATURE_KEY,
                "invalidVariableKey",
                GENERIC_USER_ID
        ))
    }

    @Test
    fun testBadGetFeatureVariableDouble() {
        val optimizelyClient = OptimizelyClient(
                null,
                logger
        )

        //Scenario#1 without attributes
        Assert.assertNull(optimizelyClient.getFeatureVariableDouble(
                DOUBLE_FEATURE_KEY,
                DOUBLE_VARIABLE_KEY,
                GENERIC_USER_ID
        ))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} double for user {}",
                DOUBLE_FEATURE_KEY,
                DOUBLE_VARIABLE_KEY,
                GENERIC_USER_ID
        )

        //Scenario#2 with attributes
        Assert.assertNull(optimizelyClient.getFeatureVariableDouble(
                DOUBLE_FEATURE_KEY,
                DOUBLE_VARIABLE_KEY,
                GENERIC_USER_ID, emptyMap<String?, String>()))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} double for user {} with attributes",
                DOUBLE_FEATURE_KEY,
                DOUBLE_VARIABLE_KEY,
                GENERIC_USER_ID
        )
    }

    /*
     * FeatureVariableInteger
     * Scenario#1 Without attributes in which user
     * was not bucketed into any variation for feature flag will return default value which is 7 in config
     */
    @Test
    fun testGoodGetFeatureVariableIntegerWithoutAttr() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val expectedDefaultIntegerFeatureVariable = 7
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        Assert.assertEquals(expectedDefaultIntegerFeatureVariable, optimizelyClient.getFeatureVariableInteger(
                INTEGER_FEATURE_KEY,
                INTEGER_VARIABLE_KEY,
                GENERIC_USER_ID
        ) as Int)
    }

    //FeatureVariableInteger Scenario#3 with Attributes
    @Test
    fun testGoodGetFeatureVariableIntegerWithAttr() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val expectedIntegerFeatureVariable = 2
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        Assert.assertEquals(expectedIntegerFeatureVariable, optimizelyClient.getFeatureVariableInteger(
                INTEGER_FEATURE_KEY,
                INTEGER_VARIABLE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ) as Int)
        Mockito.verifyZeroInteractions(logger)
    }

    //FeatureVariableInteger Scenario#3 if feature not found
    @Test
    fun testGoodGetFeatureVariableIntegerInvalidFeatureKey() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )

        //Scenario#3 if feature not found
        Assert.assertNull(optimizelyClient.getFeatureVariableInteger(
                "invalidFeatureKey",
                "invalidVariableKey",
                GENERIC_USER_ID
        ))
    }

    //FeatureVariableInteger Scenario#4 if variable not found
    @Test
    fun testGoodGetFeatureVariableIntegerInvalidVariableKey() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )

        //Scenario#4 if variable not found
        Assert.assertNull(optimizelyClient.getFeatureVariableInteger(
                INTEGER_FEATURE_KEY,
                "invalidVariableKey",
                GENERIC_USER_ID
        ))
    }

    @Test
    fun testBadGetFeatureVariableInteger() {
        val optimizelyClient = OptimizelyClient(null, logger)

        //Scenario#1 without attributes
        Assert.assertNull(optimizelyClient.getFeatureVariableInteger(
                INTEGER_FEATURE_KEY,
                INTEGER_VARIABLE_KEY,
                GENERIC_USER_ID
        ))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} integer for user {}",
                INTEGER_FEATURE_KEY,
                INTEGER_VARIABLE_KEY,
                GENERIC_USER_ID
        )

        //Scenario#2 with attributes
        Assert.assertNull(optimizelyClient.getFeatureVariableInteger(
                INTEGER_FEATURE_KEY,
                INTEGER_VARIABLE_KEY,
                GENERIC_USER_ID, emptyMap<String?, String>()))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} integer for user {} with attributes",
                INTEGER_FEATURE_KEY,
                INTEGER_VARIABLE_KEY,
                GENERIC_USER_ID
        )
    }

    /*
     * FeatureVariableString
     * Scenario#1 Without attributes in which user
     * was not bucketed into any variation for feature flag will return default value which is 'H' in config
     */
    @Test
    fun testGoodGetFeatureVariableStringWithoutAttr() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        val defaultValueOfStringVar = "H"
        Assert.assertEquals(defaultValueOfStringVar, optimizelyClient.getFeatureVariableString(
                STRING_FEATURE_KEY,
                STRING_VARIABLE_KEY,
                GENERIC_USER_ID
        ))
    }

    //FeatureVariableString Scenario#2 with attributes
    @Test
    fun testGoodGetFeatureVariableStringWithAttr() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger)
        Assert.assertEquals("F", optimizelyClient.getFeatureVariableString(
                STRING_FEATURE_KEY,
                STRING_VARIABLE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ))
        Mockito.verifyZeroInteractions(logger)
    }

    //FeatureVariableString Scenario#3 if feature not found
    @Test
    fun testGoodGetFeatureVariableStringInvalidFeatureKey() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger)
        Assert.assertNull(optimizelyClient.getFeatureVariableString(
                "invalidFeatureKey",
                "invalidVariableKey",
                GENERIC_USER_ID
        ))
    }

    //FeatureVariableString Scenario#4 if variable not found
    @Test
    fun testGoodGetFeatureVariableStringInvalidVariableKey() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )

        //Scenario#4 if variable not found
        Assert.assertNull(optimizelyClient.getFeatureVariableString(
                STRING_FEATURE_KEY,
                "invalidVariableKey",
                GENERIC_USER_ID
        ))
    }

    @Test
    fun testBadGetFeatureVariableString() {
        val optimizelyClient = OptimizelyClient(null, logger)

        //Scenario#1 without attributes
        Assert.assertNull(optimizelyClient.getFeatureVariableString(
                STRING_FEATURE_KEY,
                STRING_VARIABLE_KEY,
                GENERIC_USER_ID
        ))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} string for user {}",
                STRING_FEATURE_KEY,
                STRING_VARIABLE_KEY,
                GENERIC_USER_ID
        )

        //Scenario#2 with attributes
        Assert.assertNull(optimizelyClient.getFeatureVariableString(
                STRING_FEATURE_KEY,
                STRING_VARIABLE_KEY,
                GENERIC_USER_ID, emptyMap<String?, String>()))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} string for user {} with attributes",
                STRING_FEATURE_KEY,
                STRING_VARIABLE_KEY,
                GENERIC_USER_ID
        )
    }

    /*
     * FeatureVariableJSON
     * Scenario#1 Without attributes in which user
     * was not bucketed into any variation for feature flag will return default value which is
     * '{"k1":"v1","k2":3.5,"k3":true,"k4":{"kk1":"vv1","kk2":false}}' in config
     */
    @Test
    fun testGetFeatureVariableJSONWithoutAttr() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        val defaultValueOfStringVar = "{\"k1\":\"v1\",\"k2\":3.5,\"k3\":true,\"k4\":{\"kk1\":\"vv1\",\"kk2\":false}}"
        val json = optimizelyClient.getFeatureVariableJSON(
                STRING_FEATURE_KEY,
                JSON_VARIABLE_KEY,
                GENERIC_USER_ID
        )
        Assert.assertTrue(compareJsonStrings(json.toString(), defaultValueOfStringVar))
    }

    //FeatureVariableJSON Scenario#2 with attributes
    @Test
    fun testGetFeatureVariableJsonWithAttr() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger)
        val defaultValueOfStringVar = "{\"k1\":\"s1\",\"k2\":103.5,\"k3\":false,\"k4\":{\"kk1\":\"ss1\",\"kk2\":true}}"
        val json = optimizelyClient.getFeatureVariableJSON(
                STRING_FEATURE_KEY,
                JSON_VARIABLE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        )
        Assert.assertTrue(compareJsonStrings(json.toString(), defaultValueOfStringVar))
        Mockito.verifyZeroInteractions(logger)
    }

    //FeatureVariableJSON Scenario#3 if feature not found
    @Test
    fun testGetFeatureVariableJsonInvalidFeatureKey() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger)
        Assert.assertNull(optimizelyClient.getFeatureVariableJSON(
                "invalidFeatureKey",
                "invalidVariableKey",
                GENERIC_USER_ID
        ))
    }

    //FeatureVariableJSON Scenario#4 if variable not found
    @Test
    fun testGetFeatureVariableJsonInvalidVariableKey() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )

        //Scenario#4 if variable not found
        Assert.assertNull(optimizelyClient.getFeatureVariableJSON(
                STRING_FEATURE_KEY,
                "invalidVariableKey",
                GENERIC_USER_ID
        ))
    }

    @Test
    fun testBadGetFeatureVariableJson() {
        val optimizelyClient = OptimizelyClient(null, logger)

        //Scenario#1 without attributes
        Assert.assertNull(optimizelyClient.getFeatureVariableJSON(
                STRING_FEATURE_KEY,
                JSON_VARIABLE_KEY,
                GENERIC_USER_ID
        ))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} JSON for user {}.",
                STRING_FEATURE_KEY,
                JSON_VARIABLE_KEY,
                GENERIC_USER_ID
        )

        //Scenario#2 with attributes
        Assert.assertNull(optimizelyClient.getFeatureVariableJSON(
                STRING_FEATURE_KEY,
                JSON_VARIABLE_KEY,
                GENERIC_USER_ID,
                Collections.EMPTY_MAP as Map<String?, *>
        ))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} JSON for user {} with attributes.",
                STRING_FEATURE_KEY,
                JSON_VARIABLE_KEY,
                GENERIC_USER_ID
        )
    }

    /*
     * getAllFeatureVariables
     * Scenario#1 Without attributes in which user
     * was not bucketed into any variation for feature flag will return default value which is
     * '{"first_letter":"H","json_patched":{"k1":"v1","k2":3.5,"k3":true,"k4":{"kk1":"vv1","kk2":false}},"rest_of_name":"arry"}' in config
     */
    @Test
    fun testGetAllFeatureVariablesWithoutAttr() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        val defaultValueOfStringVar = "{\"first_letter\":\"H\",\"json_patched\":{\"k1\":\"v1\",\"k2\":3.5,\"k3\":true,\"k4\":{\"kk1\":\"vv1\",\"kk2\":false}},\"rest_of_name\":\"arry\"}"
        val json = optimizelyClient.getAllFeatureVariables(
                STRING_FEATURE_KEY,
                GENERIC_USER_ID
        )
        Assert.assertTrue(compareJsonStrings(json.toString(), defaultValueOfStringVar))
    }

    //GetAllFeatureVariables Scenario#2 with attributes
    @Test
    fun testGetAllFeatureVariablesWithAttr() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger)
        val defaultValueOfStringVar = "{\"first_letter\":\"F\",\"json_patched\":{\"k1\":\"s1\",\"k2\":103.5,\"k3\":false,\"k4\":{\"kk1\":\"ss1\",\"kk2\":true}},\"rest_of_name\":\"eorge\"}"
        val json = optimizelyClient.getAllFeatureVariables(
                STRING_FEATURE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        )
        Assert.assertTrue(compareJsonStrings(json.toString(), defaultValueOfStringVar))
        Mockito.verifyZeroInteractions(logger)
    }

    //GetAllFeatureVariables Scenario#3 if feature not found
    @Test
    fun testGetAllFeatureVariablesInvalidFeatureKey() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger)
        Assert.assertNull(optimizelyClient.getAllFeatureVariables(
                "invalidFeatureKey",
                GENERIC_USER_ID
        ))
    }

    @Test
    fun testBadGetAllFeatureVariables() {
        val optimizelyClient = OptimizelyClient(null, logger)

        //Scenario#1 without attributes
        Assert.assertNull(optimizelyClient.getAllFeatureVariables(
                STRING_FEATURE_KEY,
                GENERIC_USER_ID
        ))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get feature {} all feature variables for user {}.",
                STRING_FEATURE_KEY,
                GENERIC_USER_ID
        )

        //Scenario#2 with attributes
        Assert.assertNull(optimizelyClient.getAllFeatureVariables(
                STRING_FEATURE_KEY,
                GENERIC_USER_ID,
                Collections.EMPTY_MAP as Map<String?, *>
        ))
        Mockito.verify(logger).warn("Optimizely is not initialized, could not get feature {} all feature variables for user {} with attributes.",
                STRING_FEATURE_KEY,
                GENERIC_USER_ID
        )
    }

    // Accessibility testing of OptimizelyJSON.getValue
    @Test
    fun testGetValueOfOptimizelyJson() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val expectedMap: MutableMap<String, Any> = HashMap()
        expectedMap["kk1"] = "vv1"
        expectedMap["kk2"] = false
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        val optimizelyJSON = optimizelyClient.getAllFeatureVariables(
                STRING_FEATURE_KEY,
                GENERIC_USER_ID
        )
        try {
            Assert.assertEquals(optimizelyJSON!!.getValue("first_letter", String::class.java), "H")
            Assert.assertEquals(optimizelyJSON.getValue("json_patched.k4", Map::class.java), expectedMap)

            // When given jsonKey does not exist
            Assert.assertNull(optimizelyJSON.getValue("json_patched.k5", String::class.java))
        } catch (e: JsonParseException) {
            e.printStackTrace()
        }
    }

    @Test
    fun testGetOptimizelyConfig() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(optimizely,
                logger)
        val optimizelyConfig = optimizelyClient.optimizelyConfig
        Assert.assertNotNull(optimizelyConfig!!.experimentsMap)
        Assert.assertNotNull(optimizelyConfig.featuresMap)
        Assert.assertNotNull(optimizelyConfig.revision)
    }

    @Test
    fun testGetOptimizelyConfigReturnNullWhenConfigIsNull() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(null,
                logger)
        val optimizelyConfig = optimizelyClient.optimizelyConfig
        Assert.assertNull(optimizelyConfig)
    }

    @Test
    fun testAddDecisionNotificationHandler() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        val notificationId = optimizelyClient.addDecisionNotificationHandler { decisionNotification: DecisionNotification? -> }
        Assert.assertTrue(optimizelyClient.notificationCenter!!.removeNotificationListener(notificationId))
    }

    @Test
    fun testAddTrackNotificationHandler() {
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        val manager = optimizely!!.getNotificationCenter()
                .getNotificationManager<TrackNotification>(TrackNotification::class.java)
        val notificationId = optimizelyClient.addTrackNotificationHandler { trackNotification: TrackNotification? -> }
        Assert.assertTrue(manager!!.remove(notificationId))
    }

    @Test
    fun testAddingTrackNotificationHandlerWithInvalidOptimizely() {
        val optimizelyClient = OptimizelyClient(
                null,
                logger
        )
        val manager = optimizely!!.getNotificationCenter()
                .getNotificationManager<TrackNotification>(TrackNotification::class.java)
        val notificationId = optimizelyClient.addTrackNotificationHandler { trackNotification: TrackNotification? -> }
        Assert.assertEquals(-1, notificationId)
        Assert.assertFalse(manager!!.remove(notificationId))
    }

    @Test
    fun testAddingDecisionNotificationHandlerWithInvalidOptimizely() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                null,
                logger
        )
        val manager = optimizely!!.getNotificationCenter()
                .getNotificationManager<DecisionNotification>(DecisionNotification::class.java)
        val notificationId = optimizelyClient.addDecisionNotificationHandler { decisionNotification: DecisionNotification? -> }
        Assert.assertEquals(-1, notificationId)
        Assert.assertFalse(manager!!.remove(notificationId))
    }

    @Test
    fun testAddingDecisionNotificationHandlerTwice() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        val decisionNotificationHandler = NotificationHandler<DecisionNotification?> { }
        val notificationId = optimizelyClient.addDecisionNotificationHandler(decisionNotificationHandler)
        val notificationId2 = optimizelyClient.addDecisionNotificationHandler(decisionNotificationHandler)
        org.junit.Assert.assertNotEquals(-1, notificationId.toLong())
        Assert.assertEquals(-1, notificationId2)
        Assert.assertTrue(optimizelyClient.notificationCenter!!.removeNotificationListener(notificationId))
        Assert.assertFalse(optimizelyClient.notificationCenter!!.removeNotificationListener(notificationId2))
    }

    @Test
    fun testAddUpdateConfigNotificationHandler() {
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        val notificationId = optimizelyClient.addUpdateConfigNotificationHandler { notification: UpdateConfigNotification? -> }
        Assert.assertTrue(optimizely!!.getNotificationCenter()
                .getNotificationManager<Any>(UpdateConfigNotification::class.java)!!.remove(notificationId))
    }

    @Test
    fun testAddUpdateConfigNotificationHandlerWithInvalidOptimizely() {
        val optimizelyClient = OptimizelyClient(
                null,
                logger
        )
        val notificationId = optimizelyClient.addUpdateConfigNotificationHandler { notification: UpdateConfigNotification? -> }
        Assert.assertEquals(-1, notificationId)
        Assert.assertFalse(optimizely!!.getNotificationCenter()
                .getNotificationManager<Any>(UpdateConfigNotification::class.java)!!.remove(notificationId))
    }

    @Test
    fun testAddLogEventNotificationHandler() {
        val optimizelyClient = OptimizelyClient(
                optimizely,
                logger
        )
        val notificationId = optimizelyClient.addLogEventNotificationHandler { notification: LogEvent? -> }
        Assert.assertTrue(optimizely!!.getNotificationCenter()
                .getNotificationManager<Any>(LogEvent::class.java)!!.remove(notificationId))
    }

    @Test
    fun testAddLogEventNotificationHandlerWithInvalidOptimizely() {
        val optimizelyClient = OptimizelyClient(
                null,
                logger
        )
        val notificationId = optimizelyClient.addLogEventNotificationHandler { notification: LogEvent? -> }
        Assert.assertEquals(-1, notificationId)
        Assert.assertFalse(optimizely!!.getNotificationCenter()
                .getNotificationManager<Any>(LogEvent::class.java)!!.remove(notificationId))
    }

    // OptimizelyUserContext + Decide API
    @Test
    fun testCreateUserContext() {
        val optimizelyClient = OptimizelyClient(optimizely, logger)
        val userContext = optimizelyClient.createUserContext(GENERIC_USER_ID)
        Assert.assertEquals(userContext!!.userId, GENERIC_USER_ID)
        assert(userContext.attributes.isEmpty())
    }

    @Test
    fun testCreateUserContext_withAttributes() {
        val attributes = Collections.singletonMap<String?, Any?>("house", "Gryffindor")
        val optimizelyClient = OptimizelyClient(optimizely, logger)
        val userContext = optimizelyClient.createUserContext(GENERIC_USER_ID, attributes)
        Assert.assertEquals(userContext!!.userId, GENERIC_USER_ID)
        Assert.assertEquals(userContext.attributes, attributes)
    }

    @Test // this should be enough to validate connection to the core java-sdk
    fun testDecide() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val flagKey = INTEGER_FEATURE_KEY
        val attributes = Collections.singletonMap<String?, Any?>("house", "Gryffindor")
        val optimizelyClient = OptimizelyClient(optimizely, logger)
        val userContext = optimizelyClient.createUserContext(GENERIC_USER_ID, attributes)
        val decision = userContext!!.decide(flagKey)
        val variablesExpected = OptimizelyJSON(Collections.singletonMap<String, Any>("integer_variable", 2))
        Assert.assertEquals(decision.variationKey, "Feorge")
        Assert.assertTrue(decision.enabled)
        Assert.assertEquals(decision.variables.toMap(), variablesExpected.toMap())
        Assert.assertEquals(decision.ruleKey, FEATURE_MULTI_VARIATE_EXPERIMENT_KEY)
        Assert.assertEquals(decision.flagKey, flagKey)
        Assert.assertEquals(decision.userContext, userContext)
        Assert.assertTrue(decision.reasons.isEmpty())
    }

    @Test // this should be enough to validate connection to the core java-sdk
    @Throws(IOException::class)
    fun testDecide_withoutDefaultDecideOptions() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val datafile = loadRawResource(InstrumentationRegistry.getInstrumentation().targetContext, R.raw.validprojectconfigv4)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val optimizelyManager = builder(testProjectId).build(context)
        optimizelyManager!!.initialize(context, datafile)
        val optimizelyClient = optimizelyManager.optimizely
        val userContext = optimizelyClient.createUserContext(GENERIC_USER_ID)
        val decision = userContext!!.decide(INTEGER_FEATURE_KEY)
        Assert.assertTrue(decision.reasons.isEmpty())
    }

    @Test // this should be enough to validate connection to the core java-sdk
    @Throws(IOException::class)
    fun testDecide_withDefaultDecideOptions() {
        Assume.assumeTrue(datafileVersion == ProjectConfig.Version.V4.toString().toInt())
        val defaultDecideOptions = Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS)
        val datafile = loadRawResource(InstrumentationRegistry.getInstrumentation().targetContext, R.raw.validprojectconfigv4)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val optimizelyManager = builder(testProjectId)
                .withDefaultDecideOptions(defaultDecideOptions)
                .build(context)
        optimizelyManager!!.initialize(context, datafile)
        val optimizelyClient = optimizelyManager.optimizely
        val userContext = optimizelyClient.createUserContext(GENERIC_USER_ID)
        val decision = userContext!!.decide(INTEGER_FEATURE_KEY)
        Assert.assertTrue(decision.reasons.size > 0)
    }

    // Utils
    private fun compareJsonStrings(str1: String, str2: String): Boolean {
        val parser = JsonParser()
        val j1 = parser.parse(str1)
        val j2 = parser.parse(str2)
        return j1 == j2
    }

    private fun parseJsonString(str: String): Map<*, *> {
        return Gson().fromJson<Map<*, *>>(str, MutableMap::class.java)
    }

    companion object {
        var BUCKETING_ATTRIBUTE = "\$opt_bucketing_id"
        @JvmStatic
        @Parameterized.Parameters
        @Throws(IOException::class)
        fun data() = listOf(arrayOf(
                    3,
                    loadRawResource(InstrumentationRegistry.getInstrumentation().targetContext, R.raw.validprojectconfigv3)
            ), arrayOf(
                    4,
                    loadRawResource(InstrumentationRegistry.getInstrumentation().targetContext, R.raw.validprojectconfigv4)
            ))

        private const val FEATURE_ANDROID_EXPERIMENT_KEY = "android_experiment_key"
        private const val FEATURE_MULTI_VARIATE_EXPERIMENT_KEY = "multivariate_experiment"
        private const val FEATURE_MULTI_VARIATE_FEATURE_KEY = "multi_variate_feature"
        private const val BOOLEAN_FEATURE_KEY = "boolean_single_variable_feature"
        private const val BOOLEAN_VARIABLE_KEY = "boolean_variable"
        private const val DOUBLE_FEATURE_KEY = "double_single_variable_feature"
        private const val DOUBLE_VARIABLE_KEY = "double_variable"
        private const val INTEGER_FEATURE_KEY = "integer_single_variable_feature"
        private const val INTEGER_VARIABLE_KEY = "integer_variable"
        private const val STRING_FEATURE_KEY = "multi_variate_feature"
        private const val STRING_VARIABLE_KEY = "first_letter"
        private const val JSON_VARIABLE_KEY = "json_patched"
        private const val GENERIC_USER_ID = "userId"
    }

    init {
        try {
            eventHandler = Mockito.spy(DefaultEventHandler.getInstance(InstrumentationRegistry.getInstrumentation().targetContext))
            optimizely = Optimizely.builder(datafile, eventHandler!!).build()

            // set to return DecisionResponse with null variation by default (instead of null DecisionResponse)
            Mockito.`when`(bucketer.bucket(org.mockito.Matchers.anyObject(), org.mockito.Matchers.anyObject(), org.mockito.Matchers.anyObject())).thenReturn(DecisionResponse.nullNoReasons() as DecisionResponse<Variation>?)
            if (datafileVersion == 3) {
                val variation = optimizely!!.getProjectConfig()!!.experiments[0].variations[0]
                Mockito.`when`(bucketer.bucket(
                        optimizely!!.getProjectConfig()!!.experiments[0],
                        GENERIC_USER_ID,
                        optimizely!!.getProjectConfig()!!)
                ).thenReturn(DecisionResponse.responseNoReasons(variation) as DecisionResponse<Variation>?)
            } else {
                val variation = optimizely!!.getProjectConfig()!!.experimentKeyMapping[FEATURE_MULTI_VARIATE_EXPERIMENT_KEY]!!.variations[1]
                Mockito.`when`(bucketer.bucket(
                        optimizely!!.getProjectConfig()!!.experimentKeyMapping[FEATURE_MULTI_VARIATE_EXPERIMENT_KEY]!!,
                        GENERIC_USER_ID,
                        optimizely!!.getProjectConfig()!!)
                ).thenReturn(DecisionResponse.responseNoReasons(variation) as DecisionResponse<Variation>?)
            }
            spyOnConfig()
        } catch (configException: Exception) {
            logger.error("Error in parsing config", configException)
        }
    }
}