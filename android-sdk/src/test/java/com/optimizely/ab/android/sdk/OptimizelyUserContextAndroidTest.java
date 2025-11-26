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
import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;
import com.optimizely.ab.optimizelydecision.OptimizelyDecision;
import com.optimizely.ab.optimizelydecision.OptimizelyDecisionCallback;
import com.optimizely.ab.optimizelydecision.OptimizelyDecisionsCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OptimizelyUserContextAndroid}
 */
@RunWith(MockitoJUnitRunner.class)
public class OptimizelyUserContextAndroidTest {

    @Mock
    Optimizely mockOptimizely;

    @Mock
    OptimizelyDecision mockDecision;

    @Mock
    Map<String, OptimizelyDecision> mockDecisionsMap;

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

    // ===========================================
    // Tests for Sync Decide Methods
    // ===========================================

    @Test
    public void testDecide_withOptions() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));
        doReturn(mockDecision).when(userContext).coreDecideSync(any(), any());

        OptimizelyDecision result = userContext.decide(TEST_FLAG_KEY, TEST_OPTIONS);

        verify(userContext).coreDecideSync(TEST_FLAG_KEY, TEST_OPTIONS);
        assertEquals(mockDecision, result);
    }

    @Test
    public void testDecide_withoutOptions() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));
        doReturn(mockDecision).when(userContext).coreDecideSync(any(), any());

        OptimizelyDecision result = userContext.decide(TEST_FLAG_KEY);

        verify(userContext).coreDecideSync(TEST_FLAG_KEY, Collections.emptyList());
        assertEquals(mockDecision, result);
    }

    @Test
    public void testDecideForKeys_withOptions() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));
        doReturn(mockDecisionsMap).when(userContext).coreDecideForKeysSync(any(), any());

        Map<String, OptimizelyDecision> result = userContext.decideForKeys(TEST_FLAG_KEYS, TEST_OPTIONS);

        verify(userContext).coreDecideForKeysSync(TEST_FLAG_KEYS, TEST_OPTIONS);
        assertEquals(mockDecisionsMap, result);
    }

    @Test
    public void testDecideForKeys_withoutOptions() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));
        doReturn(mockDecisionsMap).when(userContext).coreDecideForKeysSync(any(), any());

        Map<String, OptimizelyDecision> result = userContext.decideForKeys(TEST_FLAG_KEYS);

        verify(userContext).coreDecideForKeysSync(TEST_FLAG_KEYS, Collections.emptyList());
        assertEquals(mockDecisionsMap, result);
    }

    @Test
    public void testDecideAll_withOptions() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));
        doReturn(mockDecisionsMap).when(userContext).coreDecideAllSync(any());

        Map<String, OptimizelyDecision> result = userContext.decideAll(TEST_OPTIONS);

        verify(userContext).coreDecideAllSync(TEST_OPTIONS);
        assertEquals(mockDecisionsMap, result);
    }

    @Test
    public void testDecideAll_withoutOptions() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));
        doReturn(mockDecisionsMap).when(userContext).coreDecideAllSync(any());

        Map<String, OptimizelyDecision> result = userContext.decideAll();

        verify(userContext).coreDecideAllSync(Collections.emptyList());
        assertEquals(mockDecisionsMap, result);
    }

    // ===========================================
    // Tests for Async Decide Methods (with callbacks)
    // ===========================================

    @Test
    public void testDecideAsync_withCallbackAndOptions() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));

        userContext.decideAsync(TEST_FLAG_KEY, TEST_OPTIONS, mockDecisionCallback);

        verify(userContext).coreDecideAsync(TEST_FLAG_KEY, TEST_OPTIONS, mockDecisionCallback);
    }

    @Test
    public void testDecideAsync_withCallbackOnly() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));

        userContext.decideAsync(TEST_FLAG_KEY, mockDecisionCallback);

        verify(userContext).coreDecideAsync(TEST_FLAG_KEY, Collections.emptyList(), mockDecisionCallback);
    }

    @Test
    public void testDecideForKeysAsync_withCallbackAndOptions() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));

        userContext.decideForKeysAsync(TEST_FLAG_KEYS, TEST_OPTIONS, mockDecisionsCallback);

        verify(userContext).coreDecideForKeysAsync(TEST_FLAG_KEYS, TEST_OPTIONS, mockDecisionsCallback);
    }

    @Test
    public void testDecideForKeysAsync_withCallbackOnly() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));

        userContext.decideForKeysAsync(TEST_FLAG_KEYS, mockDecisionsCallback);

        verify(userContext).coreDecideForKeysAsync(TEST_FLAG_KEYS, Collections.emptyList(), mockDecisionsCallback);
    }

    @Test
    public void testDecideAllAsync_withCallbackAndOptions() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));

        userContext.decideAllAsync(TEST_OPTIONS, mockDecisionsCallback);

        verify(userContext).coreDecideAllAsync(TEST_OPTIONS, mockDecisionsCallback);
    }

    @Test
    public void testDecideAllAsync_withCallbackOnly() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));

        userContext.decideAllAsync(mockDecisionsCallback);

        verify(userContext).coreDecideAllAsync(Collections.emptyList(), mockDecisionsCallback);
    }

    // ===========================================
    // Tests for Blocking Async Decide Methods
    // ===========================================

    @Test
    public void testDecideAsync_blocking_withOptions() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));

        doReturn(mockDecision).when(userContext).coreDecide(any(), any());

        OptimizelyDecision result = userContext.decideAsync(TEST_FLAG_KEY, TEST_OPTIONS);

        verify(userContext).coreDecide(TEST_FLAG_KEY, TEST_OPTIONS);
        assertEquals(mockDecision, result);
    }

    @Test
    public void testDecideAsync_blocking_withoutOptions() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));

        doReturn(mockDecision).when(userContext).coreDecide(any(), any());

        OptimizelyDecision result = userContext.decideAsync(TEST_FLAG_KEY);

        verify(userContext).coreDecide(TEST_FLAG_KEY, Collections.emptyList());
        assertEquals(mockDecision, result);
    }

    @Test
    public void testDecideForKeysAsync_blocking_withOptions() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));

        doReturn(mockDecisionsMap).when(userContext).coreDecideForKeys(any(), any());

        Map<String, OptimizelyDecision> result = userContext.decideForKeysAsync(TEST_FLAG_KEYS, TEST_OPTIONS);

        verify(userContext).coreDecideForKeys(TEST_FLAG_KEYS, TEST_OPTIONS);
        assertEquals(mockDecisionsMap, result);
    }

    @Test
    public void testDecideForKeysAsync_blocking_withoutOptions() throws Exception {
        // Create spy of the actual class
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));

        doReturn(mockDecisionsMap).when(userContext).coreDecideForKeys(any(), any());

        Map<String, OptimizelyDecision> result = userContext.decideForKeysAsync(TEST_FLAG_KEYS);

        verify(userContext).coreDecideForKeys(TEST_FLAG_KEYS, Collections.emptyList());
        assertEquals(mockDecisionsMap, result);
    }

    @Test
    public void testDecideAllAsync_blocking_withOptions() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));

        doReturn(mockDecisionsMap).when(userContext).coreDecideAll(any());

        Map<String, OptimizelyDecision> result = userContext.decideAllAsync(TEST_OPTIONS);

        verify(userContext).coreDecideAll(TEST_OPTIONS);
        assertEquals(mockDecisionsMap, result);
    }

    @Test
    public void testDecideAllAsync_blocking_withoutOptions() throws Exception {
        OptimizelyUserContextAndroid userContext = spy(new OptimizelyUserContextAndroid(
            mockOptimizely,
            TEST_USER_ID,
            testAttributes
        ));

        doReturn(mockDecisionsMap).when(userContext).coreDecideAll(any());

        Map<String, OptimizelyDecision> result = userContext.decideAllAsync();

        verify(userContext).coreDecideAll(Collections.emptyList());
        assertEquals(mockDecisionsMap, result);
    }

}
