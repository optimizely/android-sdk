/****************************************************************************
 * Copyright 2016-2019, Optimizely, Inc. and contributors                   *
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

import com.optimizely.ab.Optimizely
import com.optimizely.ab.config.Experiment
import com.optimizely.ab.config.Variation
import com.optimizely.ab.event.LogEvent
import com.optimizely.ab.internal.ReservedEventKey
import com.optimizely.ab.notification.ActivateNotificationListener
import com.optimizely.ab.notification.NotificationCenter
import com.optimizely.ab.notification.TrackNotificationListener
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.exceptions.verification.junit.ArgumentsAreDifferent
import org.mockito.runners.MockitoJUnitRunner
import org.slf4j.Logger
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

/**
 * Tests for [OptimizelyClient]
 */
@RunWith(MockitoJUnitRunner::class)
class OptimizelyClientTest {
    @Mock
    var logger: Logger? = null

    @Mock
    var optimizely: Optimizely? = null

    @Mock
    var notificationCenter: NotificationCenter? = null
    @Before
    fun setup() {
        var field: Field? = null
        try {
            field = Optimizely::class.java.getDeclaredField("notificationCenter")
            // Mark the field as public so we can toy with it
            field.isAccessible = true
            // Get the Modifiers for the Fields
            val modifiersField = Field::class.java.getDeclaredField("modifiers")
            // Allow us to change the modifiers
            modifiersField.isAccessible = true
            // Remove final modifier from field by blanking out the bit that says "FINAL" in the Modifiers
            modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
            // Set new value
            field[optimizely] = notificationCenter
            Mockito.`when`(optimizely!!.isValid).thenReturn(true)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    @Test(expected = ArgumentsAreDifferent::class)
    fun testGoodActivation1() {
        val optimizelyClient = OptimizelyClient(optimizely, logger!!)
        optimizelyClient.activate("1", "1")
        Mockito.verify(optimizely)?.activate("1", "1")
    }

    @Test
    fun testBadActivation1() {
        val optimizelyClient = OptimizelyClient(null, logger!!)
        optimizelyClient.activate("1", "1")
        Mockito.verify(logger)?.warn("Optimizely is not initialized, could not activate experiment {} " +
                "for user {}", "1", "1")
    }

    @Test
    fun testGoodActivation2() {
        val optimizelyClient = OptimizelyClient(optimizely, logger!!)
        val attributes = HashMap<String, String?>()
        optimizelyClient.activate("1", "1", attributes)
        Mockito.verify(optimizely)?.activate("1", "1", attributes)
    }

    @Test
    fun testBadActivation2() {
        val optimizelyClient = OptimizelyClient(null, logger!!)
        optimizelyClient.activate("1", "1", HashMap<String, String?>())
        Mockito.verify(logger)?.warn("Optimizely is not initialized, could not activate experiment {} " +
                "for user {} with attributes", "1", "1")
    }

    @Test(expected = ArgumentsAreDifferent::class)
    fun testGoodTrack1() {
        val optimizelyClient = OptimizelyClient(optimizely, logger!!)
        optimizelyClient.track("event1", "1")
        Mockito.verify(optimizely)?.track("event1", "1")
    }

    @Test
    fun testBadTrack1() {
        val optimizelyClient = OptimizelyClient(null, logger!!)
        optimizelyClient.track("event1", "1")
        Mockito.verify(logger)?.warn("Optimizely is not initialized, could not track event {} for user {}", "event1", "1")
    }

    @Test
    fun testGoodTrack2() {
        val optimizelyClient = OptimizelyClient(optimizely, logger!!)
        val attributes = HashMap<String, String?>()
        optimizelyClient.track("event1", "1", attributes)
        Mockito.verify(optimizely)?.track("event1", "1", attributes)
    }

    @Test
    fun testBadTrack2() {
        val optimizelyClient = OptimizelyClient(null, logger!!)
        val attributes = HashMap<String, String?>()
        optimizelyClient.track("event1", "1", attributes)
        Mockito.verify(logger)?.warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with attributes", "event1", "1")
    }

    @Test
    fun testGoodTrack3() {
        val optimizelyClient = OptimizelyClient(optimizely, logger!!)
        val attributes = HashMap<String, String?>()
        optimizelyClient.track("event1", "1", attributes, Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L))
        val defaultAttributes: Map<String, String?> = HashMap()
        Mockito.verify(optimizely)?.track("event1", "1", defaultAttributes, Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L))
    }

    @Test
    fun testBadTrack4() {
        val optimizelyClient = OptimizelyClient(null, logger!!)
        val attributes = HashMap<String, String?>()
        optimizelyClient.track("event1", "1", attributes, Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L))
        Mockito.verify(logger)?.warn("Optimizely is not initialized, could not track event {} for user {}" +
                " with attributes and event tags", "event1", "1")
    }

    @Test
    fun testTrackWithEventTags() {
        val optimizelyClient = OptimizelyClient(optimizely, logger!!)
        val attributes = HashMap<String, String?>()
        attributes["foo"] = "bar"
        val eventTags = HashMap<String?, Any?>()
        eventTags["foo"] = 843
        optimizelyClient.track("event1", "1", attributes, eventTags)
        Mockito.verify(optimizely)?.track("event1", "1", attributes, eventTags)
    }

    @Test(expected = ArgumentsAreDifferent::class)
    fun testGoodGetVariation1() {
        val optimizelyClient = OptimizelyClient(optimizely, logger!!)
        optimizelyClient.getVariation("1", "1")
        Mockito.verify(optimizely)?.getVariation("1", "1")
    }

    @Test
    fun testBadGetVariation1() {
        val optimizelyClient = OptimizelyClient(null, logger!!)
        optimizelyClient.getVariation("1", "1")
        Mockito.verify(logger)?.warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {}", "1", "1")
    }

    @Test
    fun testGoodGetVariation3() {
        val optimizelyClient = OptimizelyClient(optimizely, logger!!)
        val attributes = HashMap<String, String?>()
        optimizelyClient.getVariation("1", "1", attributes)
        Mockito.verify(optimizely)?.getVariation("1", "1", attributes)
    }

    @Test
    fun testBadGetVariation3() {
        val optimizelyClient = OptimizelyClient(null, logger!!)
        val attributes = HashMap<String, String?>()
        optimizelyClient.getVariation("1", "1", attributes)
        Mockito.verify(logger)?.warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {} with attributes", "1", "1")
    }

    @Test
    fun testGoodGetProjectConfig() {
        val optimizelyClient = OptimizelyClient(optimizely, logger!!)
        optimizelyClient.projectConfig
        Mockito.verify(optimizely)?.projectConfig
    }

    @Test
    fun testBadGetProjectConfig() {
        val optimizelyClient = OptimizelyClient(null, logger!!)
        optimizelyClient.projectConfig
        Mockito.verify(logger)?.warn("Optimizely is not initialized, could not get project config")
    }

    @Test
    fun testIsValid() {
        val optimizelyClient = OptimizelyClient(optimizely, logger!!)
        assertTrue(optimizelyClient.isValid)
    }

    @Test
    fun testIsInvalid() {
        val optimizelyClient = OptimizelyClient(null, logger!!)
        assertFalse(optimizelyClient.isValid)
    }

    //======== Notification listeners ========//
    @Test
    fun testNewGoodAddNotificationCenterListener() {
        val optimizelyClient = OptimizelyClient(optimizely, logger!!)
        val listener: ActivateNotificationListener = object : ActivateNotificationListener() {
            override fun onActivate(experiment: Experiment, userId: String, attributes: MutableMap<String, *>, variation: Variation, event: LogEvent) {
                TODO("Not yet implemented")
            }
        }
        val notificationId = optimizelyClient.notificationCenter!!.addNotificationListener(NotificationCenter.NotificationType.Activate, listener)
        Mockito.verify(optimizely!!.notificationCenter).addNotificationListener(NotificationCenter.NotificationType.Activate, listener)
    }

    @Test
    fun testBadAddNotificationCenterListener() {
        val optimizelyClient = OptimizelyClient(optimizely, logger!!)
        val listener: ActivateNotificationListener = object : ActivateNotificationListener() {
            override fun onActivate(experiment: Experiment, userId: String, attributes: MutableMap<String, *>, variation: Variation, event: LogEvent) {
                TODO("Not yet implemented")
            }
        }
        optimizelyClient.notificationCenter!!.addNotificationListener(NotificationCenter.NotificationType.Activate, listener)
        Mockito.verify(optimizely!!.notificationCenter).addNotificationListener(NotificationCenter.NotificationType.Activate, listener)
    }

    @Test
    fun testGoodRemoveNotificationCenterListener() {
        val optimizelyClient = OptimizelyClient(optimizely, logger!!)
        val listener: TrackNotificationListener = object : TrackNotificationListener() {
            override fun onTrack(eventKey: String, userId: String, attributes: MutableMap<String, *>, eventTags: MutableMap<String, *>, event: LogEvent) {
                TODO("Not yet implemented")
            }
        }
        val note = optimizelyClient.notificationCenter!!.addNotificationListener(NotificationCenter.NotificationType.Track, listener)
        optimizelyClient.notificationCenter!!.removeNotificationListener(note)
        Mockito.verify(optimizely!!.notificationCenter).removeNotificationListener(note)
    }

    @Test
    fun testBadRemoveNotificationCenterListener() {
        val optimizelyClient = OptimizelyClient(null, logger!!)
        val notificationCenter = if (optimizelyClient.notificationCenter != null) optimizelyClient.notificationCenter else NotificationCenter()
        notificationCenter!!.removeNotificationListener(1)
        Mockito.verify(logger)?.warn("Optimizely is not initialized, could not get the notification listener")
    }

    @Test
    fun testGoodClearNotificationCenterListeners() {
        val optimizelyClient = OptimizelyClient(optimizely, logger!!)
        optimizelyClient.notificationCenter!!.clearAllNotificationListeners()
        Mockito.verify(optimizely!!.notificationCenter).clearAllNotificationListeners()
    }

    @Test
    fun testBadClearNotificationCenterListeners() {
        val optimizelyClient = OptimizelyClient(null, logger!!)
        val notificationCenter = if (optimizelyClient.notificationCenter != null) optimizelyClient.notificationCenter else NotificationCenter()
        notificationCenter!!.clearAllNotificationListeners()
        Mockito.verify(logger)?.warn("Optimizely is not initialized, could not get the notification listener")
    }
}