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

import com.optimizely.ab.android.datafile_handler.DatafileHandler;
import com.optimizely.ab.android.user_profile.DefaultUserProfileService;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.event.EventHandler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OptimizelyManagerBuilderTest {

    private String testProjectId = "7595190003";
    private Logger logger;

    private String minDatafile = "{\n" +
            "experiments: [ ],\n" +
            "version: \"2\",\n" +
            "audiences: [ ],\n" +
            "groups: [ ],\n" +
            "attributes: [ ],\n" +
            "projectId: \"" + testProjectId + "\",\n" +
            "accountId: \"6365361536\",\n" +
            "events: [ ],\n" +
            "revision: \"1\"\n" +
            "}";
    /**
     * Verify that building the {@link OptimizelyManager} with a polling interval less than 60
     * seconds defaults to 60 seconds.
     */
//    @Test
//    public void testBuildWithInvalidPollingInterval() {
//        Context appContext = mock(Context.class);
//        when(appContext.getApplicationContext()).thenReturn(appContext);
//        OptimizelyManager manager = OptimizelyManager.builder("1")
//                .withDatafileDownloadInterval(5L)
//                .build(appContext);
//
//        assertEquals(900L, manager.getDatafileDownloadInterval().longValue());
//    }

    /**
     * Verify that building the {@link OptimizelyManager} with a polling interval greater than 60
     * seconds is properly registered.
     */
    @Test
    public void testBuildWithValidPollingInterval() {
        Context appContext = mock(Context.class);
        when(appContext.getApplicationContext()).thenReturn(appContext);
        OptimizelyManager manager = OptimizelyManager.builder("1")
                .withDatafileDownloadInterval(901L)
                .build(appContext);

        assertEquals(901L, manager.getDatafileDownloadInterval().longValue());
    }

    @Test
    public void testBuildWithEventHandler() {
        Context appContext = mock(Context.class);
        when(appContext.getApplicationContext()).thenReturn(appContext);
        EventHandler eventHandler = mock(EventHandler.class);
        OptimizelyManager manager = OptimizelyManager.builder(testProjectId)
                .withDatafileDownloadInterval(901L)
                .withEventHandler(eventHandler)
                .build(appContext);

        assertEquals(901L, manager.getDatafileDownloadInterval().longValue());
        assertEquals(manager.getEventHandler(appContext), eventHandler);


    }

    @Test
    public void testBuildWithErrorHandler() {
        Context appContext = mock(Context.class);
        when(appContext.getApplicationContext()).thenReturn(appContext);
        ErrorHandler errorHandler = mock(ErrorHandler.class);
        OptimizelyManager manager = OptimizelyManager.builder(testProjectId)
                .withDatafileDownloadInterval(61L)
                .withErrorHandler(errorHandler)
                .build(appContext);

        manager.initialize(appContext, minDatafile);

        assertEquals(manager.getErrorHandler(appContext), errorHandler);
    }

    @Test
    public void testBuildWithDatafileHandler() {
        Context appContext = mock(Context.class);
        when(appContext.getApplicationContext()).thenReturn(appContext);
        DatafileHandler dfHandler = mock(DatafileHandler.class);
        OptimizelyManager manager = OptimizelyManager.builder(testProjectId)
                .withDatafileDownloadInterval(61L)
                .withDatafileHandler(dfHandler)
                .build(appContext);

        manager.initialize(appContext, minDatafile);

        assertEquals(manager.getDatafileHandler(), dfHandler);
    }

    @Test
    public void testBuildWithUserProfileService() {
        Context appContext = mock(Context.class);
        when(appContext.getApplicationContext()).thenReturn(appContext);
        DefaultUserProfileService ups = mock(DefaultUserProfileService.class);
        OptimizelyManager manager = OptimizelyManager.builder(testProjectId)
                .withDatafileDownloadInterval(61L)
                .withUserProfileService(ups)
                .build(appContext);

        manager.initialize(appContext, minDatafile);

        assertEquals(manager.getUserProfileService(), ups);
    }
}
