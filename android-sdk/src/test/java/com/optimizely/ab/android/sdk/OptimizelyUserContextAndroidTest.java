// Copyright 2025, Optimizely, Inc. and contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.optimizely.ab.android.sdk;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.OptimizelyForcedDecision;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;
import com.optimizely.ab.optimizelydecision.OptimizelyDecision;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.annotation.Nonnull;

/**
 * Tests for {@link OptimizelyUserContextAndroid}
 */
@RunWith(MockitoJUnitRunner.class)
public class OptimizelyUserContextAndroidTest {

    // Mock callback interfaces for testing async methods
    public interface OptimizelyDecisionCallback {
        void onDecision(@Nonnull OptimizelyDecision decision);
    }

    public interface OptimizelyDecisionsCallback {
        void onDecisions(@Nonnull Map<String, OptimizelyDecision> decisions);
    }

    @Mock
    Optimizely mockOptimizely;

    @Mock
    OptimizelyDecision mockDecision;

    @Mock
    OptimizelyDecisionCallback mockDecisionCallback;

    @Mock
    OptimizelyDecisionsCallback mockDecisionsCallback;

    private static final String TEST_USER_ID = "testUser123";
    private static final String TEST_FLAG_KEY = "testFlag";
    private static final List<String> TEST_FLAG_KEYS = Arrays.asList("flag1", "flag2");
    private static final List<OptimizelyDecideOption> TEST_OPTIONS = Arrays.asList(OptimizelyDecideOption.DISABLE_DECISION_EVENT);

    private Map<String, Object> testAttributes;
    private Map<String, OptimizelyForcedDecision> testForcedDecisions;
    private List<String> testQualifiedSegments;

    @Before
    public void setup() {
        testAttributes = new HashMap<>();
        testAttributes.put("isLoggedIn", true);
        testAttributes.put("userType", "premium");

        testForcedDecisions = new HashMap<>();
        testQualifiedSegments = Arrays.asList("segment1", "segment2");

        // Setup mock return values
        when(mockOptimizely.decideSync(any(), eq(TEST_FLAG_KEY), any())).thenReturn(mockDecision);
        when(mockOptimizely.decideForKeysSync(any(), eq(TEST_FLAG_KEYS), any())).thenReturn(Collections.emptyMap());
        when(mockOptimizely.decideAllSync(any(), any())).thenReturn(Collections.emptyMap());
    }

    @Test
    public void testConstructor_withBasicParameters() {
        // Test constructor with optimizely, userId, and attributes
        OptimizelyUserContextAndroid userContext = new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        );

        assertNotNull(userContext);
        assertEquals(TEST_USER_ID, userContext.getUserId());
        assertEquals(testAttributes, userContext.getAttributes());
    }

    @Test
    public void testConstructor_withForcedDecisionsAndSegments() {
        // Test constructor with forced decisions and qualified segments
        OptimizelyUserContextAndroid userContext = new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes,
            testForcedDecisions,
            testQualifiedSegments
        );

        assertNotNull(userContext);
        assertEquals(TEST_USER_ID, userContext.getUserId());
        assertEquals(testAttributes, userContext.getAttributes());
    }

    @Test
    public void testConstructor_withAllParameters() {
        // Test constructor with all parameters including shouldIdentifyUser
        OptimizelyUserContextAndroid userContext = new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes,
            testForcedDecisions,
            testQualifiedSegments,
            true
        );

        assertNotNull(userContext);
        assertEquals(TEST_USER_ID, userContext.getUserId());
        assertEquals(testAttributes, userContext.getAttributes());
    }

    @Test
    public void testDecide_withOptions() {
        OptimizelyUserContextAndroid userContext = new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        );

        OptimizelyDecision result = userContext.decide(TEST_FLAG_KEY, TEST_OPTIONS);

        verify(mockOptimizely).decideSync(any(OptimizelyUserContext.class), eq(TEST_FLAG_KEY), eq(TEST_OPTIONS));
        assertEquals(mockDecision, result);
    }

    @Test
    public void testDecide_withoutOptions() {
        OptimizelyUserContextAndroid userContext = new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        );

        OptimizelyDecision result = userContext.decide(TEST_FLAG_KEY);

        verify(mockOptimizely).decideSync(any(OptimizelyUserContext.class), eq(TEST_FLAG_KEY), eq(Collections.emptyList()));
        assertEquals(mockDecision, result);
    }

    @Test
    public void testDecideForKeys_withOptions() {
        OptimizelyUserContextAndroid userContext = new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        );

        Map<String, OptimizelyDecision> result = userContext.decideForKeys(TEST_FLAG_KEYS, TEST_OPTIONS);

        verify(mockOptimizely).decideForKeysSync(any(OptimizelyUserContext.class), eq(TEST_FLAG_KEYS), eq(TEST_OPTIONS));
        assertNotNull(result);
    }

    @Test
    public void testDecideForKeys_withoutOptions() {
        OptimizelyUserContextAndroid userContext = new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        );

        Map<String, OptimizelyDecision> result = userContext.decideForKeys(TEST_FLAG_KEYS);

        verify(mockOptimizely).decideForKeysSync(any(OptimizelyUserContext.class), eq(TEST_FLAG_KEYS), eq(Collections.emptyList()));
        assertNotNull(result);
    }

    @Test
    public void testDecideAll_withOptions() {
        OptimizelyUserContextAndroid userContext = new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        );

        Map<String, OptimizelyDecision> result = userContext.decideAll(TEST_OPTIONS);

        verify(mockOptimizely).decideAllSync(any(OptimizelyUserContext.class), eq(TEST_OPTIONS));
        assertNotNull(result);
    }

    @Test
    public void testDecideAll_withoutOptions() {
        OptimizelyUserContextAndroid userContext = new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        );

        Map<String, OptimizelyDecision> result = userContext.decideAll();

        verify(mockOptimizely).decideAllSync(any(OptimizelyUserContext.class), eq(Collections.emptyList()));
        assertNotNull(result);
    }

    // ===========================================
    // Tests for Async Methods
    // ===========================================

    @Test
    public void testDecideAsync_withOptions() {
        OptimizelyUserContextAndroid userContext = new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        );

        userContext.decideAsync(TEST_FLAG_KEY, mockDecisionCallback, TEST_OPTIONS);

        verify(mockOptimizely).decideAsync(any(OptimizelyUserContext.class), eq(TEST_FLAG_KEY), eq(mockDecisionCallback), eq(TEST_OPTIONS));
    }

    @Test
    public void testDecideAsync_withoutOptions() {
        OptimizelyUserContextAndroid userContext = new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        );

        userContext.decideAsync(TEST_FLAG_KEY, mockDecisionCallback);

        verify(mockOptimizely).decideAsync(any(OptimizelyUserContext.class), eq(TEST_FLAG_KEY), eq(mockDecisionCallback), eq(Collections.emptyList()));
    }

    @Test
    public void testDecideForKeysAsync_withOptions() {
        OptimizelyUserContextAndroid userContext = new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        );

        userContext.decideForKeysAsync(TEST_FLAG_KEYS, mockDecisionsCallback, TEST_OPTIONS);

        verify(mockOptimizely).decideForKeysAsync(any(OptimizelyUserContext.class), eq(TEST_FLAG_KEYS), eq(mockDecisionsCallback), eq(TEST_OPTIONS));
    }

    @Test
    public void testDecideForKeysAsync_withoutOptions() {
        OptimizelyUserContextAndroid userContext = new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        );

        userContext.decideForKeysAsync(TEST_FLAG_KEYS, mockDecisionsCallback);

        verify(mockOptimizely).decideForKeysAsync(any(OptimizelyUserContext.class), eq(TEST_FLAG_KEYS), eq(mockDecisionsCallback), eq(Collections.emptyList()));
    }

    @Test
    public void testDecideAllAsync_withOptions() {
        OptimizelyUserContextAndroid userContext = new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        );

        userContext.decideAllAsync(mockDecisionsCallback, TEST_OPTIONS);

        verify(mockOptimizely).decideAllAsync(any(OptimizelyUserContext.class), eq(mockDecisionsCallback), eq(TEST_OPTIONS));
    }

    @Test
    public void testDecideAllAsync_withoutOptions() {
        OptimizelyUserContextAndroid userContext = new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        );

        userContext.decideAllAsync(mockDecisionsCallback);

        verify(mockOptimizely).decideAllAsync(any(OptimizelyUserContext.class), eq(mockDecisionsCallback), eq(Collections.emptyList()));
    }

}
