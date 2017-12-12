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

package com.optimizely.ab.android.sdk;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.android.event_handler.DefaultEventHandler;
import com.optimizely.ab.bucketing.Bucketer;
import com.optimizely.ab.bucketing.DecisionService;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.notification.NotificationListener;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class OptimizelyClientTest {
    private Logger logger = mock(Logger.class);
    private Optimizely optimizely;
    private Optimizely optimizelyWithV4Datafile;
    private EventHandler eventHandler;
    private Bucketer bucketer = mock(Bucketer.class);
    private static final String genericUserId = "genericUserId";
    private String testProjectId = "7595190003";

    private String minDatafile = "{\"groups\": [], \"projectId\": \"8504447126\", \"variables\": [{\"defaultValue\": \"true\", \"type\": \"boolean\", \"id\": \"8516291943\", \"key\": \"test_variable\"}], \"version\": \"3\", \"experiments\": [{\"status\": \"Running\", \"key\": \"android_experiment_key\", \"layerId\": \"8499056327\", \"trafficAllocation\": [{\"entityId\": \"8509854340\", \"endOfRange\": 5000}, {\"entityId\": \"8505434669\", \"endOfRange\": 10000}], \"audienceIds\": [], \"variations\": [{\"variables\": [], \"id\": \"8509854340\", \"key\": \"var_1\"}, {\"variables\": [], \"id\": \"8505434669\", \"key\": \"var_2\"}], \"forcedVariations\": {}, \"id\": \"8509139139\"}], \"audiences\": [], \"anonymizeIP\": true, \"attributes\": [], \"revision\": \"7\", \"events\": [{\"experimentIds\": [\"8509139139\"], \"id\": \"8505434668\", \"key\": \"test_event\"}], \"accountId\": \"8362480420\"}";
    private String minDataFilev4 = "{ \"accountId\": \"2360254204\", \"anonymizeIP\": true, \"projectId\": \"3918735994\", \"revision\": \"1480511547\", \"version\": \"4\", \"audiences\": [{ \"id\": \"3468206642\", \"name\": \"Gryffindors\", \"conditions\": \"[\\\"and\\\", [\\\"or\\\", [\\\"or\\\", {\\\"name\\\": \\\"house\\\", \\\"type\\\": \\\"custom_dimension\\\", \\\"value\\\":\\\"Gryffindor\\\"}]]]\" }, { \"id\": \"3988293898\", \"name\": \"Slytherins\", \"conditions\": \"[\\\"and\\\", [\\\"or\\\", [\\\"or\\\", {\\\"name\\\": \\\"house\\\", \\\"type\\\": \\\"custom_dimension\\\", \\\"value\\\":\\\"Slytherin\\\"}]]]\" }, { \"id\": \"4194404272\", \"name\": \"english_citizens\", \"conditions\": \"[\\\"and\\\", [\\\"or\\\", [\\\"or\\\", {\\\"name\\\": \\\"nationality\\\", \\\"type\\\": \\\"custom_dimension\\\", \\\"value\\\":\\\"English\\\"}]]]\" }, { \"id\": \"2196265320\", \"name\": \"audience_with_missing_value\", \"conditions\": \"[\\\"and\\\", [\\\"or\\\", [\\\"or\\\", {\\\"name\\\": \\\"nationality\\\", \\\"type\\\": \\\"custom_dimension\\\", \\\"value\\\": \\\"English\\\"}, {\\\"name\\\": \\\"nationality\\\", \\\"type\\\": \\\"custom_dimension\\\"}]]]\" } ], \"attributes\": [{ \"id\": \"553339214\", \"key\": \"house\" }, { \"id\": \"58339410\", \"key\": \"nationality\" } ], \"events\": [{ \"id\": \"3785620495\", \"key\": \"basic_event\", \"experimentIds\": [ \"1323241596\", \"2738374745\", \"3042640549\", \"3262035800\", \"3072915611\" ] }, { \"id\": \"3195631717\", \"key\": \"event_with_paused_experiment\", \"experimentIds\": [ \"2667098701\" ] }, { \"id\": \"1987018666\", \"key\": \"event_with_launched_experiments_only\", \"experimentIds\": [ \"3072915611\" ] } ], \"experiments\": [ { \"id\": \"1323241596\", \"key\": \"basic_experiment\", \"layerId\": \"1630555626\", \"status\": \"Running\", \"variations\": [{ \"id\": \"1423767502\", \"key\": \"A\", \"variables\": [] }, { \"id\": \"3433458314\", \"key\": \"B\", \"variables\": [] } ], \"trafficAllocation\": [{ \"entityId\": \"1423767502\", \"endOfRange\": 5000 }, { \"entityId\": \"3433458314\", \"endOfRange\": 10000 } ], \"audienceIds\": [], \"forcedVariations\": { \"Harry Potter\": \"A\", \"Tom Riddle\": \"B\" } }, { \"id\": \"3262035800\", \"key\": \"multivariate_experiment\", \"layerId\": \"3262035800\", \"status\": \"Running\", \"variations\": [{ \"id\": \"1880281238\", \"key\": \"Fred\", \"variables\": [{ \"id\": \"675244127\", \"value\": \"F\" }, { \"id\": \"4052219963\", \"value\": \"red\" } ] }, { \"id\": \"3631049532\", \"key\": \"Feorge\", \"variables\": [{ \"id\": \"675244127\", \"value\": \"F\" }, { \"id\": \"4052219963\", \"value\": \"eorge\" } ] }, { \"id\": \"4204375027\", \"key\": \"Gred\", \"variables\": [{ \"id\": \"675244127\", \"value\": \"G\" }, { \"id\": \"4052219963\", \"value\": \"red\" } ] }, { \"id\": \"2099211198\", \"key\": \"George\", \"variables\": [{ \"id\": \"675244127\", \"value\": \"G\" }, { \"id\": \"4052219963\", \"value\": \"eorge\" } ] } ], \"trafficAllocation\": [{ \"entityId\": \"1880281238\", \"endOfRange\": 2500 }, { \"entityId\": \"3631049532\", \"endOfRange\": 5000 }, { \"entityId\": \"4204375027\", \"endOfRange\": 7500 }, { \"entityId\": \"2099211198\", \"endOfRange\": 10000 } ], \"audienceIds\": [ \"3468206642\" ], \"forcedVariations\": { \"Fred\": \"Fred\", \"Feorge\": \"Feorge\", \"Gred\": \"Gred\", \"George\": \"George\" } }, { \"id\": \"2201520193\", \"key\": \"double_single_variable_feature_experiment\", \"layerId\": \"1278722008\", \"status\": \"Running\", \"variations\": [{ \"id\": \"1505457580\", \"key\": \"pi_variation\", \"variables\": [{ \"id\": \"4111654444\", \"value\": \"3.14\" }] }, { \"id\": \"119616179\", \"key\": \"euler_variation\", \"variables\": [{ \"id\": \"4111654444\", \"value\": \"2.718\" }] } ], \"trafficAllocation\": [{ \"entityId\": \"1505457580\", \"endOfRange\": 4000 }, { \"entityId\": \"119616179\", \"endOfRange\": 8000 } ], \"audienceIds\": [\"3988293898\"], \"forcedVariations\": {} }, { \"id\": \"2667098701\", \"key\": \"paused_experiment\", \"layerId\": \"3949273892\", \"status\": \"Paused\", \"variations\": [{ \"id\": \"391535909\", \"key\": \"Control\", \"variables\": [] }], \"trafficAllocation\": [{ \"entityId\": \"391535909\", \"endOfRange\": 10000 }], \"audienceIds\": [], \"forcedVariations\": { \"Harry Potter\": \"Control\" } }, { \"id\": \"3072915611\", \"key\": \"launched_experiment\", \"layerId\": \"3587821424\", \"status\": \"Launched\", \"variations\": [{ \"id\": \"1647582435\", \"key\": \"launch_control\", \"variables\": [] }], \"trafficAllocation\": [{ \"entityId\": \"1647582435\", \"endOfRange\": 8000 }], \"audienceIds\": [], \"forcedVariations\": {} }, { \"id\": \"748215081\", \"key\": \"experiment_with_malformed_audience\", \"layerId\": \"1238149537\", \"status\": \"Running\", \"variations\": [{ \"id\": \"535538389\", \"key\": \"var1\", \"variables\": [] }], \"trafficAllocation\": [{ \"entityId\": \"535538389\", \"endOfRange\": 10000 }], \"audienceIds\": [\"2196265320\"], \"forcedVariations\": {} }, { \"id\": \"3262035800\", \"key\": \"multivariate_experiment\", \"layerId\": \"3262035800\", \"status\": \"Running\", \"variations\": [{ \"id\": \"1880281238\", \"key\": \"Fred\", \"variables\": [{ \"id\": \"675244127\", \"value\": \"F\" }, { \"id\": \"4052219963\", \"value\": \"red\" } ] }, { \"id\": \"3631049532\", \"key\": \"Feorge\", \"variables\": [{ \"id\": \"675244127\", \"value\": \"F\" }, { \"id\": \"4052219963\", \"value\": \"eorge\" } ] }, { \"id\": \"4204375027\", \"key\": \"Gred\", \"variables\": [{ \"id\": \"675244127\", \"value\": \"G\" }, { \"id\": \"4052219963\", \"value\": \"red\" } ] }, { \"id\": \"2099211198\", \"key\": \"George\", \"variables\": [{ \"id\": \"675244127\", \"value\": \"G\" }, { \"id\": \"4052219963\", \"value\": \"eorge\" } ] } ], \"trafficAllocation\": [{ \"entityId\": \"1880281238\", \"endOfRange\": 2500 }, { \"entityId\": \"3631049532\", \"endOfRange\": 5000 }, { \"entityId\": \"4204375027\", \"endOfRange\": 7500 }, { \"entityId\": \"2099211198\", \"endOfRange\": 10000 } ], \"audienceIds\": [ \"3468206642\" ], \"forcedVariations\": { \"Fred\": \"Fred\", \"Feorge\": \"Feorge\", \"Gred\": \"Gred\", \"George\": \"George\" } } ], \"groups\": [{ \"id\": \"1015968292\", \"policy\": \"random\", \"experiments\": [{ \"id\": \"2738374745\", \"key\": \"first_grouped_experiment\", \"layerId\": \"3301900159\", \"status\": \"Running\", \"variations\": [{ \"id\": \"2377378132\", \"key\": \"A\", \"variables\": [] }, { \"id\": \"1179171250\", \"key\": \"B\", \"variables\": [] } ], \"trafficAllocation\": [{ \"entityId\": \"2377378132\", \"endOfRange\": 5000 }, { \"entityId\": \"1179171250\", \"endOfRange\": 10000 } ], \"audienceIds\": [ \"3468206642\" ], \"forcedVariations\": { \"Harry Potter\": \"A\", \"Tom Riddle\": \"B\" } }, { \"id\": \"3042640549\", \"key\": \"second_grouped_experiment\", \"layerId\": \"2625300442\", \"status\": \"Running\", \"variations\": [{ \"id\": \"1558539439\", \"key\": \"A\", \"variables\": [] }, { \"id\": \"2142748370\", \"key\": \"B\", \"variables\": [] } ], \"trafficAllocation\": [{ \"entityId\": \"1558539439\", \"endOfRange\": 5000 }, { \"entityId\": \"2142748370\", \"endOfRange\": 10000 } ], \"audienceIds\": [ \"3468206642\" ], \"forcedVariations\": { \"Hermione Granger\": \"A\", \"Ronald Weasley\": \"B\" } } ], \"trafficAllocation\": [{ \"entityId\": \"2738374745\", \"endOfRange\": 4000 }, { \"entityId\": \"3042640549\", \"endOfRange\": 8000 } ] }, { \"id\": \"2606208781\", \"policy\": \"random\", \"experiments\": [{ \"id\": \"4138322202\", \"key\": \"mutex_group_2_experiment_1\", \"layerId\": \"3755588495\", \"status\": \"Running\", \"variations\": [{ \"id\": \"1394671166\", \"key\": \"mutex_group_2_experiment_1_variation_1\", \"variables\": [{ \"id\": \"2059187672\", \"value\": \"mutex_group_2_experiment_1_variation_1\" }] }], \"audienceIds\": [], \"forcedVariations\": {}, \"trafficAllocation\": [{ \"entityId\": \"1394671166\", \"endOfRange\": 10000 }] }, { \"id\": \"1786133852\", \"key\": \"mutex_group_2_experiment_2\", \"layerId\": \"3818002538\", \"status\": \"Running\", \"variations\": [{ \"id\": \"1619235542\", \"key\": \"mutex_group_2_experiment_2_variation_2\", \"variables\": [{ \"id\": \"2059187672\", \"value\": \"mutex_group_2_experiment_2_variation_2\" }] }], \"trafficAllocation\": [{ \"entityId\": \"1619235542\", \"endOfRange\": 10000 }], \"audienceIds\": [], \"forcedVariations\": {} } ], \"trafficAllocation\": [{ \"entityId\": \"4138322202\", \"endOfRange\": 5000 }, { \"entityId\": \"1786133852\", \"endOfRange\": 10000 } ] } ], \"featureFlags\": [{ \"id\": \"4195505407\", \"key\": \"boolean_feature\", \"rolloutId\": \"\", \"experimentIds\": [], \"variables\": [] }, { \"id\": \"3926744821\", \"key\": \"double_single_variable_feature\", \"rolloutId\": \"\", \"experimentIds\": [\"2201520193\"], \"variables\": [{ \"id\": \"4111654444\", \"key\": \"double_variable\", \"type\": \"double\", \"defaultValue\": \"14.99\" }] }, { \"id\": \"3281420120\", \"key\": \"integer_single_variable_feature\", \"rolloutId\": \"2048875663\", \"experimentIds\": [], \"variables\": [{ \"id\": \"593964691\", \"key\": \"integer_variable\", \"type\": \"integer\", \"defaultValue\": \"7\" }] }, { \"id\": \"2591051011\", \"key\": \"boolean_single_variable_feature\", \"rolloutId\": \"\", \"experimentIds\": [], \"variables\": [{ \"id\": \"3974680341\", \"key\": \"boolean_variable\", \"type\": \"boolean\", \"defaultValue\": \"true\" }] }, { \"id\": \"2079378557\", \"key\": \"string_single_variable_feature\", \"rolloutId\": \"1058508303\", \"experimentIds\": [], \"variables\": [{ \"id\": \"2077511132\", \"key\": \"string_variable\", \"type\": \"string\", \"defaultValue\": \"wingardium leviosa\" }] }, { \"id\": \"3263342226\", \"key\": \"multi_variate_feature\", \"rolloutId\": \"813411034\", \"experimentIds\": [\"3262035800\"], \"variables\": [{ \"id\": \"675244127\", \"key\": \"first_letter\", \"type\": \"string\", \"defaultValue\": \"H\" }, { \"id\": \"4052219963\", \"key\": \"rest_of_name\", \"type\": \"string\", \"defaultValue\": \"arry\" } ] }, { \"id\": \"3263342226\", \"key\": \"mutex_group_feature\", \"rolloutId\": \"\", \"experimentIds\": [\"4138322202\", \"1786133852\"], \"variables\": [{ \"id\": \"2059187672\", \"key\": \"correlating_variation_name\", \"type\": \"string\", \"defaultValue\": \"null\" }] } ], \"rollouts\": [{ \"id\": \"1058508303\", \"experiments\": [{ \"id\": \"1785077004\", \"key\": \"1785077004\", \"status\": \"Running\", \"layerId\": \"1058508303\", \"audienceIds\": [], \"forcedVariations\": {}, \"variations\": [{ \"id\": \"1566407342\", \"key\": \"1566407342\", \"variables\": [{ \"id\": \"2077511132\", \"value\": \"lumos\" }] }], \"trafficAllocation\": [{ \"entityId\": \"1566407342\", \"endOfRange\": 5000 }] }] }, { \"id\": \"813411034\", \"experiments\": [{ \"id\": \"3421010877\", \"key\": \"3421010877\", \"status\": \"Running\", \"layerId\": \"813411034\", \"audienceIds\": [\"3468206642\"], \"forcedVariations\": {}, \"variations\": [{ \"id\": \"521740985\", \"key\": \"521740985\", \"variables\": [{ \"id\": \"675244127\", \"value\": \"G\" }, { \"id\": \"4052219963\", \"value\": \"odric\" } ] }], \"trafficAllocation\": [{ \"entityId\": \"521740985\", \"endOfRange\": 5000 }] }, { \"id\": \"600050626\", \"key\": \"600050626\", \"status\": \"Running\", \"layerId\": \"813411034\", \"audienceIds\": [\"3988293898\"], \"forcedVariations\": {}, \"variations\": [{ \"id\": \"180042646\", \"key\": \"180042646\", \"variables\": [{ \"id\": \"675244127\", \"value\": \"S\" }, { \"id\": \"4052219963\", \"value\": \"alazar\" } ] }], \"trafficAllocation\": [{ \"entityId\": \"180042646\", \"endOfRange\": 5000 }] }, { \"id\": \"2637642575\", \"key\": \"2637642575\", \"status\": \"Running\", \"layerId\": \"813411034\", \"audienceIds\": [\"4194404272\"], \"forcedVariations\": {}, \"variations\": [{ \"id\": \"2346257680\", \"key\": \"2346257680\", \"variables\": [{ \"id\": \"675244127\", \"value\": \"D\" }, { \"id\": \"4052219963\", \"value\": \"udley\" } ] }], \"trafficAllocation\": [{ \"entityId\": \"2346257680\", \"endOfRange\": 5000 }] }, { \"id\": \"828245624\", \"key\": \"828245624\", \"status\": \"Running\", \"layerId\": \"813411034\", \"audienceIds\": [], \"forcedVariations\": {}, \"variations\": [{ \"id\": \"3137445031\", \"key\": \"3137445031\", \"variables\": [{ \"id\": \"675244127\", \"value\": \"M\" }, { \"id\": \"4052219963\", \"value\": \"uggle\" } ] }], \"trafficAllocation\": [{ \"entityId\": \"3137445031\", \"endOfRange\": 5000 }] } ] }, { \"id\": \"2048875663\", \"experiments\": [{ \"id\": \"3794675122\", \"key\": \"3794675122\", \"status\": \"Running\", \"layerId\": \"2048875663\", \"audienceIds\": [], \"forcedVariations\": {}, \"variations\": [{ \"id\": \"589640735\", \"key\": \"589640735\", \"variables\": [] }], \"trafficAllocation\": [{ \"entityId\": \"589640735\", \"endOfRange\": 10000 }] }] } ], \"variables\": [] }";

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
    @Before
    public void setUp() throws Exception {
        eventHandler = spy(DefaultEventHandler.getInstance(InstrumentationRegistry.getTargetContext()));
        optimizely = Optimizely.builder(minDatafile, eventHandler).build();
        optimizelyWithV4Datafile = Optimizely.builder(minDataFilev4, eventHandler).build();
        when(bucketer.bucket(optimizely.getProjectConfig().getExperiments().get(0), "1")).thenReturn(optimizely.getProjectConfig().getExperiments().get(0).getVariations().get(0));
        spyOnConfig();
    }

    @Test
    public void testGoodActivation() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        Variation v = optimizelyClient.activate("android_experiment_key", "1");
        assertNotNull(v);

    }

    @Test
    public void testGoodForcedActivation() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        // bucket will always return var_1
        Variation v = optimizelyClient.activate("android_experiment_key", "1");
        assertEquals(v.getKey(), "var_1");
        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertTrue(didSetForced);
        v = optimizelyClient.activate("android_experiment_key", "1");
        assertNotNull(v);
        assertEquals(v.getKey(), "var_2");
        v = optimizelyClient.getForcedVariation("android_experiment_key", "1");
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));
    }

    @Test
    public void testGoodForceAActivationAttrib() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        // bucket will always return var_1
        Variation v = optimizelyClient.activate("android_experiment_key", "1", attributes);
        assertEquals(v.getKey(), "var_1");

        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertTrue(didSetForced);
        v = optimizelyClient.activate("android_experiment_key", "1", attributes);
        assertNotNull(v);
        assertEquals(v.getKey(), "var_2");
        v = optimizelyClient.getForcedVariation("android_experiment_key", "1");
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));
    }

    @Test
    public void testGoodActivationAttrib() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        Variation v = optimizelyClient.activate("android_experiment_key", "1", attributes);
        assertNotNull(v);
    }

    @Test
    public void testBadForcedActivationAttrib() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertFalse(didSetForced);
        Variation v = optimizelyClient.activate("1", "1", new HashMap<String, String>());
        verify(logger).warn("Optimizely is not initialized, could not activate experiment {} " +
                "for user {} with attributes", "1", "1");
        assertNull(v);
        v = optimizelyClient.getForcedVariation("android_experiment_key", "1");
        assertNull(v);

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));

    }


    @Test
    public void testBadActivationAttrib() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.activate("1", "1", new HashMap<String, String>());
        verify(logger).warn("Optimizely is not initialized, could not activate experiment {} " +
                "for user {} with attributes", "1", "1");
    }

    @Test
    public void testGoodForcedTrack() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);

        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizely.getProjectConfig();

        optimizelyClient.track("test_event", "1");

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

        verify(config).getForcedVariation("android_experiment_key", "1");

        Variation v = optimizelyClient.getForcedVariation("android_experiment_key", "1");
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));

    }

    @Test
    public void testGoodTrack() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        optimizelyClient.track("test_event", "1");
        verifyZeroInteractions(logger);
    }

    @Test
    public void testBadTrack() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.track("test_event", "1");
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}", "test_event", "1");
    }

    @Test
    public void testBadForcedTrack() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertFalse(didSetForced);
        optimizelyClient.track("test_event", "1");
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}",
                "test_event",
                "1");

        Variation v = optimizelyClient.getForcedVariation("android_experiment_key", "1");
        assertNull(v);

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));
    }

    @Test
    public void testGoodForcedTrackAttrib() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();

        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizely.getProjectConfig();

        optimizelyClient.track("test_event", "1", attributes);

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

        verify(config).getForcedVariation("android_experiment_key", "1");

        Variation v = optimizelyClient.getForcedVariation("android_experiment_key", "1");
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));

    }

    @Test
    public void testGoodTrackAttrib() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();

        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizely.getProjectConfig();

        optimizelyClient.track("test_event", "1", attributes);

        verifyZeroInteractions(logger);

        verify(config).getForcedVariation("android_experiment_key", "1");

        Variation v = optimizelyClient.getForcedVariation("android_experiment_key", "1");
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));
    }

    @Test
    public void testBadTrackAttrib() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.track("event1", "1", attributes);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with attributes", "event1", "1");
    }

    @Test
    public void testBadForcedTrackAttrib() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();

        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertFalse(didSetForced);
        optimizelyClient.track("event1", "1", attributes);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with attributes", "event1", "1");

        Variation v = optimizelyClient.getForcedVariation("android_experiment_key", "1");
        assertNull(v);

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));

    }

    @Test
    public void testGoodForcedTrackEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);

        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizely.getProjectConfig();

        optimizelyClient.track("test_event", "1", 1L);

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

        verify(config).getForcedVariation("android_experiment_key", "1");

        Variation v = optimizelyClient.getForcedVariation("android_experiment_key", "1");
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));
    }

    @Test
    public void testGoodTrackEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        optimizelyClient.track("test_event", "1", 1L);
        verifyZeroInteractions(logger);
    }

    @Test
    public void testBadTrackEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        optimizelyClient.track("event1", "1", 1L);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with value {}", "event1", "1", 1L);
    }

    @Test
    public void testBadForcedTrackEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertFalse(didSetForced);
        optimizelyClient.track("event1", "1", 1L);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with value {}", "event1", "1", 1L);

        Variation v = optimizelyClient.getForcedVariation("android_experiment_key", "1");
        assertNull(v);

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));
    }

    @Test
    public void testGoodTrackAttribEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.track("test_event", "1", attributes, 1L);
        verifyZeroInteractions(logger);
    }

    @Test
    public void testGoodForcedTrackAttribEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();

        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizely.getProjectConfig();

        optimizelyClient.track("test_event", "1", attributes, 1L);

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

        verify(config).getForcedVariation("android_experiment_key", "1");

        Variation v = optimizelyClient.getForcedVariation("android_experiment_key", "1");
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));
    }

    @Test
    public void testBadTrackAttribEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.track("event1", "1", attributes, 1L);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with value {} and attributes", "event1", "1", 1L);
    }

    @Test
    public void testBadForcedTrackAttribEventVal() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();

        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertFalse(didSetForced);

        optimizelyClient.track("event1", "1", attributes, 1L);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with value {} and attributes", "event1", "1", 1L);

        Variation v = optimizelyClient.getForcedVariation("android_experiment_key", "1");
        assertNull(v);

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));
    }

    @Test
    public void testTrackWithEventTags() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        attributes.put("foo", "bar");
        final HashMap<String, Object> eventTags = new HashMap<>();
        eventTags.put("foo", 843);
        optimizelyClient.track("test_event", "1", attributes, eventTags);
        verifyZeroInteractions(logger);
    }

    @Test
    public void testForcedTrackWithEventTags() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        attributes.put("foo", "bar");
        final HashMap<String, Object> eventTags = new HashMap<>();
        eventTags.put("foo", 843);

        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertTrue(didSetForced);

        ProjectConfig config = optimizely.getProjectConfig();

        optimizelyClient.track("test_event", "1", attributes, eventTags);

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

        verify(config).getForcedVariation("android_experiment_key", "1");

        Variation v = optimizelyClient.getForcedVariation("android_experiment_key", "1");
        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));
    }

    @Test
    public void testGoodGetVariation1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        Variation v = optimizelyClient.getVariation("android_experiment_key", "1");
        assertNotNull(v);
    }

    @Test
    public void testGoodGetVariation1Forced() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertTrue(didSetForced);
        Variation v = optimizelyClient.getForcedVariation("android_experiment_key", "1");

        assertEquals(v.getKey(), "var_2");

        v = optimizelyClient.getVariation("android_experiment_key", "1");

        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));

    }

    @Test
    public void testBadGetVariation1() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        Variation v = optimizelyClient.getVariation("android_experiment_key", "1");

        assertNull(v);

        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {}", "android_experiment_key", "1");

    }

    @Test
    public void testBadGetVariation1Forced() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertFalse(didSetForced);
        Variation v = optimizelyClient.getForcedVariation("android_experiment_key", "1");

        assertNull(v);

        v = optimizelyClient.getVariation("android_experiment_key", "1");

        assertNull(v);

        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {}", "android_experiment_key", "1");

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));
    }

    @Test
    public void testGoodGetVariationAttrib() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.getVariation("android_experiment_key", "1", attributes);
        verifyZeroInteractions(logger);
    }

    @Test
    public void testGoodForcedGetVariationAttrib() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertTrue(didSetForced);
        Variation v = optimizelyClient.getForcedVariation("android_experiment_key", "1");

        assertEquals(v.getKey(), "var_2");

        v = optimizelyClient.getVariation("android_experiment_key", "1", attributes);

        verifyZeroInteractions(logger);

        assertEquals(v.getKey(), "var_2");

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertTrue(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));

    }

    @Test
    public void testBadGetVariationAttrib() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        optimizelyClient.getVariation("android_experiment_key", "1", attributes);
        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {} with attributes", "android_experiment_key", "1");
    }

    @Test
    public void testBadForcedGetVariationAttrib() {
        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        boolean didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_2");

        assertFalse(didSetForced);
        Variation v = optimizelyClient.getForcedVariation("android_experiment_key", "1");

        assertNull(v);

        v = optimizelyClient.getVariation("android_experiment_key", "1", attributes);

        assertNull(v);

        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {} with attributes", "android_experiment_key", "1");

        didSetForced = optimizelyClient.setForcedVariation("android_experiment_key", "1", null);

        assertFalse(didSetForced);

        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));
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
        assertTrue(config.setForcedVariation("android_experiment_key", "1", "var_1"));
        assertEquals(config.getForcedVariation("android_experiment_key", "1"), config.getExperimentKeyMapping().get("android_experiment_key").getVariations().get(0));
        assertTrue(config.setForcedVariation("android_experiment_key", "1", null));
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
        assertFalse(optimizelyClient.setForcedVariation("android_experiment_key", "1", "var_1"));
        verify(logger).warn("Optimizely is not initialized, could not set forced variation");
        assertNull(optimizelyClient.getForcedVariation("android_experiment_key", "1"));
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

    //=======Feature Variables Testing===========
    @Test
    public void testGoodGetFeatureVariableBoolean() {
        String featureKey = "boolean_single_variable_feature";
        String variableKey = "boolean_variable";
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizelyWithV4Datafile, logger);

        //Scenario#1 Without attributes
        assertTrue(optimizelyClient.getFeatureVariableBoolean(featureKey, variableKey,genericUserId));

        //Scenario#2 With attributes
        assertTrue(optimizelyClient.getFeatureVariableBoolean(featureKey,
                variableKey,genericUserId,
                Collections.singletonMap("key", "value")));
        verifyZeroInteractions(logger);

        //Scenario#3 if feature not found
        assertNull(optimizelyClient.getFeatureVariableBoolean(
                "invalidFeatureKey",
                "invalidVariableKey",
                genericUserId));

        //Scenario#4 if variable not found
        assertNull(optimizelyClient.getFeatureVariableBoolean(
                featureKey,
                "invalidVariableKey",
                genericUserId));

    }

    @Test
    public void testBadGetFeatureVariableBoolean() {
        String featureKey = "boolean_single_variable_feature";
        String variableKey = "boolean_variable";

        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        //Scenario#1 without attributes
        assertNull(optimizelyClient.getFeatureVariableBoolean(featureKey,variableKey,genericUserId));
        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} boolean for user {}", featureKey                                   ,variableKey, genericUserId);

        //Scenario#2 with attributes
        assertNull(optimizelyClient.getFeatureVariableBoolean(featureKey,variableKey,genericUserId,Collections.<String, String>emptyMap()));
        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} boolean for user {} with attributes",                              featureKey,variableKey, genericUserId);
    }

    @Test
    public void testGoodGetFeatureVariableDouble() {
        String featureKey = "double_single_variable_feature";
        String variableKey = "double_variable";
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizelyWithV4Datafile, logger);

        //Scenario#1 without attributes
        assertEquals(14.99,optimizelyClient.getFeatureVariableDouble(
                featureKey,
                variableKey,
                genericUserId
        ));

        //Scenario#2 with attributes
        assertEquals(3.14, optimizelyClient.getFeatureVariableDouble(
                featureKey,
                variableKey,
                genericUserId,
                Collections.singletonMap("house", "Slytherin")
        ));
        verifyZeroInteractions(logger);

        //Scenario#3 if feature not found
        assertNull(optimizelyClient.getFeatureVariableDouble(
                "invalidFeatureKey",
                "invalidVariableKey",
                genericUserId));

        //Scenario#4 if variable not found
        assertNull(optimizelyClient.getFeatureVariableDouble(
                featureKey,
                "invalidVariableKey",
                genericUserId));
    }

    @Test
    public void testBadGetFeatureVariableDouble() {
        String featureKey = "double_single_variable_feature";
        String variableKey = "double_variablex";

        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        //Scenario#1 without attributes
        assertNull(optimizelyClient.getFeatureVariableDouble(featureKey,variableKey,genericUserId));
        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} double for user {}", featureKey                                    ,variableKey, genericUserId);

        //Scenario#2 with attributes
        assertNull(optimizelyClient.getFeatureVariableDouble(featureKey,variableKey,genericUserId,Collections.<String, String>emptyMap()));
        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} double for user {} with attributes",                               featureKey,variableKey, genericUserId);
    }

    @Test
    public void testGoodGetFeatureVariableInteger() {
        String featureKey = "integer_single_variable_feature";
        String variableKey = "integer_variable";
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizelyWithV4Datafile, logger);

        //Scenario#1 without attributes
        assertEquals(7,(int) optimizelyClient.getFeatureVariableInteger(
                featureKey,
                variableKey,
                genericUserId
        ));

        //Scenario#2 with attributes
        assertEquals(7, (int)optimizelyClient.getFeatureVariableInteger(
                featureKey,
                variableKey,
                genericUserId,
                Collections.<String, String>emptyMap()
        ));
        verifyZeroInteractions(logger);

        //Scenario#3 if feature not found
        assertNull(optimizelyClient.getFeatureVariableInteger(
                "invalidFeatureKey",
                "invalidVariableKey",
                genericUserId));

        //Scenario#4 if variable not found
        assertNull(optimizelyClient.getFeatureVariableInteger(
                featureKey,
                "invalidVariableKey",
                genericUserId));
    }

    @Test
    public void testBadGetFeatureVariableInteger() {
        String featureKey = "integer_single_variable_feature";
        String variableKey = "integer_variable";

        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        //Scenario#1 without attributes
        assertNull(optimizelyClient.getFeatureVariableInteger(featureKey,variableKey,genericUserId));
        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} integer for user {}", featureKey                                   ,variableKey, genericUserId);

        //Scenario#2 with attributes
        assertNull(optimizelyClient.getFeatureVariableInteger(featureKey,variableKey,genericUserId,Collections.<String, String>emptyMap()));
        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} integer for user {} with attributes",                              featureKey,variableKey, genericUserId);
    }


    @Test
    public void testGoodGetFeatureVariableString() {
        String featureKey = "multi_variate_feature";
        String variableKey = "first_letter";
        OptimizelyClient optimizelyClient = new OptimizelyClient(optimizelyWithV4Datafile, logger);

        //Scenario#1 Without attributes
        assertEquals("M", optimizelyClient.getFeatureVariableString(
                featureKey,
                variableKey,
                genericUserId
        ));

        //Scenario#2 with attributes
        assertEquals("G", optimizelyClient.getFeatureVariableString(
                featureKey,
                variableKey,
                genericUserId,
                Collections.singletonMap("house", "Gryffindor")
        ));
        verifyZeroInteractions(logger);

        //Scenario#3 if feature not found
        assertNull(optimizelyClient.getFeatureVariableString(
                "invalidFeatureKey",
                "invalidVariableKey",
                genericUserId));

        //Scenario#4 if variable not found
        assertNull(optimizelyClient.getFeatureVariableString(
                featureKey,
                "invalidVariableKey",
                genericUserId));
    }

    @Test
    public void testBadGetFeatureVariableString() {
        String featureKey = "multi_variate_feature";
        String variableKey = "first_letter";

        OptimizelyClient optimizelyClient = new OptimizelyClient(null, logger);

        //Scenario#1 without attributes
        assertNull(optimizelyClient.getFeatureVariableString(featureKey,variableKey,genericUserId));
        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} string for user {}", featureKey                                        ,variableKey, genericUserId);

        //Scenario#2 with attributes
        assertNull(optimizelyClient.getFeatureVariableString(featureKey,variableKey,genericUserId,Collections.<String, String>emptyMap()));
        verify(logger).warn("Optimizely is not initialized, could not get feature {} variable {} string for user {} with attributes",                                   featureKey,variableKey, genericUserId);
    }
}
