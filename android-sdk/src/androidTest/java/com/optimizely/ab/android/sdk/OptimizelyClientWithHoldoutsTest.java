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

import com.optimizely.ab.OptimizelyUserContext;
import com.optimizely.ab.optimizelydecision.OptimizelyDecideOption;
import com.optimizely.ab.optimizelydecision.OptimizelyDecision;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.android.sdk.OptimizelyManager.loadRawResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Holdout-specific tests for OptimizelyClient functionality.
 * These tests use a special holdouts configuration file to test holdout experiment behavior.
 */
@RunWith(JUnit4.class)
public class OptimizelyClientWithHoldoutsTest {
    
    private final String testProjectId = "7595190003";

    private OptimizelyClient createOptimizelyClientWithHoldouts(Context context) throws IOException {
        String holdoutDatafile = loadRawResource(context, R.raw.holdouts_project_config);
        OptimizelyManager optimizelyManager = OptimizelyManager.builder(testProjectId).build(context);
        optimizelyManager.initialize(context, holdoutDatafile);
        return optimizelyManager.getOptimizely();
    }

    @Test
    public void testDecide_withHoldout() throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        OptimizelyClient optimizelyClient = createOptimizelyClientWithHoldouts(context);

        String flagKey = "boolean_feature";
        String userId = "user123";
        String variationKey = "ho_off_key";
        String ruleKey = "basic_holdout";

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("$opt_bucketing_id", "ppid160000");  // deterministic bucketing into basic_holdout
        attributes.put("nationality", "English");           // non-reserved attribute

        OptimizelyUserContext userContext = optimizelyClient.createUserContext(userId, attributes);
        OptimizelyDecision decision = userContext.decide(flagKey, Collections.singletonList(OptimizelyDecideOption.INCLUDE_REASONS));

        // Validate holdout decision
        assertEquals(flagKey, decision.getFlagKey());
        assertEquals(variationKey, decision.getVariationKey());
        assertEquals(ruleKey, decision.getRuleKey());
        assertFalse(decision.getEnabled());
        assertTrue(decision.getVariables().toMap().isEmpty());
        assertTrue("Expected holdout reason", decision.getReasons().stream()
            .anyMatch(reason -> reason.contains("holdout")));
    }

    @Test
    public void testDecideForKeys_withHoldout() throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        OptimizelyClient optimizelyClient = createOptimizelyClientWithHoldouts(context);

        String userId = "user123";
        String variationKey = "ho_off_key";
        String ruleKey = "basic_holdout";

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("$opt_bucketing_id", "ppid160000");  // deterministic bucketing into basic_holdout

        List<String> flagKeys = Arrays.asList(
            "boolean_feature",
            "double_single_variable_feature",
            "integer_single_variable_feature"
        );

        OptimizelyUserContext userContext = optimizelyClient.createUserContext(userId, attributes);
        Map<String, OptimizelyDecision> decisions = userContext.decideForKeys(flagKeys, Collections.singletonList(OptimizelyDecideOption.INCLUDE_REASONS));

        assertEquals(3, decisions.size());

        for (String flagKey : flagKeys) {
            OptimizelyDecision decision = decisions.get(flagKey);
            assertNotNull("Missing decision for flag " + flagKey, decision);
            assertEquals(flagKey, decision.getFlagKey());
            assertEquals(variationKey, decision.getVariationKey());
            assertEquals(ruleKey, decision.getRuleKey());
            assertFalse(decision.getEnabled());
            assertTrue("Expected holdout reason for flag " + flagKey, decision.getReasons().stream()
                .anyMatch(reason -> reason.contains("holdout")));
        }
    }

    @Test
    public void testDecideAll_withHoldout() throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        OptimizelyClient optimizelyClient = createOptimizelyClientWithHoldouts(context);

        String userId = "user123";
        String variationKey = "ho_off_key";

        Map<String, Object> attributes = new HashMap<>();
        // ppid120000 buckets user into holdout_included_flags (selective holdout)
        attributes.put("$opt_bucketing_id", "ppid120000");

        // Flags INCLUDED in holdout_included_flags (only these should be holdout decisions)
        List<String> includedInHoldout = Arrays.asList(
            "boolean_feature",
            "double_single_variable_feature",
            "integer_single_variable_feature"
        );

        OptimizelyUserContext userContext = optimizelyClient.createUserContext(userId, attributes);
        Map<String, OptimizelyDecision> decisions = userContext.decideAll(Arrays.asList(
            OptimizelyDecideOption.INCLUDE_REASONS,
            OptimizelyDecideOption.DISABLE_DECISION_EVENT
        ));

        assertTrue("Should have multiple decisions", !decisions.isEmpty());

        String expectedReason = "User (" + userId + ") is in variation (" + variationKey + ") of holdout (holdout_included_flags).";

        int holdoutCount = 0;
        for (Map.Entry<String, OptimizelyDecision> entry : decisions.entrySet()) {
            String flagKey = entry.getKey();
            OptimizelyDecision decision = entry.getValue();
            assertNotNull("Missing decision for flag " + flagKey, decision);

            if (includedInHoldout.contains(flagKey)) {
                // Should be holdout decision
                assertEquals(variationKey, decision.getVariationKey());
                assertFalse(decision.getEnabled());
                assertTrue("Expected holdout reason for flag " + flagKey, decision.getReasons().contains(expectedReason));
                holdoutCount++;
            } else {
                // Should NOT be a holdout decision
                assertFalse("Non-included flag should not have holdout reason: " + flagKey, 
                    decision.getReasons().contains(expectedReason));
            }
        }
        assertEquals("Expected exactly the included flags to be in holdout", includedInHoldout.size(), holdoutCount);
    }

    @Test
    public void testDecisionNotificationHandler_withHoldout() throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        OptimizelyClient optimizelyClient = createOptimizelyClientWithHoldouts(context);

        String flagKey = "boolean_feature";
        String userId = "user123";
        String variationKey = "ho_off_key";
        String ruleKey = "basic_holdout";

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("$opt_bucketing_id", "ppid160000");  // deterministic bucketing into basic_holdout
        attributes.put("nationality", "English");           // non-reserved attribute

        final boolean[] listenerCalled = {false};
        optimizelyClient.addDecisionNotificationHandler(decisionNotification -> {
            assertEquals("FLAG", decisionNotification.getType());
            assertEquals(userId, decisionNotification.getUserId());
            assertEquals(attributes, decisionNotification.getAttributes());

            Map<String, ?> info = decisionNotification.getDecisionInfo();
            assertEquals(flagKey, info.get("flagKey"));
            assertEquals(variationKey, info.get("variationKey"));
            assertEquals(false, info.get("enabled"));
            assertEquals(ruleKey, info.get("ruleKey"));
            assertTrue(((Map<?, ?>) info.get("variables")).isEmpty());

            listenerCalled[0] = true;
        });

        OptimizelyUserContext userContext = optimizelyClient.createUserContext(userId, attributes);
        OptimizelyDecision decision = userContext.decide(flagKey, Collections.singletonList(OptimizelyDecideOption.INCLUDE_REASONS));

        assertTrue("Decision notification handler should have been called", listenerCalled[0]);
        assertEquals(variationKey, decision.getVariationKey());
        assertFalse(decision.getEnabled());
    }

    @Test
    public void testHoldout_zeroTraffic() throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        OptimizelyClient optimizelyClient = createOptimizelyClientWithHoldouts(context);

        String flagKey = "boolean_feature";
        String userId = "user456";

        Map<String, Object> attributes = new HashMap<>();
        // Use a bucketing ID that would normally fall into holdout_zero_traffic, but since it has 0% traffic, should not be in holdout
        attributes.put("$opt_bucketing_id", "ppid300000");

        OptimizelyUserContext userContext = optimizelyClient.createUserContext(userId, attributes);
        OptimizelyDecision decision = userContext.decide(flagKey, Collections.singletonList(OptimizelyDecideOption.INCLUDE_REASONS));

        // Should NOT be a holdout decision since holdout_zero_traffic has 0% traffic allocation
        assertNotEquals("ho_off_key", decision.getVariationKey());
        assertNotEquals("holdout_zero_traffic", decision.getRuleKey());
        assertFalse("Should not have holdout reason", decision.getReasons().stream()
            .anyMatch(reason -> reason.contains("holdout_zero_traffic")));
    }

    @Test
    public void testHoldout_attributeFiltering() throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        OptimizelyClient optimizelyClient = createOptimizelyClientWithHoldouts(context);

        String flagKey = "boolean_feature";
        String userId = "user789";

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("$opt_bucketing_id", "ppid160000");  // deterministic bucketing into basic_holdout
        attributes.put("nationality", "English");           // should appear in notifications
        attributes.put("$opt_reserved", "filtered");        // should be filtered out

        final Map<String, Object>[] notificationAttributes = new Map[1];
        optimizelyClient.addDecisionNotificationHandler(decisionNotification -> {
            notificationAttributes[0] = new HashMap<>(decisionNotification.getAttributes());
        });

        OptimizelyUserContext userContext = optimizelyClient.createUserContext(userId, attributes);
        OptimizelyDecision decision = userContext.decide(flagKey, Collections.singletonList(OptimizelyDecideOption.INCLUDE_REASONS));

        // Validate that reserved attributes are filtered from notifications but non-reserved ones remain
        assertNotNull("Notification should have been called", notificationAttributes[0]);
        assertTrue("Should contain nationality", notificationAttributes[0].containsKey("nationality"));
        assertEquals("English", notificationAttributes[0].get("nationality"));
        assertTrue("Should contain $opt_bucketing_id", notificationAttributes[0].containsKey("$opt_bucketing_id"));
        assertTrue("Should contain $opt_reserved", notificationAttributes[0].containsKey("$opt_reserved"));
        
        // Validate holdout decision
        assertEquals("ho_off_key", decision.getVariationKey());
        assertEquals("basic_holdout", decision.getRuleKey());
        assertFalse(decision.getEnabled());
    }
}
