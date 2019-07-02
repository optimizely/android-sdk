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

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.core.deps.guava.util.concurrent.ListeningExecutorService;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.OptimizelyRuntimeException;
import com.optimizely.ab.android.datafile_handler.DefaultDatafileHandler;
import com.optimizely.ab.android.event_handler.DefaultEventHandler;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.android.user_profile.DefaultUserProfileService;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.event.internal.payload.Event;
import com.optimizely.ab.notification.NotificationListener;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.exceptions.verification.junit.ArgumentsAreDifferent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class OptimizelyManagerBuilderTest {
    private Logger logger = mock(Logger.class);

    private String testProjectId = "7595190003";

    private String minDatafile = "{\"groups\": [], \"projectId\": \"8504447126\", \"variables\": [{\"defaultValue\": \"true\", \"type\": \"boolean\", \"id\": \"8516291943\", \"key\": \"test_variable\"}], \"version\": \"3\", \"experiments\": [{\"status\": \"Running\", \"key\": \"android_experiment_key\", \"layerId\": \"8499056327\", \"trafficAllocation\": [{\"entityId\": \"8509854340\", \"endOfRange\": 5000}, {\"entityId\": \"8505434669\", \"endOfRange\": 10000}], \"audienceIds\": [], \"variations\": [{\"variables\": [], \"id\": \"8509854340\", \"key\": \"var_1\"}, {\"variables\": [], \"id\": \"8505434669\", \"key\": \"var_2\"}], \"forcedVariations\": {}, \"id\": \"8509139139\"}], \"audiences\": [], \"anonymizeIP\": true, \"attributes\": [], \"revision\": \"7\", \"events\": [{\"experimentIds\": [\"8509139139\"], \"id\": \"8505434668\", \"key\": \"test_event\"}], \"accountId\": \"8362480420\"}";

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testBuilderWith() {
        ErrorHandler errorHandler = new ErrorHandler() {
            @Override
            public <T extends OptimizelyRuntimeException> void handleError(T exception) throws T {
                logger.error("Inside error handler", exception);
            }
        };

        OptimizelyManager manager = OptimizelyManager.builder(testProjectId).withUserProfileService(DefaultUserProfileService.newInstance(testProjectId, InstrumentationRegistry.getTargetContext()))
                .withDatafileDownloadInterval(30L)
                .withEventDispatchInterval(30L)
                .withDatafileHandler(new DefaultDatafileHandler())
                .withErrorHandler(errorHandler)
                .withEventHandler(DefaultEventHandler.getInstance(InstrumentationRegistry.getTargetContext()))
                .withLogger(logger).build(InstrumentationRegistry.getTargetContext());

        assertNotNull(manager);
        assertNotNull(manager.getDatafileHandler());
        assertNotNull(manager.getUserProfileService());
        assertNotNull(manager.getErrorHandler(InstrumentationRegistry.getTargetContext()));
        assertNotNull(manager.getEventHandler(InstrumentationRegistry.getTargetContext()));
    }

    @Test
    public void testBuilderWithOut() {
        OptimizelyManager manager = OptimizelyManager.builder(testProjectId).build(InstrumentationRegistry.getTargetContext());

        assertNotNull(manager);
        assertNotNull(manager.getDatafileHandler());
        assertNotNull(manager.getUserProfileService());
        assertNotNull(manager.getEventHandler(InstrumentationRegistry.getTargetContext()));
    }

}
