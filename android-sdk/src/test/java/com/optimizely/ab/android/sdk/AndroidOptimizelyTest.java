/*
 * Copyright 2016, Optimizely
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.optimizely.ab.android.sdk;

import com.optimizely.ab.Optimizely;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.HashMap;

import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AndroidOptimizely}
 */
@RunWith(MockitoJUnitRunner.class)
public class AndroidOptimizelyTest {

    @Mock Logger logger;
    @Mock Optimizely optimizely;

    @Test
    public void testGoodActivation1() {
        AndroidOptimizely androidOptimizely = new AndroidOptimizely(optimizely, logger);
        androidOptimizely.activate("1", "1");
        verify(optimizely).activate("1", "1");
    }

    @Test
    public void testBadActivation1() {
        AndroidOptimizely androidOptimizely = new AndroidOptimizely(null, logger);
        androidOptimizely.activate("1", "1");
        verify(logger).warn("Optimizely is not initialized, can't activate experiment {} " +
                "for user {}", "1", "1");
    }

    @Test
    public void testGoodActivation2() {
        AndroidOptimizely androidOptimizely = new AndroidOptimizely(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        androidOptimizely.activate("1", "1", attributes);
        verify(optimizely).activate("1", "1", attributes);
    }

    @Test
    public void testBadActivation2() {
        AndroidOptimizely androidOptimizely = new AndroidOptimizely(null, logger);
        androidOptimizely.activate("1", "1", new HashMap<String, String>());
        verify(logger).warn("Optimizely is not initialized, can't activate experiment {} " +
                "for user {} with attributes", "1", "1");
    }


    @Test
    public void testGoodTrack1() {
        AndroidOptimizely androidOptimizely = new AndroidOptimizely(optimizely, logger);
        androidOptimizely.track("event1", "1");
        verify(optimizely).track("event1", "1");
    }

    @Test
    public void testBadTrack1() {
        AndroidOptimizely androidOptimizely = new AndroidOptimizely(null, logger);
        androidOptimizely.track("event1", "1");
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {}", "event1", "1");
    }

    @Test
    public void testGoodTrack2() {
        AndroidOptimizely androidOptimizely = new AndroidOptimizely(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        androidOptimizely.track("event1", "1", attributes);
        verify(optimizely).track("event1", "1", attributes);
    }

    @Test
    public void testBadTrack2() {
        AndroidOptimizely androidOptimizely = new AndroidOptimizely(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        androidOptimizely.track("event1", "1", attributes);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with attributes", "event1", "1");
    }

    @Test
    public void testGoodTrack3() {
        AndroidOptimizely androidOptimizely = new AndroidOptimizely(optimizely, logger);
        androidOptimizely.track("event1", "1", 1L);
        verify(optimizely).track("event1", "1", 1L);
    }

    @Test
    public void testBadTrack3() {
        AndroidOptimizely androidOptimizely = new AndroidOptimizely(null, logger);
        androidOptimizely.track("event1", "1", 1L);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with value {}", "event1", "1", 1L);
    }

    @Test
    public void testGoodTrack4() {
        AndroidOptimizely androidOptimizely = new AndroidOptimizely(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        androidOptimizely.track("event1", "1", attributes, 1L);
        verify(optimizely).track("event1", "1", attributes, 1L);
    }

    @Test
    public void testBadTrack4() {
        AndroidOptimizely androidOptimizely = new AndroidOptimizely(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        androidOptimizely.track("event1", "1", attributes, 1L);
        verify(logger).warn("Optimizely is not initialized, could not track event {} for user {} " +
                "with value {} and attributes", "event1", "1", 1L);
    }

    @Test
    public void testGoodGetVariation1() {
        AndroidOptimizely androidOptimizely = new AndroidOptimizely(optimizely, logger);
        androidOptimizely.getVariation("1", "1");
        verify(optimizely).getVariation("1", "1");
    }

    @Test
    public void testBadGetVariation1() {
        AndroidOptimizely androidOptimizely = new AndroidOptimizely(null, logger);
        androidOptimizely.getVariation("1", "1");
        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                    "for user {}", "1", "1");
    }

    @Test
    public void testGoodGetVariation3() {
        AndroidOptimizely androidOptimizely = new AndroidOptimizely(optimizely, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        androidOptimizely.getVariation("1", "1", attributes);
        verify(optimizely).getVariation("1", "1", attributes);
    }

    @Test
    public void testBadGetVariation3() {
        AndroidOptimizely androidOptimizely = new AndroidOptimizely(null, logger);
        final HashMap<String, String> attributes = new HashMap<>();
        androidOptimizely.getVariation("1", "1", attributes);
        verify(logger).warn("Optimizely is not initialized, could not get variation for experiment {} " +
                "for user {} with attributes", "1", "1");
    }
}
