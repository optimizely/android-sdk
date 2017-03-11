/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.config.audience;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the evaluation of different audience condition types (And, Or, Not, and UserAttribute)
 */
@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
    justification = "mockito verify calls do have a side-effect")
public class AudienceConditionEvaluationTest {

    Map<String, String> testUserAttributes;

    @Before
    public void initialize() {
        testUserAttributes = new HashMap<String, String>();
        testUserAttributes.put("browser_type", "chrome");
        testUserAttributes.put("device_type", "Android");
    }

    /**
     * Verify that UserAttribute.evaluate returns true on exact-matching visitor attribute data.
     */
    @Test
    public void userAttributeEvaluateTrue() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_dimension", "chrome");
        assertTrue(testInstance.evaluate(testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate returns false on non-exact-matching visitor attribute data.
     */
    @Test
    public void userAttributeEvaluateFalse() throws Exception {
        UserAttribute testInstance = new UserAttribute("browser_type", "custom_dimension", "firefox");
        assertFalse(testInstance.evaluate(testUserAttributes));
    }

    /**
     * Verify that UserAttribute.evaluate returns false on unknown visitor attributes.
     */
    @Test
    public void userAttributeUnknownAttribute() throws Exception {
        UserAttribute testInstance = new UserAttribute("unknown_dim", "custom_dimension", "unknown");
        assertFalse(testInstance.evaluate(testUserAttributes));
    }

    /**
     * Verify that NotCondition.evaluate returns true when its condition operand evaluates to false.
     */
    @Test
    public void notConditionEvaluateTrue() throws Exception {
        UserAttribute userAttribute = mock(UserAttribute.class);
        when(userAttribute.evaluate(testUserAttributes)).thenReturn(false);

        NotCondition notCondition = new NotCondition(userAttribute);
        assertTrue(notCondition.evaluate(testUserAttributes));
        verify(userAttribute, times(1)).evaluate(testUserAttributes);
    }

    /**
     * Verify that NotCondition.evaluate returns false when its condition operand evaluates to true.
     */
    @Test
    public void notConditionEvaluateFalse() throws Exception {
        UserAttribute userAttribute = mock(UserAttribute.class);
        when(userAttribute.evaluate(testUserAttributes)).thenReturn(true);

        NotCondition notCondition = new NotCondition(userAttribute);
        assertFalse(notCondition.evaluate(testUserAttributes));
        verify(userAttribute, times(1)).evaluate(testUserAttributes);
    }

    /**
     * Verify that OrCondition.evaluate returns true when at least one of its operand conditions evaluate to true.
     */
    @Test
    public void orConditionEvaluateTrue() throws Exception {
        UserAttribute userAttribute1 = mock(UserAttribute.class);
        when(userAttribute1.evaluate(testUserAttributes)).thenReturn(true);

        UserAttribute userAttribute2 = mock(UserAttribute.class);
        when(userAttribute2.evaluate(testUserAttributes)).thenReturn(false);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(userAttribute1);
        conditions.add(userAttribute2);

        OrCondition orCondition = new OrCondition(conditions);
        assertTrue(orCondition.evaluate(testUserAttributes));
        verify(userAttribute1, times(1)).evaluate(testUserAttributes);
        // shouldn't be called due to short-circuiting in 'Or' evaluation
        verify(userAttribute2, times(0)).evaluate(testUserAttributes);
    }

    /**
     * Verify that OrCondition.evaluate returns false when all of its operand conditions evaluate to false.
     */
    @Test
    public void orConditionEvaluateFalse() throws Exception {
        UserAttribute userAttribute1 = mock(UserAttribute.class);
        when(userAttribute1.evaluate(testUserAttributes)).thenReturn(false);

        UserAttribute userAttribute2 = mock(UserAttribute.class);
        when(userAttribute2.evaluate(testUserAttributes)).thenReturn(false);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(userAttribute1);
        conditions.add(userAttribute2);

        OrCondition orCondition = new OrCondition(conditions);
        assertFalse(orCondition.evaluate(testUserAttributes));
        verify(userAttribute1, times(1)).evaluate(testUserAttributes);
        verify(userAttribute2, times(1)).evaluate(testUserAttributes);
    }

    /**
     * Verify that AndCondition.evaluate returns true when all of its operand conditions evaluate to true.
     */
    @Test
    public void andConditionEvaluateTrue() throws Exception {
        OrCondition orCondition1 = mock(OrCondition.class);
        when(orCondition1.evaluate(testUserAttributes)).thenReturn(true);

        OrCondition orCondition2 = mock(OrCondition.class);
        when(orCondition2.evaluate(testUserAttributes)).thenReturn(true);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(orCondition1);
        conditions.add(orCondition2);

        AndCondition andCondition = new AndCondition(conditions);
        assertTrue(andCondition.evaluate(testUserAttributes));
        verify(orCondition1, times(1)).evaluate(testUserAttributes);
        verify(orCondition2, times(1)).evaluate(testUserAttributes);
    }

    /**
     * Verify that AndCondition.evaluate returns false when any one of its operand conditions evaluate to false.
     */
    @Test
    public void andConditionEvaluateFalse() throws Exception {
        OrCondition orCondition1 = mock(OrCondition.class);
        when(orCondition1.evaluate(testUserAttributes)).thenReturn(false);

        OrCondition orCondition2 = mock(OrCondition.class);
        when(orCondition2.evaluate(testUserAttributes)).thenReturn(true);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(orCondition1);
        conditions.add(orCondition2);

        AndCondition andCondition = new AndCondition(conditions);
        assertFalse(andCondition.evaluate(testUserAttributes));
        verify(orCondition1, times(1)).evaluate(testUserAttributes);
        // shouldn't be called due to short-circuiting in 'And' evaluation
        verify(orCondition2, times(0)).evaluate(testUserAttributes);
    }
}
