/****************************************************************************
 * Copyright 2017-2021, 2023 Optimizely, Inc. and contributors              *
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
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.optimizely.ab.Optimizely;
import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.android.event_handler.DefaultEventHandler;
import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.bucketing.DecisionService;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.config.parser.JsonParseException;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.internal.ReservedEventKey;
import com.optimizely.ab.notification.ActivateNotificationListener;
import com.optimizely.ab.notification.DecisionNotification;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.notification.NotificationHandler;
import com.optimizely.ab.notification.NotificationManager;
import com.optimizely.ab.notification.TrackNotification;
import com.optimizely.ab.notification.TrackNotificationListener;
import com.optimizely.ab.notification.UpdateConfigNotification;
import com.optimizely.ab.optimizelyconfig.OptimizelyConfig;
import com.optimizely.ab.optimizelydecision.DecisionResponse;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;
import com.optimizely.ab.optimizelydecision.OptimizelyDecision;
import com.optimizely.ab.optimizelyjson.OptimizelyJSON;

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
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import static com.optimizely.ab.android.sdk.OptimizelyManager.loadRawResource;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class OptimizelyClientTest {
    static String BUCKETING_ATTRIBUTE = "$opt_bucketing_id";

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][] {
                {
                        3,
                        loadRawResource(InstrumentationRegistry.getInstrumentation().getTargetContext(),R.raw.validprojectconfigv3)
                },
                {
                        4,
                        loadRawResource(InstrumentationRegistry.getInstrumentation().getTargetContext(),R.raw.validprojectconfigv4)
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
    private final static String BOOLEAN_FEATURE_KEY = "boolean_single_variable_feature";
    private final static String BOOLEAN_VARIABLE_KEY = "boolean_variable";
    private final static String DOUBLE_FEATURE_KEY = "double_single_variable_feature";
    private final static String DOUBLE_VARIABLE_KEY = "double_variable";
    private final static String INTEGER_FEATURE_KEY = "integer_single_variable_feature";
    private final static String INTEGER_VARIABLE_KEY = "integer_variable";
    private final static String STRING_FEATURE_KEY = "multi_variate_feature";
    private final static String STRING_VARIABLE_KEY = "first_letter";
    private final static String JSON_VARIABLE_KEY = "json_patched";
    private static final String GENERIC_USER_ID = "userId";
    private String testProjectId = "7595190003";
    private int datafileVersion;


    public OptimizelyClientTest(int datafileVersion,String datafile){
        try {
            this.datafileVersion = datafileVersion;
            eventHandler = spy(DefaultEventHandler.getInstance(InstrumentationRegistry.getInstrumentation().getTargetContext()));
            optimizely = Optimizely.builder(datafile, eventHandler).build();

            // set to return DecisionResponse with null variation by default (instead of null DecisionResponse)
            when(bucketer.bucket(any(), any(), any())).thenReturn(DecisionResponse.nullNoReasons());

            if(datafileVersion==3) {
                Variation variation = optimizely.getProjectConfig().getExperiments().get(0).getVariations().get(0);
                when(bucketer.bucket(
                        optimizely.getProjectConfig().getExperiments().get(0),
                        GENERIC_USER_ID,
                        optimizely.getProjectConfig())
                ).thenReturn(DecisionResponse.responseNoReasons(variation));
            } else {
                Variation variation = optimizely.getProjectConfig().getExperimentKeyMapping().get(FEATURE_MULTI_VARIATE_EXPERIMENT_KEY).getVariations().get(1);
                when(bucketer.bucket(
                        optimizely.getProjectConfig().getExperimentKeyMapping().get(FEATURE_MULTI_VARIATE_EXPERIMENT_KEY),
                        GENERIC_USER_ID,
                        optimizely.getProjectConfig())
                ).thenReturn(DecisionResponse.responseNoReasons(variation));
            }
            spyOnConfig();
        } catch (Exception configException) {
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
            OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
            Variation v = optimizelyClient.activate(FEATURE_MULTI_VARIATE_EXPERIMENT_KEY, GENERIC_USER_ID, Collections.singletonMap("house", "Gryffindor"));
            assertNotNull(v);
        } else if (datafileVersion == 3) {
            OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
            Variation v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
            assertNotNull(v);
        }

    }

    @Test
    public void testGoodActivationWithListener() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        final boolean[] callbackCalled = new boolean[1];
        final Variation[] callbackVariation = new Variation[1];

        callbackCalled[0] = false;
        int notificationId = optimizelyClient.getNotificationCenter().addNotificationListener(NotificationCenter.NotificationType.Activate, new ActivateNotificationListener() {
            @Override
            public void onActivate(@Nonnull Experiment experiment, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Variation variation, @Nonnull LogEvent event) {
              callbackCalled[0] = true;
              callbackVariation[0] = variation;
            }
        });

        Variation v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);

        if (datafileVersion == 3) {
            assertEquals(v, callbackVariation[0]);
            assertEquals(true, callbackCalled[0]);
            assertEquals(1, notificationId);
            assertTrue(optimizelyClient.getNotificationCenter().removeNotificationListener(notificationId));
        }
        else {
            assertNull(v);
            assertEquals(false, callbackCalled[0]);
            assertEquals(1, notificationId);
            assertTrue(optimizelyClient.getNotificationCenter().removeNotificationListener(notificationId));
        }

    }

    @Test
    public void testBadActivationWithListener() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        final boolean[] callbackCalled = new boolean[1];

        callbackCalled[0] = false;
        int notificationId = optimizelyClient.getNotificationCenter().addNotificationListener(NotificationCenter.NotificationType.Activate, new TrackNotificationListener() {
                    @Override
                    public void onTrack(@Nonnull String eventKey, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Map<String, ?> eventTags, @Nonnull LogEvent event) {
                        callbackCalled[0] = true;
                    }
                });

        Variation v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);

        assertEquals(false, callbackCalled[0]);
        assertTrue(notificationId <= 0);
        assertFalse(optimizelyClient.getNotificationCenter().removeNotificationListener(notificationId));

    }

    @Test
    public void testGoodForcedActivation() {

        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY,GENERIC_USER_ID,"var_1");
        // bucket will always return var_1
        Variation v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
        assertEquals(v.getKey(), "var_1");
        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertTrue(didSetForced);
        v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
        assertNotNull(v);
        assertEquals(v.getKey(), "var_2");
        v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));
    }

    @Test
    public void testGoodForceAActivationAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY,GENERIC_USER_ID,"var_1");

        final HashMap<String, String> attributes = new HashMap<>();
        // bucket will always return var_1
        Variation v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, attributes);
        assertEquals(v.getKey(), "var_1");

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertTrue(didSetForced);
        v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, attributes);
        assertNotNull(v);
        assertEquals(v.getKey(), "var_2");
        v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));
    }

    @Test
    public void testGoodActivationAttribute() {
        if (datafileVersion == 4) {
            OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
            final HashMap<String, String> attributes = new HashMap<>();
            attributes.put("house", "Gryffindor");
            Variation v = optimizelyClient.activate(FEATURE_MULTI_VARIATE_EXPERIMENT_KEY, GENERIC_USER_ID, attributes);
            assertNotNull(v);
        } else if (datafileVersion == 3) {
            OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
            final HashMap<String, String> attributes = new HashMap<>();
            Variation v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, attributes);
            assertNotNull(v);
        }
    }

    private Map<String, ?> expectedAttributes;

    @Test
    public void testGoodActivationWithTypedAttribute() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        String attributeString = "house";
        String attributeBoolean = "booleanKey";
        String attributeInteger = "integerKey";
        String attributeDouble = "doubleKey";

        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                    logger);
        final HashMap<String, Object> attributes = new HashMap<>();
        attributes.put(attributeString, "Gryffindor");
        attributes.put(attributeBoolean, true);
        attributes.put(attributeInteger, 3);
        attributes.put(attributeDouble, 3.123);


        int notificationId = optimizelyClient.getNotificationCenter().addNotificationListener(NotificationCenter.NotificationType.Activate, new ActivateNotificationListener() {
            @Override
            public void onActivate(@Nonnull Experiment experiment, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Variation variation, @Nonnull LogEvent event) {
                expectedAttributes = new HashMap<>(attributes);
            }
        });

        Variation v = optimizelyClient.activate(FEATURE_MULTI_VARIATE_EXPERIMENT_KEY, GENERIC_USER_ID, attributes);

        assertThat((Map<String,? extends String>)expectedAttributes, hasEntry(attributeString, "Gryffindor"));
        assertThat((Map<String,? extends Boolean>)expectedAttributes, hasEntry(attributeBoolean, true));
        assertThat((Map<String,? extends Integer>)expectedAttributes, hasEntry(attributeInteger, 3));
        assertThat((Map<String,? extends Double>)expectedAttributes, hasEntry(attributeDouble, 3.123));
        assertNotNull(v);

    }

    @Test
    public void testGoodActivationBucketingId() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        String bucketingId = "1";
        Experiment experiment = optimizelyClient.getProjectConfig().getExperimentKeyMapping().get(FEATURE_ANDROID_EXPERIMENT_KEY);
        attributes.put(BUCKETING_ATTRIBUTE, bucketingId);
        Variation v = optimizelyClient.activate(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, attributes);
        verify(bucketer).bucket(experiment, bucketingId, optimizely.getProjectConfig());
    }

    @Test
        public void testBadForcedActivationAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertFalse(didSetForced);
        Variation v = optimizelyClient.activate(GENERIC_USER_ID, GENERIC_USER_ID, new HashMap<String, String>());
        verify(logger).warn("Optimizely is not initialized, could not activate experiment {} " +
                "for user {} with attributes", GENERIC_USER_ID, GENERIC_USER_ID);
        assertNull(v);
        v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
        assertNull(v);

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));

    }


    @Test
    public void testBadActivationAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.activate(GENERIC_USER_ID, GENERIC_USER_ID, new HashMap<String, String>());
        verify(logger).warn("Optimizely is not initialized, could not activate experiment {} " +
                "for user {} with attributes", GENERIC_USER_ID, GENERIC_USER_ID);
    }

    @Test
    public void testGoodForcedTrack() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizely.getProjectConfig();

        optimizelyClient.track("test_event", GENERIC_USER_ID);

        verifyNoInteractions(logger);

        ArgumentCaptor<LogEvent> logEventArgumentCaptor = ArgumentCaptor.forClass(LogEvent.class);
        try {
            verify(eventHandler).dispatchEvent(logEventArgumentCaptor.capture());
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogEvent logEvent = logEventArgumentCaptor.getValue();

        // id of var_2
        // the new event backend accepts both camel case and snake case
        // https://logx.optimizely.com/v1/events
        assertTrue(logEvent.getBody().contains("\"enrich_decisions\":true"));

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));

    }

    @Test
    public void testGoodTrack() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        optimizelyClient.track("test_event", GENERIC_USER_ID);
        verifyNoInteractions(logger);
    }

    @Test
    public void testBadTrackWithListener() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);

        final boolean[] numberOfCalls = new boolean[1];
        numberOfCalls[0]= false;

        int notificationId = optimizelyClient.getNotificationCenter().addNotificationListener(NotificationCenter.NotificationType.Activate,
                new TrackNotificationListener() {
                    @Override
                    public void onTrack(@Nonnull String eventKey, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Map<String, ?> eventTags, @Nonnull LogEvent event) {
                        numberOfCalls[0] = true;
                    }
                });
        optimizelyClient.track("test_event", GENERIC_USER_ID);
        assertTrue(notificationId <= 0);
        assertFalse(optimizelyClient.getNotificationCenter().removeNotificationListener(notificationId));
        assertEquals(false, numberOfCalls[0]);
        verifyNoInteractions(logger);

    }

    @Test
    public void testGoodTrackWithListener() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);

        final boolean[] numberOfCalls = new boolean[1];
        numberOfCalls[0]= false;

        int notificationId = optimizelyClient.getNotificationCenter().addNotificationListener(NotificationCenter.NotificationType.Track,
                new TrackNotificationListener() {
                    @Override
                    public void onTrack(@Nonnull String eventKey, @Nonnull String userId, @Nonnull Map<String, ?> attributes, @Nonnull Map<String, ?> eventTags, @Nonnull LogEvent event) {
                        numberOfCalls[0] = true;
                    }
                });
        optimizelyClient.track("test_event", GENERIC_USER_ID);
        assertTrue(notificationId > 0);
        assertTrue(optimizelyClient.getNotificationCenter().removeNotificationListener(notificationId));
        if (datafileVersion == 3) {
            assertEquals(true, numberOfCalls[0]);
        }
        else {
            assertEquals(true, numberOfCalls[0]);
        }
        verifyNoInteractions(logger);

    }

    @Test
    public void testGoodTrackBucketing() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        Map<String,String> attributes = new HashMap<>();
        String bucketingId = "1";
        Experiment experiment = optimizelyClient.getProjectConfig().getExperimentsForEventKey("test_event").get(0);
        attributes.put(BUCKETING_ATTRIBUTE, bucketingId);
        optimizelyClient.track("test_event", "userId", attributes);
        verifyNoInteractions(logger);
    }

    @Test
    public void testBadTrack() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.track("test_event", GENERIC_USER_ID);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}", "test_event", GENERIC_USER_ID);
    }

    @Test
    public void testBadForcedTrack() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertFalse(didSetForced);
        optimizelyClient.track("test_event", GENERIC_USER_ID);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}",
                "test_event",
                GENERIC_USER_ID);

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
        assertNull(v);

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));
    }

    @Test
    public void testGoodForcedTrackAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        final HashMap<String, String> attributes = new HashMap<>();

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizely.getProjectConfig();

        optimizelyClient.track("test_event", GENERIC_USER_ID, attributes);

        verifyNoInteractions(logger);

        ArgumentCaptor<LogEvent> logEventArgumentCaptor = ArgumentCaptor.forClass(LogEvent.class);

        try {
            verify(eventHandler).dispatchEvent(logEventArgumentCaptor.capture());
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogEvent logEvent = logEventArgumentCaptor.getValue();

        // the new event backend accepts both camel case and snake case
        // https://logx.optimizely.com/v1/events
        // id of var_2
        assertTrue(logEvent.getBody().contains("\"enrich_decisions\":true") ||
                logEvent.getBody().contains("\"variation_id\":\"8505434669\""));

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));

    }

    @Test
    public void testGoodTrackAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        final HashMap<String, String> attributes = new HashMap<>();

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizely.getProjectConfig();

        optimizelyClient.track("test_event", GENERIC_USER_ID, attributes);

        verifyNoInteractions(logger);

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));
    }

    @Test
    public void testBadTrackAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.track("event1", GENERIC_USER_ID, attributes);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with attributes", "event1", GENERIC_USER_ID);
    }

    @Test
    public void testBadForcedTrackAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertFalse(didSetForced);
        optimizelyClient.track("event1", GENERIC_USER_ID, attributes);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with attributes", "event1", GENERIC_USER_ID);

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
        assertNull(v);

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));

    }

    @Test
    public void testGoodForcedTrackEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizely.getProjectConfig();

        optimizelyClient.track("test_event",
                GENERIC_USER_ID,
                Collections.<String, String>emptyMap(),
                Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L));

        verifyNoInteractions(logger);

        ArgumentCaptor<LogEvent> logEventArgumentCaptor = ArgumentCaptor.forClass(LogEvent.class);

        try {
            verify(eventHandler).dispatchEvent(logEventArgumentCaptor.capture());
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogEvent logEvent = logEventArgumentCaptor.getValue();

        // the new event backend accepts both camel case and snake case
        // https://logx.optimizely.com/v1/events
        // id of var_2
        assertTrue(logEvent.getBody().contains("\"enrich_decisions\":true"));

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));
    }

    @Test
    public void testGoodTrackEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        optimizelyClient.track("test_event",
                GENERIC_USER_ID,
                Collections.<String, String>emptyMap(),
                Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L));
        verifyNoInteractions(logger);
    }

    @Test
    public void testBadTrackEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.track("event1",
                GENERIC_USER_ID,
                Collections.<String, String>emptyMap(),
                Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L));
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}" +
                " with attributes and event tags", "event1", GENERIC_USER_ID);
    }

    @Test
    public void testBadForcedTrackEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertFalse(didSetForced);
        optimizelyClient.track("event1",
                GENERIC_USER_ID,
                attributes,
                Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L));
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}" +
                " with attributes and event tags", "event1", GENERIC_USER_ID);

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
        assertNull(v);

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));
    }

    @Test
    public void testGoodTrackAttributeEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.track("test_event", GENERIC_USER_ID, attributes,
                Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L));
        verifyNoInteractions(logger);
    }

    @Test
    public void testGoodForcedTrackAttributeEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        final HashMap<String, String> attributes = new HashMap<>();

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizelyClient.getProjectConfig();

        optimizelyClient.track("test_event",
                GENERIC_USER_ID,
                attributes,
                Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L));

        verifyNoInteractions(logger);

        ArgumentCaptor<LogEvent> logEventArgumentCaptor = ArgumentCaptor.forClass(LogEvent.class);

        try {
            verify(eventHandler).dispatchEvent(logEventArgumentCaptor.capture());
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogEvent logEvent = logEventArgumentCaptor.getValue();

        // the new event backend accepts both camel case and snake case
        // https://logx.optimizely.com/v1/events
        // id of var_2
        assertTrue(logEvent.getBody().contains("\"enrich_decisions\":true"));

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));
    }

    @Test
    public void testBadTrackAttributeEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.track("event1", GENERIC_USER_ID, attributes,
                Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L));
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}" +
                " with attributes and event tags", "event1", GENERIC_USER_ID);
    }

    @Test
    public void testBadForcedTrackAttributeEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertFalse(didSetForced);

        optimizelyClient.track("event1", GENERIC_USER_ID, attributes,
                Collections.singletonMap(ReservedEventKey.REVENUE.toString(), 1L));
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}" +
                " with attributes and event tags", "event1", GENERIC_USER_ID);

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
        assertNull(v);

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));
    }

    @Test
    public void testTrackWithEventTags() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        final HashMap<String, String> attributes = new HashMap<>();
        attributes.put("foo", "bar");
        final HashMap<String, Object> eventTags = new HashMap<>();
        eventTags.put("foo", 843);
        optimizelyClient.track("test_event", GENERIC_USER_ID, attributes, eventTags);
        verifyNoInteractions(logger);
    }

    @Test
    public void testForcedTrackWithEventTags() {

        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);

        final HashMap<String, String> attributes = new HashMap<>();
        attributes.put("foo", "bar");
        final HashMap<String, Object> eventTags = new HashMap<>();
        eventTags.put("foo", 843);

        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizely.getProjectConfig();

        optimizelyClient.track("test_event", GENERIC_USER_ID, attributes, eventTags);

        ArgumentCaptor<LogEvent> logEventArgumentCaptor = ArgumentCaptor.forClass(LogEvent.class);

        try {
            verify(eventHandler).dispatchEvent(logEventArgumentCaptor.capture());
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogEvent logEvent = logEventArgumentCaptor.getValue();

        // the new event backend accepts both camel case and snake case
        // https://logx.optimizely.com/v1/events
        // id of var_2
        assertTrue(logEvent.getBody().contains("\"enrich_decisions\":true"));

        verifyNoInteractions(logger);

        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));
    }

    @Test
    public void testGoodGetVariation1() {
        if (datafileVersion == 4) {
            OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
            Variation v = optimizelyClient.getVariation(FEATURE_MULTI_VARIATE_EXPERIMENT_KEY, GENERIC_USER_ID, Collections.singletonMap("house", "Gryffindor"));
            assertNotNull(v);
        } else if (datafileVersion == 3) {
            OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
            Variation v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
            assertNotNull(v);
        }
    }

    @Test
    public void testGoodGetVariationBucketingId() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        Experiment experiment = optimizelyClient.getProjectConfig().getExperimentKeyMapping().get("android_experiment_key");
        String bucketingId = "1";
        Map<String, String> attributes = new HashMap<>();
        attributes.put(BUCKETING_ATTRIBUTE, bucketingId);
        Variation v = optimizelyClient.getVariation("android_experiment_key", "userId", attributes);
        verify(bucketer).bucket(experiment, bucketingId, optimizely.getProjectConfig());
    }

    @Test
    public void testGoodGetVariation1Forced() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertTrue(didSetForced);
        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);

        assertEquals(v.getKey(), "var_2");

        v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);

        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));

    }

    @Test
    public void testBadGetVariation1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        Variation v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);

        assertNull(v);

        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {}", FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);

    }

    @Test
    public void testBadGetVariation1Forced() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertFalse(didSetForced);
        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);

        assertNull(v);

        v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);

        assertNull(v);

        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {}", FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));
    }

    @Test
    public void testGoodGetVariationAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, attributes);
        verifyNoInteractions(logger);
    }

    @Test
    public void testGoodForcedGetVariationAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        final HashMap<String, String> attributes = new HashMap<>();
        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertTrue(didSetForced);
        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);

        assertEquals(v.getKey(), "var_2");

        v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, attributes);

        verifyNoInteractions(logger);

        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));

    }

    @Test
    public void testBadGetVariationAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, attributes);
        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {} with attributes", FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);
    }

    @Test
    public void testBadForcedGetVariationAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        boolean didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_2");

        assertFalse(didSetForced);
        Variation v = optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);

        assertNull(v);

        v = optimizelyClient.getVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, attributes);

        assertNull(v);

        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {} with attributes", FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID);

        didSetForced = optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));
    }

    @Test
    public void testGoodGetProjectConfig() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        ProjectConfig config = optimizelyClient.getProjectConfig();
        assertNotNull(config);
    }

    @Test
    public void testGoodGetProjectConfigForced() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        ProjectConfig config = optimizelyClient.getProjectConfig();
        assertNotNull(config);
        assertTrue(optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_1"));
        assertEquals(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID), config.getExperimentKeyMapping().get(FEATURE_ANDROID_EXPERIMENT_KEY).getVariations().get(0));
        assertTrue(optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, null));
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
        assertFalse(optimizelyClient.setForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID, "var_1"));
        verify(logger).warn("Optimizely is not initialized, could not set forced variation");
        assertNull(optimizelyClient.getForcedVariation(FEATURE_ANDROID_EXPERIMENT_KEY, GENERIC_USER_ID));
        verify(logger).warn("Optimizely is not initialized, could not get forced variation");
    }

    @Test
    public void testIsValid() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
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

        Map<String, ?> map = optimizelyClient.getDefaultAttributes();
        Assert.assertEquals(map.size(), 4);
    }

    //Feature variation Testing

    //Test when optimizelyClient initialized with valid optimizely and without attributes
    @Test
    public void testGoodIsFeatureEnabledWithoutAttribute() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        //Scenario#1 without attributes: Assert false because user is not meeting audience condition
        assertFalse(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID));

    }

    @Test
    public void testGoodIsFeatureEnabledWithForcedVariations(){
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        Experiment activatedExperiment = optimizelyClient.getProjectConfig().getExperimentKeyMapping().get(
                FEATURE_MULTI_VARIATE_EXPERIMENT_KEY);
        Variation forcedVariation = activatedExperiment.getVariations().get(1);
        optimizelyClient.setForcedVariation(
                activatedExperiment.getKey(),
                GENERIC_USER_ID,
                forcedVariation.getKey()
        );

        assertTrue(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID
        ));

        assertTrue(optimizelyClient.setForcedVariation(
                activatedExperiment.getKey(),
                GENERIC_USER_ID,
                null
        ));

        assertNull(optimizelyClient.getForcedVariation(
                activatedExperiment.getKey(),
                GENERIC_USER_ID
        ));

        assertFalse(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID
        ));
    }

    //Test when optimizelyClient initialized with valid optimizely and with attributes
    @Test
    public void testGoodIsFeatureEnabledWithAttribute() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        //Scenario#2 with valid attributes
        assertTrue(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ));

        verifyNoInteractions(logger);

        assertFalse(optimizelyClient.isFeatureEnabled(
                "InvalidFeatureKey",
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ));
    }

    //Test when optimizelyClient initialized with invalid optimizely;
    @Test
    public void testBadIsFeatureEnabledWithAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        //Scenario#1 without attributes
        assertFalse(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID
        ));

        verify(logger).warn("Optimizely is not initialized, could not enable feature {} for user {}",
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID
        );

    }

    @Test
    public void testBadIsFeatureEnabledWithoutAttribute() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        //Scenario#2 with attributes
        assertFalse(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ));

        verify(logger).warn("Optimizely is not initialized, could not enable feature {} for user {} with attributes",
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID
        );
    }

    /**
     * Verify {@link Optimizely#getEnabledFeatures(String, Map)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} for each featureFlag
     * return List of FeatureFlags that are enabled
     */
    @Test
    public void testGetEnabledFeaturesWithValidUserID(){
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        List<String> enabledFeatures = optimizelyClient.getEnabledFeatures(GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor"));
        assertFalse(enabledFeatures.isEmpty());
    }

    /**
     * Verify {@link Optimizely#getEnabledFeatures(String, Map)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} for each featureFlag
     * here user id is not valid because its not bucketed into any variation so it will
     * return empty List of enabledFeatures
     */
    @Test
    public void testGetEnabledFeaturesWithInValidUserIDandValidAttributes(){
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );
        List<String> enabledFeatures = optimizelyClient.getEnabledFeatures("InvalidUserID",
                Collections.singletonMap("house", "Gryffindor"));
        assertTrue(enabledFeatures.isEmpty());
    }


    /**
     * Verify {@link Optimizely#getEnabledFeatures(String, Map)} calls into
     * {@link Optimizely#isFeatureEnabled(String, String, Map)} for each featureFlag
     * here Attributes are not valid because its not meeting any audience condition so
     * return empty List of enabledFeatures
     */
    @Test
    public void testGetEnabledFeaturesWithValidUserIDAndInvalidAttributes() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );
        List<String> enabledFeatures = optimizelyClient.getEnabledFeatures(GENERIC_USER_ID,
                Collections.singletonMap("invalidKey", "invalidVal"));
        assertTrue(enabledFeatures.isEmpty());
    }

    /**
     * Verify {@link Optimizely#isFeatureEnabled(String, String, Map)}
     * returns True
     * when the user is bucketed into a variation for the feature.
     * The user is also bucketed into an experiment
     * and featureEnabled is also set to true
     */
    @Test
    public void testIsFeatureEnabledWithFeatureEnabledTrue(){
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        //with valid attributes
        assertTrue(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ));

        verifyNoInteractions(logger);

    }

    /**
     * Verify using forced variation to force the user into the fourth variation of experiment
     * FEATURE_MULTI_VARIATE_EXPERIMENT_KEY in which FeatureEnabled is set to
     * false so {@link Optimizely#isFeatureEnabled(String, String, Map)}  will return false
     */
    @Test
    public void testIsFeatureEnabledWithfeatureEnabledFalse(){
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );
        Experiment activatedExperiment = optimizelyClient.getProjectConfig().getExperimentKeyMapping().get(
                FEATURE_MULTI_VARIATE_EXPERIMENT_KEY);
        Variation forcedVariation = activatedExperiment.getVariations().get(3);
        optimizelyClient.setForcedVariation(
                activatedExperiment.getKey(),
                GENERIC_USER_ID,
                forcedVariation.getKey()
        );
        assertFalse(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ));
    }

    /**
     * Verify using forced variation to force the user into the third variation of experiment
     * FEATURE_MULTI_VARIATE_EXPERIMENT_KEY in which FeatureEnabled is not set so by default it should return
     * false so {@link Optimizely#isFeatureEnabled(String, String, Map)}  will return false
     */
    @Test
    public void testIsFeatureEnabledWithfeatureEnabledNotSet() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );
        Experiment activatedExperiment = optimizelyClient.getProjectConfig().getExperimentKeyMapping().get(
                FEATURE_MULTI_VARIATE_EXPERIMENT_KEY);
        Variation forcedVariation = activatedExperiment.getVariations().get(2);
        optimizelyClient.setForcedVariation(
                activatedExperiment.getKey(),
                GENERIC_USER_ID,
                forcedVariation.getKey()
        );
        assertFalse(optimizelyClient.isFeatureEnabled(
                FEATURE_MULTI_VARIATE_FEATURE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ));
    }

    //=======Feature Variables Testing===========

    /* FeatureVariableBoolean
     * Scenario#1 Without attributes in which user
     * was not bucketed into any variation for feature flag will return default value which is true in config
     */
    @Test
    public void testGoodGetFeatureVariableBooleanWithoutAttr() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        //Scenario#1 Without attributes
        assertTrue(optimizelyClient.getFeatureVariableBoolean(
                BOOLEAN_FEATURE_KEY,
                BOOLEAN_VARIABLE_KEY,
                GENERIC_USER_ID
        ));
    }

    //FeatureVariableBoolean Scenario#2 With attributes
    @Test
    public void testGoodGetFeatureVariableBooleanWithAttr() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        assertTrue(optimizelyClient.getFeatureVariableBoolean(
                BOOLEAN_FEATURE_KEY,
                BOOLEAN_VARIABLE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("key", "value")
        ));
        verifyNoInteractions(logger);

    }

    //FeatureVariableBoolean Scenario#3 if feature not found
    @Test
    public void testGoodGetFeatureVariableBooleanWithInvalidFeature() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        assertNull(optimizelyClient.getFeatureVariableBoolean(
                "invalidFeatureKey",
                "invalidVariableKey",
                GENERIC_USER_ID
        ));

    }

    //FeatureVariableBoolean Scenario#4 if variable not found
    @Test
    public void testGoodGetFeatureVariableBooleanWithInvalidVariable() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        assertNull(optimizelyClient.getFeatureVariableBoolean(
                BOOLEAN_FEATURE_KEY,
                "invalidVariableKey",
                GENERIC_USER_ID
        ));
    }

    @Test
    public void testBadGetFeatureVariableBoolean() {

        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        //Scenario#1 without attributes
        assertNull(optimizelyClient.getFeatureVariableBoolean(
                BOOLEAN_FEATURE_KEY,
                BOOLEAN_VARIABLE_KEY,
                GENERIC_USER_ID
        ));

        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} boolean for user {}",
                BOOLEAN_FEATURE_KEY,
                BOOLEAN_VARIABLE_KEY,
                GENERIC_USER_ID
        );

        //Scenario#2 with attributes
        assertNull(optimizelyClient.getFeatureVariableBoolean(
                BOOLEAN_FEATURE_KEY,
                BOOLEAN_VARIABLE_KEY,
                GENERIC_USER_ID,
                Collections.<String, String>emptyMap()
        ));

        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} boolean for user {} with attributes",
                BOOLEAN_FEATURE_KEY,
                BOOLEAN_VARIABLE_KEY,
                GENERIC_USER_ID
        );
    }

    /* FeatureVariableDouble
     * Scenario#1 Without attributes in which user
     * was not bucketed into any variation for feature flag will return default value which is 14.99 in config
     */
    @Test
    public void testGoodGetFeatureVariableDoubleWithoutAttr() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        double expectedDoubleDefaultFeatureVariable = 14.99;
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        assertEquals(expectedDoubleDefaultFeatureVariable, optimizelyClient.getFeatureVariableDouble(
                DOUBLE_FEATURE_KEY,
                DOUBLE_VARIABLE_KEY,
                GENERIC_USER_ID
        ));
    }

    //FeatureVariableDouble Scenario#2 With attributes
    @Test
    public void testGoodGetFeatureVariableDoubleWithAttr() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        double expectedDoubleFeatureVariable = 3.14;
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        assertEquals(expectedDoubleFeatureVariable, optimizelyClient.getFeatureVariableDouble(
                DOUBLE_FEATURE_KEY,
                DOUBLE_VARIABLE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ));
        verifyNoInteractions(logger);
    }

    //FeatureVariableDouble Scenario#3 if feature not found
    @Test
    public void testGoodGetFeatureVariableDoubleInvalidFeatueKey() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        assertNull(optimizelyClient.getFeatureVariableDouble(
                "invalidFeatureKey",
                "invalidVariableKey",
                GENERIC_USER_ID
        ));
    }

    //FeatureVariableDouble Scenario#4 if variable not found
    @Test
    public void testGoodGetFeatureVariableDoubleInvalidVariableKey() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        assertNull(optimizelyClient.getFeatureVariableDouble(
                DOUBLE_FEATURE_KEY,
                "invalidVariableKey",
                GENERIC_USER_ID
        ));
    }

    @Test
    public void testBadGetFeatureVariableDouble() {

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                null,
                logger
        );

        //Scenario#1 without attributes
        assertNull(optimizelyClient.getFeatureVariableDouble(
                DOUBLE_FEATURE_KEY,
                DOUBLE_VARIABLE_KEY,
                GENERIC_USER_ID
        ));

        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} double for user {}",
                DOUBLE_FEATURE_KEY,
                DOUBLE_VARIABLE_KEY,
                GENERIC_USER_ID
        );

        //Scenario#2 with attributes
        assertNull(optimizelyClient.getFeatureVariableDouble(
                DOUBLE_FEATURE_KEY,
                DOUBLE_VARIABLE_KEY,
                GENERIC_USER_ID,
                Collections.<String, String>emptyMap()
        ));

        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} double for user {} with attributes",
                DOUBLE_FEATURE_KEY,
                DOUBLE_VARIABLE_KEY,
                GENERIC_USER_ID
        );
    }

    /*
     * FeatureVariableInteger
     * Scenario#1 Without attributes in which user
     * was not bucketed into any variation for feature flag will return default value which is 7 in config
     */
    @Test
    public void testGoodGetFeatureVariableIntegerWithoutAttr() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        int expectedDefaultIntegerFeatureVariable = 7;
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        assertEquals(expectedDefaultIntegerFeatureVariable, (int) optimizelyClient.getFeatureVariableInteger(
                INTEGER_FEATURE_KEY,
                INTEGER_VARIABLE_KEY,
                GENERIC_USER_ID
        ));
    }

    //FeatureVariableInteger Scenario#3 with Attributes
    @Test
    public void testGoodGetFeatureVariableIntegerWithAttr() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        int expectedIntegerFeatureVariable = 2;
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        assertEquals(expectedIntegerFeatureVariable, (int) optimizelyClient.getFeatureVariableInteger(
                INTEGER_FEATURE_KEY,
                INTEGER_VARIABLE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ));
        verifyNoInteractions(logger);
    }

    //FeatureVariableInteger Scenario#3 if feature not found
    @Test
    public void testGoodGetFeatureVariableIntegerInvalidFeatureKey() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        //Scenario#3 if feature not found
        assertNull(optimizelyClient.getFeatureVariableInteger(
                "invalidFeatureKey",
                "invalidVariableKey",
                GENERIC_USER_ID
        ));
    }

    //FeatureVariableInteger Scenario#4 if variable not found
    @Test
    public void testGoodGetFeatureVariableIntegerInvalidVariableKey() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        //Scenario#4 if variable not found
        assertNull(optimizelyClient.getFeatureVariableInteger(
                INTEGER_FEATURE_KEY,
                "invalidVariableKey",
                GENERIC_USER_ID
        ));
    }

    @Test
    public void testBadGetFeatureVariableInteger() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        //Scenario#1 without attributes
        assertNull(optimizelyClient.getFeatureVariableInteger(
                INTEGER_FEATURE_KEY,
                INTEGER_VARIABLE_KEY,
                GENERIC_USER_ID
        ));
        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} integer for user {}",
                INTEGER_FEATURE_KEY,
                INTEGER_VARIABLE_KEY,
                GENERIC_USER_ID
        );

        //Scenario#2 with attributes
        assertNull(optimizelyClient.getFeatureVariableInteger(
                INTEGER_FEATURE_KEY,
                INTEGER_VARIABLE_KEY,
                GENERIC_USER_ID,
                Collections.<String, String>emptyMap()
        ));

        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} integer for user {} with attributes",
                INTEGER_FEATURE_KEY,
                INTEGER_VARIABLE_KEY,
                GENERIC_USER_ID
        );
    }

    /*
     * FeatureVariableString
     * Scenario#1 Without attributes in which user
     * was not bucketed into any variation for feature flag will return default value which is 'H' in config
     */
    @Test
    public void testGoodGetFeatureVariableStringWithoutAttr() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );
        String defaultValueOfStringVar = "H";

        assertEquals(defaultValueOfStringVar, optimizelyClient.getFeatureVariableString(
                STRING_FEATURE_KEY,
                STRING_VARIABLE_KEY,
                GENERIC_USER_ID
        ));
    }

    //FeatureVariableString Scenario#2 with attributes
    @Test
    public void testGoodGetFeatureVariableStringWithAttr() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger);

        assertEquals("F", optimizelyClient.getFeatureVariableString(
                STRING_FEATURE_KEY,
                STRING_VARIABLE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        ));
        verifyNoInteractions(logger);
    }

    //FeatureVariableString Scenario#3 if feature not found
    @Test
    public void testGoodGetFeatureVariableStringInvalidFeatureKey() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger);

        assertNull(optimizelyClient.getFeatureVariableString(
                "invalidFeatureKey",
                "invalidVariableKey",
                GENERIC_USER_ID
        ));
    }

    //FeatureVariableString Scenario#4 if variable not found
    @Test
    public void testGoodGetFeatureVariableStringInvalidVariableKey() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        //Scenario#4 if variable not found
        assertNull(optimizelyClient.getFeatureVariableString(
                STRING_FEATURE_KEY,
                "invalidVariableKey",
                GENERIC_USER_ID
        ));
    }

    @Test
    public void testBadGetFeatureVariableString() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        //Scenario#1 without attributes
        assertNull(optimizelyClient.getFeatureVariableString(
                STRING_FEATURE_KEY,
                STRING_VARIABLE_KEY,
                GENERIC_USER_ID
        ));

        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} string for user {}",
                STRING_FEATURE_KEY,
                STRING_VARIABLE_KEY,
                GENERIC_USER_ID
        );

        //Scenario#2 with attributes
        assertNull(optimizelyClient.getFeatureVariableString(
                STRING_FEATURE_KEY,
                STRING_VARIABLE_KEY,
                GENERIC_USER_ID,
                Collections.<String, String>emptyMap()
        ));

        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} string for user {} with attributes",
                STRING_FEATURE_KEY,
                STRING_VARIABLE_KEY,
                GENERIC_USER_ID
        );
    }

    /*
     * FeatureVariableJSON
     * Scenario#1 Without attributes in which user
     * was not bucketed into any variation for feature flag will return default value which is
     * '{"k1":"v1","k2":3.5,"k3":true,"k4":{"kk1":"vv1","kk2":false}}' in config
     */
    @Test
    public void testGetFeatureVariableJSONWithoutAttr() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );
        String defaultValueOfStringVar = "{\"k1\":\"v1\",\"k2\":3.5,\"k3\":true,\"k4\":{\"kk1\":\"vv1\",\"kk2\":false}}";

        OptimizelyJSON json = optimizelyClient.getFeatureVariableJSON(
                STRING_FEATURE_KEY,
                JSON_VARIABLE_KEY,
                GENERIC_USER_ID
        );

        assertTrue(compareJsonStrings(json.toString(), defaultValueOfStringVar));
    }

    //FeatureVariableJSON Scenario#2 with attributes
    @Test
    public void testGetFeatureVariableJsonWithAttr() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger);


        String defaultValueOfStringVar = "{\"k1\":\"s1\",\"k2\":103.5,\"k3\":false,\"k4\":{\"kk1\":\"ss1\",\"kk2\":true}}";

        OptimizelyJSON json = optimizelyClient.getFeatureVariableJSON(
                STRING_FEATURE_KEY,
                JSON_VARIABLE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        );

        assertTrue(compareJsonStrings(json.toString(), defaultValueOfStringVar));
        verifyNoInteractions(logger);
    }

    //FeatureVariableJSON Scenario#3 if feature not found
    @Test
    public void testGetFeatureVariableJsonInvalidFeatureKey() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger);

        assertNull(optimizelyClient.getFeatureVariableJSON(
                "invalidFeatureKey",
                "invalidVariableKey",
                GENERIC_USER_ID
        ));
    }

    //FeatureVariableJSON Scenario#4 if variable not found
    @Test
    public void testGetFeatureVariableJsonInvalidVariableKey() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        //Scenario#4 if variable not found
        assertNull(optimizelyClient.getFeatureVariableJSON(
                STRING_FEATURE_KEY,
                "invalidVariableKey",
                GENERIC_USER_ID
        ));
    }

    @Test
    public void testBadGetFeatureVariableJson() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        //Scenario#1 without attributes
        assertNull(optimizelyClient.getFeatureVariableJSON(
                STRING_FEATURE_KEY,
                JSON_VARIABLE_KEY,
                GENERIC_USER_ID
        ));

        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} JSON for user {}.",
                STRING_FEATURE_KEY,
                JSON_VARIABLE_KEY,
                GENERIC_USER_ID
        );

        //Scenario#2 with attributes
        assertNull(optimizelyClient.getFeatureVariableJSON(
                STRING_FEATURE_KEY,
                JSON_VARIABLE_KEY,
                GENERIC_USER_ID,
                Collections.EMPTY_MAP
        ));

        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} JSON for user {} with attributes.",
                STRING_FEATURE_KEY,
                JSON_VARIABLE_KEY,
                GENERIC_USER_ID
        );
    }

    /*
     * getAllFeatureVariables
     * Scenario#1 Without attributes in which user
     * was not bucketed into any variation for feature flag will return default value which is
     * '{"first_letter":"H","json_patched":{"k1":"v1","k2":3.5,"k3":true,"k4":{"kk1":"vv1","kk2":false}},"rest_of_name":"arry"}' in config
     */
    @Test
    public void testGetAllFeatureVariablesWithoutAttr() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );
        String defaultValueOfStringVar = "{\"first_letter\":\"H\",\"json_patched\":{\"k1\":\"v1\",\"k2\":3.5,\"k3\":true,\"k4\":{\"kk1\":\"vv1\",\"kk2\":false}},\"rest_of_name\":\"arry\"}";

        OptimizelyJSON json = optimizelyClient.getAllFeatureVariables(
                STRING_FEATURE_KEY,
                GENERIC_USER_ID
        );

        assertTrue(compareJsonStrings(json.toString(), defaultValueOfStringVar));
    }

    //GetAllFeatureVariables Scenario#2 with attributes
    @Test
    public void testGetAllFeatureVariablesWithAttr() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger);


        String defaultValueOfStringVar = "{\"first_letter\":\"F\",\"json_patched\":{\"k1\":\"s1\",\"k2\":103.5,\"k3\":false,\"k4\":{\"kk1\":\"ss1\",\"kk2\":true}},\"rest_of_name\":\"eorge\"}";

        OptimizelyJSON json = optimizelyClient.getAllFeatureVariables(
                STRING_FEATURE_KEY,
                GENERIC_USER_ID,
                Collections.singletonMap("house", "Gryffindor")
        );

        assertTrue(compareJsonStrings(json.toString(), defaultValueOfStringVar));
        verifyNoInteractions(logger);
    }

    //GetAllFeatureVariables Scenario#3 if feature not found
    @Test
    public void testGetAllFeatureVariablesInvalidFeatureKey() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger);

        assertNull(optimizelyClient.getAllFeatureVariables(
                "invalidFeatureKey",
                GENERIC_USER_ID
        ));
    }

    @Test
    public void testBadGetAllFeatureVariables() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        //Scenario#1 without attributes
        assertNull(optimizelyClient.getAllFeatureVariables(
                STRING_FEATURE_KEY,
                GENERIC_USER_ID
        ));

        verify(logger).warn("Optimizely is not initialized, could not get feature {} all feature variables for user {}.",
                STRING_FEATURE_KEY,
                GENERIC_USER_ID
        );

        //Scenario#2 with attributes
        assertNull(optimizelyClient.getAllFeatureVariables(
                STRING_FEATURE_KEY,
                GENERIC_USER_ID,
                Collections.EMPTY_MAP
        ));

        verify(logger).warn("Optimizely is not initialized, could not get feature {} all feature variables for user {} with attributes.",
                STRING_FEATURE_KEY,
                GENERIC_USER_ID
        );
    }

    // Accessibility testing of OptimizelyJSON.getValue
    @Test
    public void testGetValueOfOptimizelyJson() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("kk1", "vv1");
        expectedMap.put("kk2", false);

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        OptimizelyJSON optimizelyJSON = optimizelyClient.getAllFeatureVariables(
                STRING_FEATURE_KEY,
                GENERIC_USER_ID
        );

        try {
            assertEquals(optimizelyJSON.getValue("first_letter", String.class), "H");
            assertEquals(optimizelyJSON.getValue("json_patched.k4", Map.class), expectedMap);

            // When given jsonKey does not exist
            assertNull(optimizelyJSON.getValue("json_patched.k5", String.class));
        } catch (JsonParseException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetOptimizelyConfig() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely,
                logger);
        OptimizelyConfig optimizelyConfig = optimizelyClient.getOptimizelyConfig();
        assertNotNull(optimizelyConfig.getExperimentsMap());
        assertNotNull(optimizelyConfig.getFeaturesMap());
        assertNotNull(optimizelyConfig.getRevision());
        assertNotNull(optimizelyConfig.getSdkKey());
        assertNotNull(optimizelyConfig.getEnvironmentKey());
        assertNotNull(optimizelyConfig.getAttributes());
        assertNotNull(optimizelyConfig.getAudiences());
        assertNotNull(optimizelyConfig.getEvents());
    }

    @Test
    public void testGetOptimizelyConfigReturnNullWhenConfigIsNull() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));
        OptimizelyClient optimizelyClient = new OptimizelyClient(null,
                logger);
        OptimizelyConfig optimizelyConfig = optimizelyClient.getOptimizelyConfig();
        assertNull(optimizelyConfig);
    }


    @Test
    public void testAddDecisionNotificationHandler() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        int notificationId = optimizelyClient.addDecisionNotificationHandler(decisionNotification -> {});
        assertTrue(optimizelyClient.getNotificationCenter().removeNotificationListener(notificationId));
    }

    @Test
    public void testAddTrackNotificationHandler() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );
        NotificationManager<TrackNotification> manager = optimizely.getNotificationCenter()
                .getNotificationManager(TrackNotification.class);

        int notificationId = optimizelyClient.addTrackNotificationHandler(trackNotification -> {});
        assertTrue(manager.remove(notificationId));
    }

    @Test
    public void testAddingTrackNotificationHandlerWithInvalidOptimizely() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                null,
                logger
        );
        NotificationManager<TrackNotification> manager = optimizely.getNotificationCenter()
                .getNotificationManager(TrackNotification.class);

        int notificationId = optimizelyClient.addTrackNotificationHandler(trackNotification -> {});
        assertEquals(-1, notificationId);
        assertFalse(manager.remove(notificationId));
    }

    @Test
    public void testAddingDecisionNotificationHandlerWithInvalidOptimizely() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                null,
                logger
        );
        NotificationManager<DecisionNotification> manager = optimizely.getNotificationCenter()
                .getNotificationManager(DecisionNotification.class);
        int notificationId = optimizelyClient.addDecisionNotificationHandler(decisionNotification -> {});
        assertEquals(-1, notificationId);
        assertFalse(manager.remove(notificationId));
    }

    @Test
    public void testAddingDecisionNotificationHandlerTwice() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );
        NotificationHandler<DecisionNotification> decisionNotificationHandler = new NotificationHandler<DecisionNotification>() {
            @Override
            public void handle(DecisionNotification decisionNotification) {
            }
        };
        int notificationId = optimizelyClient.addDecisionNotificationHandler(decisionNotificationHandler);
        int notificationId2 = optimizelyClient.addDecisionNotificationHandler(decisionNotificationHandler);
        assertNotEquals(-1, notificationId);
        assertEquals(-1, notificationId2);
        assertTrue(optimizelyClient.getNotificationCenter().removeNotificationListener(notificationId));
        assertFalse(optimizelyClient.getNotificationCenter().removeNotificationListener(notificationId2));
    }

    @Test
    public void testAddUpdateConfigNotificationHandler() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        int notificationId = optimizelyClient.addUpdateConfigNotificationHandler(notification -> {});
        assertTrue(optimizely.getNotificationCenter()
                .getNotificationManager(UpdateConfigNotification.class).remove(notificationId));
    }

    @Test
    public void testAddUpdateConfigNotificationHandlerWithInvalidOptimizely() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                null,
                logger
        );

        int notificationId = optimizelyClient.addUpdateConfigNotificationHandler(notification -> {});
        assertEquals(-1, notificationId);
        assertFalse(optimizely.getNotificationCenter()
                .getNotificationManager(UpdateConfigNotification.class).remove(notificationId));
    }

    @Test
    public void testAddLogEventNotificationHandler() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                optimizely,
                logger
        );

        int notificationId = optimizelyClient.addLogEventNotificationHandler(notification -> {});
        assertTrue(optimizely.getNotificationCenter()
                .getNotificationManager(LogEvent.class).remove(notificationId));
    }

    @Test
    public void testAddLogEventNotificationHandlerWithInvalidOptimizely() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(
                null,
                logger
        );

        int notificationId = optimizelyClient.addLogEventNotificationHandler(notification -> {});
        assertEquals(-1, notificationId);
        assertFalse(optimizely.getNotificationCenter()
                .getNotificationManager(LogEvent.class).remove(notificationId));
    }

    // OptimizelyUserContext + Decide API

    @Test
    public void testCreateUserContext() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        OptimizelyUserContext userContext = optimizelyClient.createUserContext(GENERIC_USER_ID);
        assertEquals(userContext.getUserId(), GENERIC_USER_ID);
        assert(userContext.getAttributes().isEmpty());
    }

    @Test
    public void testCreateUserContext_withAttributes() {
        Map<String, Object> attributes = Collections.singletonMap("house", "Gryffindor");

        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        OptimizelyUserContext userContext = optimizelyClient.createUserContext(GENERIC_USER_ID, attributes);
        assertEquals(userContext.getUserId(), GENERIC_USER_ID);
        assertEquals(userContext.getAttributes(), attributes);
    }

    @Test
    public void testCreateUserContext_withVuid() {
        String vuid = "test-vuid";
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger, vuid);
        OptimizelyUserContext userContext = optimizelyClient.createUserContext();
        assertEquals(userContext.getUserId(), "test-vuid");
        assert(userContext.getAttributes().isEmpty());
    }

    @Test
    public void testCreateUserContext_withVuid_withAttributes() {
        String vuid = "test-vuid";
        Map<String, Object> attributes = Collections.singletonMap("house", "Gryffindor");

        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger, vuid);
        OptimizelyUserContext userContext = optimizelyClient.createUserContext(attributes);
        assertEquals(userContext.getUserId(), "test-vuid");
        assertEquals(userContext.getAttributes(), attributes);
    }

    @Test
    public void testVuidRegister() {
        Optimizely mockOptimizely = mock(Optimizely.class);
        when(mockOptimizely.isValid()).thenReturn(true);

        OptimizelyClient optimizelyClient = new OptimizelyClient(mockOptimizely, logger, "any-vuid");

        verify(mockOptimizely).sendODPEvent(
                null,
                "client_initialized",
                null,
                null);
    }

    @Test
    // this should be enough to validate connection to the core java-sdk
    public void testDecide() {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String flagKey = INTEGER_FEATURE_KEY;
        Map<String, Object> attributes = Collections.singletonMap("house", "Gryffindor");

        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        OptimizelyUserContext userContext = optimizelyClient.createUserContext(GENERIC_USER_ID, attributes);
        OptimizelyDecision decision = userContext.decide(flagKey);
        OptimizelyJSON variablesExpected = new OptimizelyJSON(Collections.singletonMap("integer_variable", 2));

        assertEquals(decision.getVariationKey(), "Feorge");
        assertTrue(decision.getEnabled());
        assertEquals(decision.getVariables().toMap(), variablesExpected.toMap());
        assertEquals(decision.getRuleKey(), FEATURE_MULTI_VARIATE_EXPERIMENT_KEY);
        assertEquals(decision.getFlagKey(), flagKey);
        assertEquals(decision.getUserContext(), userContext);
        assertTrue(decision.getReasons().isEmpty());
    }

    @Test
    // this should be enough to validate connection to the core java-sdk
    public void testDecide_withoutDefaultDecideOptions() throws IOException {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        String datafile = loadRawResource(InstrumentationRegistry.getInstrumentation().getTargetContext(),R.raw.validprojectconfigv4);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        OptimizelyManager optimizelyManager = OptimizelyManager.builder(testProjectId).build(context);
        optimizelyManager.initialize(context, datafile);

        OptimizelyClient optimizelyClient = optimizelyManager.getOptimizely();
        OptimizelyUserContext userContext = optimizelyClient.createUserContext(GENERIC_USER_ID);
        OptimizelyDecision decision = userContext.decide(INTEGER_FEATURE_KEY);

        assertTrue(decision.getReasons().isEmpty());
    }

    @Test
    // this should be enough to validate connection to the core java-sdk
    public void testDecide_withDefaultDecideOptions() throws IOException {
        assumeTrue(datafileVersion == Integer.parseInt(ProjectConfig.Version.V4.toString()));

        List<OptimizelyDecideOption> defaultDecideOptions = Arrays.asList(OptimizelyDecideOption.INCLUDE_REASONS);

        String datafile = loadRawResource(InstrumentationRegistry.getInstrumentation().getTargetContext(),R.raw.validprojectconfigv4);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        OptimizelyManager optimizelyManager = OptimizelyManager.builder(testProjectId)
                .withDefaultDecideOptions(defaultDecideOptions)
                .build(context);
        optimizelyManager.initialize(context, datafile);

        OptimizelyClient optimizelyClient = optimizelyManager.getOptimizely();
        OptimizelyUserContext userContext = optimizelyClient.createUserContext(GENERIC_USER_ID);
        OptimizelyDecision decision = userContext.decide(INTEGER_FEATURE_KEY);

        assertTrue(decision.getReasons().size() > 0);
    }

    // Utils

    private boolean compareJsonStrings(String str1, String str2) {
        JsonParser parser = new JsonParser();

        JsonElement j1 = parser.parse(str1);
        JsonElement j2 = parser.parse(str2);
        return j1.equals(j2);
    }

    private Map parseJsonString(String str) {
        return new Gson().fromJson(str, Map.class);
    }
}
