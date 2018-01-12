/****************************************************************************
 * Copyright 2017-2018, Optimizely, Inc. and contributors                   *
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

package com.optimizely.ab.android.sdk;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.android.event_handler.DefaultEventHandler;
import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.bucketing.DecisionService;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.notification.NotificationListener;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.optimizely.ab.android.sdk.OptimizelyManager.loadRawResource;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class OptimizelyClientTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][] {
                {
                        3,
                        loadRawResource(InstrumentationRegistry.getTargetContext(),R.raw.validprojectconfigv3)
                },
                {
                        4,
                        loadRawResource(InstrumentationRegistry.getTargetContext(),R.raw.validprojectconfigv4)
                }
        });
    }

    private Logger logger = mock(Logger.class);
    private Optimizely optimizely;
    private EventHandler eventHandler;
    private Bucketer bucketer = mock(Bucketer.class);
    private static final String FEATURE_ANDROID_EXPERIMENT_KEY = "android_experiment_key";
    private static final String FEATURE_MULTI_VARIATE_EXPERIMENT_KEY = "multivariate_experiment";
    private static final String FEATURE_MULTI_VARIATE_FEATURE_KEY = "multi_variate_feature";
    private static final String genericUserId = "userId";
    private String testProjectId = "7595190003";
    private int datafileVersion;

    public OptimizelyClientTest(int datafileVersion,String datafile){
        try {
            this.datafileVersion = datafileVersion;
            eventHandler = spy(DefaultEventHandler.getInstance(InstrumentationRegistry.getTargetContext()));
            optimizely = Optimizely.builder(datafile, eventHandler).build();
            if(datafileVersion==3) {
                when(bucketer.bucket(optimizely.getProjectConfig().getExperiments().get(0), genericUserId)).thenReturn(optimizely.getProjectConfig().getExperiments().get(0).getVariations().get(0));
            } else {
                when(bucketer.bucket(optimizely.getProjectConfig().getExperimentKeyMapping().get(FEATURE_MULTI_VARIATE_EXPERIMENT_KEY), genericUserId)).thenReturn(optimizely.getProjectConfig().getExperimentKeyMapping().get(FEATURE_MULTI_VARIATE_EXPERIMENT_KEY).getVariations().get(1));
            }
            spyOnConfig();
        }catch (ConfigParseException configException){
            logger.error("Error in parsing config",configException);
        }
    }

    private boolean setProperty(String propertyName, Object o, Object property) {
        boolean done = true;
        Field configField = null;
        try {
            configField = o.getClass().getDeclaredField(propertyName);
            configField.setAccessible(true);
            configField.set(o, property);
        }
        catch (NoSuchFieldException e) {
            e.printStackTrace();
            done = false;
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
            done = false;
        }
        return done;
    }

    private boolean setProjectConfig(Object o, ProjectConfig config) {
        return setProperty("projectConfig", o, config);
    }

    private boolean spyOnConfig() {
        ProjectConfig config = spy(optimizely.getProjectConfig());
        boolean done = true;

        try {
            Field decisionField = optimizely.getClass().getDeclaredField("decisionService");
            decisionField.setAccessible(true);
            DecisionService decisionService = (DecisionService)decisionField.get(optimizely);
            setProjectConfig(optimizely, config);
            setProjectConfig(decisionService, config);
            setProperty("bucketer", decisionService, bucketer);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            done = false;

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            done = false;
        }

        return done;
    }

    @Test
    public void testGoodActivation() {
        if (datafileVersion == 4) {
            OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
            Variation v = optimizelyClient.activate(FEATURE_MULTI_VARIATE_EXPERIMENT_KEY, genericUserId, Collections.singletonMap("house", "Gryffindor"));
            assertNotNull(v);
        } else if (datafileVersion == 3) {
            OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
            Variation v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
            assertNotNull(v);
        }

    }

    @Test
    public void testGoodForcedActivation() {

        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY,genericUserId,"var_1");
        // bucket will always return var_1
        Variation v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
        assertEquals(v.getKey(), "var_1");
        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertTrue(didSetForced);
        v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
        assertNotNull(v);
        assertEquals(v.getKey(), "var_2");
        v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));
    }

    @Test
    public void testGoodForceAActivationAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY,genericUserId,"var_1");

        final HashMap<String, String> attributes = new HashMap<>();
        // bucket will always return var_1
        Variation v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, attributes);
        assertEquals(v.getKey(), "var_1");

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertTrue(didSetForced);
        v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, attributes);
        assertNotNull(v);
        assertEquals(v.getKey(), "var_2");
        v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));
    }

    @Test
    public void testGoodActivationAttribute() {
        if (datafileVersion == 4) {
            OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
            final HashMap<String, String> attributes = new HashMap<>();
            attributes.put("house", "Gryffindor");
            Variation v = optimizelyClient.activate(FEATURE_MULTI_VARIATE_EXPERIMENT_KEY, genericUserId, attributes);
            assertNotNull(v);
        } else if (datafileVersion == 3) {
            OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
            final HashMap<String, String> attributes = new HashMap<>();
            Variation v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, attributes);
            assertNotNull(v);
        }
    }

    @Test
    public void testBadForcedActivationAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertFalse(didSetForced);
        Variation v = optimizelyClient.activate(genericUserId, genericUserId, new HashMap<String, String>());
        verify(logger).warn("Optimizely is not initialized, could not activate experiment {} " +
                "for user {} with attributes", genericUserId, genericUserId);
        assertNull(v);
        v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
        assertNull(v);

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));

    }


    @Test
    public void testBadActivationAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.activate(genericUserId, genericUserId, new HashMap<String, String>());
        verify(logger).warn("Optimizely is not initialized, could not activate experiment {} " +
                "for user {} with attributes", genericUserId, genericUserId);
    }

    @Test
    public void testGoodForcedTrack() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizely.getProjectConfig();

        optimizelyClient.track("test_event", genericUserId);

        verifyZeroInteractions(logger);

        ArgumentCaptor<LogEvent> logEventArgumentCaptor = ArgumentCaptor.forClass(LogEvent.class);
        try {
            verify(eventHandler).dispatchEvent(logEventArgumentCaptor.capture());
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogEvent logEvent = logEventArgumentCaptor.getValue();

        // id of var_2
        assertTrue(logEvent.getBody().contains("\"variationId\":\"8505434669\""));

        verify(config).getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));

    }

    @Test
    public void testGoodTrack() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        optimizelyClient.track("test_event", genericUserId);
        verifyZeroInteractions(logger);
    }

    @Test
    public void testBadTrack() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.track("test_event", genericUserId);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}", "test_event", genericUserId);
    }

    @Test
    public void testBadForcedTrack() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertFalse(didSetForced);
        optimizelyClient.track("test_event", genericUserId);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}",
                "test_event",
                genericUserId);

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
        assertNull(v);

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));
    }

    @Test
    public void testGoodForcedTrackAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizely.getProjectConfig();

        optimizelyClient.track("test_event", genericUserId, attributes);

        verifyZeroInteractions(logger);

        ArgumentCaptor<LogEvent> logEventArgumentCaptor = ArgumentCaptor.forClass(LogEvent.class);

        try {
            verify(eventHandler).dispatchEvent(logEventArgumentCaptor.capture());
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogEvent logEvent = logEventArgumentCaptor.getValue();

        // id of var_2
        assertTrue(logEvent.getBody().contains("\"variationId\":\"8505434669\""));

        verify(config).getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));

    }

    @Test
    public void testGoodTrackAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizely.getProjectConfig();

        optimizelyClient.track("test_event", genericUserId, attributes);

        verifyZeroInteractions(logger);

        verify(config).getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));
    }

    @Test
    public void testBadTrackAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.track("event1", genericUserId, attributes);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with attributes", "event1", genericUserId);
    }

    @Test
    public void testBadForcedTrackAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertFalse(didSetForced);
        optimizelyClient.track("event1", genericUserId, attributes);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with attributes", "event1", genericUserId);

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
        assertNull(v);

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));

    }

    @Test
    public void testGoodForcedTrackEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizely.getProjectConfig();

        optimizelyClient.track("test_event", genericUserId, 1L);

        verifyZeroInteractions(logger);

        ArgumentCaptor<LogEvent> logEventArgumentCaptor = ArgumentCaptor.forClass(LogEvent.class);

        try {
            verify(eventHandler).dispatchEvent(logEventArgumentCaptor.capture());
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogEvent logEvent = logEventArgumentCaptor.getValue();

        // id of var_2
        assertTrue(logEvent.getBody().contains("\"variationId\":\"8505434669\""));

        verify(config).getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));
    }

    @Test
    public void testGoodTrackEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        optimizelyClient.track("test_event", genericUserId, 1L);
        verifyZeroInteractions(logger);
    }

    @Test
    public void testBadTrackEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.track("event1", genericUserId, 1L);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with value {}", "event1", genericUserId, 1L);
    }

    @Test
    public void testBadForcedTrackEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertFalse(didSetForced);
        optimizelyClient.track("event1", genericUserId, 1L);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with value {}", "event1", genericUserId, 1L);

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
        assertNull(v);

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));
    }

    @Test
    public void testGoodTrackAttributeEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.track("test_event", genericUserId, attributes, 1L);
        verifyZeroInteractions(logger);
    }

    @Test
    public void testGoodForcedTrackAttributeEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizelyClient.getProjectConfig();

        optimizelyClient.track("test_event", genericUserId, attributes, 1L);

        verifyZeroInteractions(logger);

        ArgumentCaptor<LogEvent> logEventArgumentCaptor = ArgumentCaptor.forClass(LogEvent.class);

        try {
            verify(eventHandler).dispatchEvent(logEventArgumentCaptor.capture());
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogEvent logEvent = logEventArgumentCaptor.getValue();

        // id of var_2
        assertTrue(logEvent.getBody().contains("\"variationId\":\"8505434669\""));

        verify(config).getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));
    }

    @Test
    public void testBadTrackAttributeEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.track("event1", genericUserId, attributes, 1L);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with value {} and attributes", "event1", genericUserId, 1L);
    }

    @Test
    public void testBadForcedTrackAttributeEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertFalse(didSetForced);

        optimizelyClient.track("event1", genericUserId, attributes, 1L);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with value {} and attributes", "event1", genericUserId, 1L);

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
        assertNull(v);

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));
    }

    @Test
    public void testTrackWithEventTags() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        attributes.put("foo", "bar");
        final HashMap<String, Object> eventTags = new HashMap<>();
        eventTags.put("foo", 843);
        optimizelyClient.track("test_event", genericUserId, attributes, eventTags);
        verifyZeroInteractions(logger);
    }

    @Test
    public void testForcedTrackWithEventTags() {

        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);

        final HashMap<String, String> attributes = new HashMap<>();
        attributes.put("foo", "bar");
        final HashMap<String, Object> eventTags = new HashMap<>();
        eventTags.put("foo", 843);

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizely.getProjectConfig();

        optimizelyClient.track("test_event", genericUserId, attributes, eventTags);

        ArgumentCaptor<LogEvent> logEventArgumentCaptor = ArgumentCaptor.forClass(LogEvent.class);

        try {
            verify(eventHandler).dispatchEvent(logEventArgumentCaptor.capture());
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogEvent logEvent = logEventArgumentCaptor.getValue();

        // id of var_2
        assertTrue(logEvent.getBody().contains("\"variationId\":\"8505434669\""));

        verifyZeroInteractions(logger);

        verify(config).getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));
    }

    @Test
    public void testGoodGetVariation1() {
        if (datafileVersion == 4) {
            OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
            Variation v = optimizelyClient.getVariation(FEATURE_MULTI_VARIATE_EXPERIMENT_KEY, genericUserId, Collections.singletonMap("house", "Gryffindor"));
            assertNotNull(v);
        } else if (datafileVersion == 3) {
            OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
            Variation v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
            assertNotNull(v);
        }
    }

    @Test
    public void testGoodGetVariation1Forced() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertTrue(didSetForced);
        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);

        assertEquals(v.getKey(), "var_2");

        v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);

        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));

    }

    @Test
    public void testBadGetVariation1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        Variation v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);

        assertNull(v);

        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {}", FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);

    }

    @Test
    public void testBadGetVariation1Forced() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertFalse(didSetForced);
        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);

        assertNull(v);

        v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);

        assertNull(v);

        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {}", FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));
    }

    @Test
    public void testGoodGetVariationAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, attributes);
        verifyZeroInteractions(logger);
    }

    @Test
    public void testGoodForcedGetVariationAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertTrue(didSetForced);
        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);

        assertEquals(v.getKey(), "var_2");

        v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, attributes);

        verifyZeroInteractions(logger);

        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));

    }

    @Test
    public void testBadGetVariationAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, attributes);
        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {} with attributes", FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);
    }

    @Test
    public void testBadForcedGetVariationAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_2");

        assertFalse(didSetForced);
        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);

        assertNull(v);

        v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, attributes);

        assertNull(v);

        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {} with attributes", FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId);

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));
    }

    @Test
    public void testGoodGetProjectConfig() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        ProjectConfig config = optimizelyClient.getProjectConfig();
        assertNotNull(config);
    }

    @Test
    public void testGoodGetProjectConfigForced() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        ProjectConfig config = optimizelyClient.getProjectConfig();
        assertNotNull(config);
        assertTrue(config.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_1"));
        assertEquals(config.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId), config.getExperimentKeyMapping().get(FEATURE_ANDROID_EXPERIMENT_KEY).getVariations().get(0));
        assertTrue(config.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, null));
    }

    @Test
    public void testBadGetProjectConfig() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.getProjectConfig();
        verify(logger).warn("Optimizely is not initialized, could not get project config");
    }

    @Test
    public void testBadGetProjectConfigForced() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.getProjectConfig();
        verify(logger).warn("Optimizely is not initialized, could not get project config");
        assertFalse(optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId, "var_1"));
        verify(logger).warn("Optimizely is not initialized, could not set forced variation");
        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, genericUserId));
        verify(logger).warn("Optimizely is not initialized, could not get forced variation");
    }

    @Test
    public void testIsValid() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        assertTrue(optimizelyClient.isValid());
    }

    @Test
    public void testIsInvalid() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        assertFalse(optimizelyClient.isValid());
    }

    @Test
    public void testDefaultAttributes() {
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);
        when(appContext.getPackageName()).thenReturn("com.optly");

        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.setDefaultAttributes(OptimizelyDefaultAttributes.buildDefaultAttributesMap(context, logger));

        Map<String, String> map = optimizelyClient.getDefaultAttributes();
        Assert.assertEquals(map.size(), 4);
    }

    //======== Notification listeners ========//

    @Test
    public void testGoodAddNotificationListener() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        NotificationListener listener = new NotificationListener() {
            @Override
            public void onExperimentActivated(Experiment experiment,
                                              String s,
                                              Map<String, String> map,
                                              Variation variation) {
            }
        };
        optimizelyClient.addNotificationListener(listener);
        optimizelyClient.removeNotificationListener(listener);
        verifyZeroInteractions(logger);
    }

    @Test
    public void testBadAddNotificationListener() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        NotificationListener listener = new NotificationListener() {
            @Override
            public void onExperimentActivated(Experiment experiment,
                                              String s,
                                              Map<String, String> map,
                                              Variation variation) {
            }
        };
        optimizelyClient.addNotificationListener(listener);
        verify(logger).warn("Optimizely is not initialized, could not add notification listener");
    }

    @Test
    public void testBadRemoveNotificationListener() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        NotificationListener listener = new NotificationListener() {
            @Override
            public void onExperimentActivated(Experiment experiment,
                                              String s,
                                              Map<String, String> map,
                                              Variation variation) {
            }
        };
        optimizelyClient.removeNotificationListener(listener);
        verify(logger).warn("Optimizely is not initialized, could not remove notification listener");
    }

    @Test
    public void testGoodClearNotificationListeners() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        optimizelyClient.clearNotificationListeners();
        verifyZeroInteractions(logger);
    }

    @Test
    public void testBadClearNotificationListeners() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.clearNotificationListeners();
        verify(logger).warn("Optimizely is not initialized, could not clear notification listeners");
    }

    //Feature variation Testing

    //Test when optimizelyClient initialized with valid optimizely and without attributes
    @Test
    public void testGoodIsFeatureEnabledWithoutAttribute() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);

        //Scenario#1 without attributes: Assert false because user is not meeting audience condition
        assertFalse(optimizelyClient.isFeatureEnabled(FEATURE_MULTI_VARIATE_FEATURE_KEY, genericUserId));

    }

    @Test
    public void testGoodIsFeatureEnabledWithForcedVariations(){
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);

        Experiment activatedExperiment = optimizelyClient.getProjectConfig().getExperimentKeyMapping().get(FEATURE_MULTI_VARIATE_EXPERIMENT_KEY);
        Variation forcedVariation = activatedExperiment.getVariations().get(1);
        optimizelyClient.setForcedVariation(activatedExperiment.getKey(), genericUserId, forcedVariation.getKey() );

        assertTrue(optimizelyClient.isFeatureEnabled(FEATURE_MULTI_VARIATE_FEATURE_KEY, genericUserId));

        assertTrue(optimizelyClient.setForcedVariation(activatedExperiment.getKey(), genericUserId, null ));

        assertNull(optimizelyClient.getForcedVariation(activatedExperiment.getKey(), genericUserId));

        assertFalse(optimizelyClient.isFeatureEnabled(FEATURE_MULTI_VARIATE_FEATURE_KEY, genericUserId));

    }
    //Test when optimizelyClient initialized with valid optimizely and with attributes
    @Test
    public void testGoodIsFeatureEnabledWithAttribute() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);

        //Scenario#2 with valid attributes
        assertTrue(optimizelyClient.isFeatureEnabled(FEATURE_MULTI_VARIATE_FEATURE_KEY, genericUserId,
                Collections.singletonMap("house", "Gryffindor")));
        verifyZeroInteractions(logger);
        assertFalse(optimizelyClient.isFeatureEnabled("InvalidFeatureKey", genericUserId,
                Collections.singletonMap("house", "Gryffindor")));
    }

    //Test when optimizelyClient initialized with invalid optimizely;
    @Test
    public void testBadIsFeatureEnabledWithAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        //Scenario#1 without attributes
        assertFalse(optimizelyClient.isFeatureEnabled(FEATURE_MULTI_VARIATE_FEATURE_KEY,genericUserId));
        verify(logger).warn("Optimizely is not initialized, could not enable feature {} for user {}", FEATURE_MULTI_VARIATE_FEATURE_KEY, genericUserId);

    }
    @Test
    public void testBadIsFeatureEnabledWithoutAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        //Scenario#2 with attributes
        assertFalse(optimizelyClient.isFeatureEnabled(FEATURE_MULTI_VARIATE_FEATURE_KEY,genericUserId, Collections.singletonMap("house", "Gryffindor")));
        verify(logger).warn("Optimizely is not initialized, could not enable feature {} for user {} with attributes", FEATURE_MULTI_VARIATE_FEATURE_KEY, genericUserId);
    }

}