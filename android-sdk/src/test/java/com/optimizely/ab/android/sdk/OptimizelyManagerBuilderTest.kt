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
package com.optimizely.ab.android.sdk

import android.content.Context
import com.optimizely.ab.android.datafile_handler.DatafileHandler
import com.optimizely.ab.android.sdk.OptimizelyManager.Companion.builder
import com.optimizely.ab.android.user_profile.DefaultUserProfileService
import com.optimizely.ab.error.ErrorHandler
import com.optimizely.ab.event.EventHandler
import junit.framework.Assert
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.runners.MockitoJUnitRunner
import org.slf4j.Logger

@RunWith(MockitoJUnitRunner::class)
class OptimizelyManagerBuilderTest {
    private val testProjectId = "7595190003"
    private val logger: Logger? = null
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
    /**
     * Verify that building the [OptimizelyManager] with a polling interval less than 60
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
     * Verify that building the [OptimizelyManager] with a polling interval greater than 60
     * seconds is properly registered.
     */
    @Test
    fun testBuildWithValidPollingInterval() {
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(appContext.applicationContext).thenReturn(appContext)
        val manager = builder("1")
                .withDatafileDownloadInterval(901L)
                .build(appContext)
        assertEquals(901L, manager!!.datafileDownloadInterval)
    }

    @Test
    fun testBuildWithEventHandler() {
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(appContext.applicationContext).thenReturn(appContext)
        val eventHandler = Mockito.mock(EventHandler::class.java)
        val manager = builder(testProjectId)
                .withDatafileDownloadInterval(901L)
                .withEventHandler(eventHandler)
                .build(appContext)
        assertEquals(901L, manager!!.datafileDownloadInterval)
        Assert.assertEquals(manager!!.getEventHandler(appContext), eventHandler)
    }

    @Test
    fun testBuildWithErrorHandler() {
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(appContext.applicationContext).thenReturn(appContext)
        val errorHandler = Mockito.mock(ErrorHandler::class.java)
        val manager = builder(testProjectId)
                .withDatafileDownloadInterval(61L)
                .withErrorHandler(errorHandler)
                .build(appContext)
        manager!!.initialize(appContext, minDatafile)
        Assert.assertEquals(manager.getErrorHandler(appContext), errorHandler)
    }

    @Test
    fun testBuildWithDatafileHandler() {
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(appContext.applicationContext).thenReturn(appContext)
        val dfHandler = Mockito.mock(DatafileHandler::class.java)
        val manager = builder(testProjectId)
                .withDatafileDownloadInterval(61L)
                .withDatafileHandler(dfHandler)
                .build(appContext)
        manager!!.initialize(appContext, minDatafile)
        assertEquals(manager.datafileHandler, dfHandler)
    }

    @Test
    fun testBuildWithUserProfileService() {
        val appContext = Mockito.mock(Context::class.java)
        Mockito.`when`(appContext.applicationContext).thenReturn(appContext)
        val ups = DefaultUserProfileService.newInstance(testProjectId ,appContext)
        val manager = builder(testProjectId)
                .withDatafileDownloadInterval(61L)
                .withUserProfileService(ups)
                .build(appContext)
        manager!!.initialize(appContext, minDatafile)
        assertEquals(manager.userProfileService, ups)
    }
}