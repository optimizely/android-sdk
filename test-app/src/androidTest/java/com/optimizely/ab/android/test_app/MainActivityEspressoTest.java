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
package com.optimizely.ab.android.test_app;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityEspressoTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);
    private Context context;
    private CountingIdlingResource countingIdlingResource;

    @Before
    public void setupStorage() {
        // Clear sticky bucketing
        context = InstrumentationRegistry.getTargetContext();
        context.deleteFile("optly-user-experiment-record-7664231436.json");
        context.deleteFile("optly-data-file-7664231436.json");
        // Set the user's id to the test user that is in the whitelist.
        context.getSharedPreferences("user", Context.MODE_PRIVATE).edit()
                .putString("userId", "test_user")
                .apply();
    }


    @Before
    public void registerIdlingResource() {
        countingIdlingResource = activityRule.getActivity().getIdlingResource();
        // To prove that the test fails, omit this call:
        Espresso.registerIdlingResources(countingIdlingResource);
    }

    @Test
    public void experimentActivationForWhitelistUser() {
        // Check that the text was changed.
        // These tests are pointed at a real project.
        // The user 'test_user` is in the whitelist for variation 1 for experiment 0 and experiment 1.
        onView(withId(R.id.button_1))
                .check(matches(withText(context.getString(R.string.button_1_text_var_1))));


        // Espresso will wait for Optimizely to start due to the registered idling resources
        onView(withId(R.id.text_view_1))
                .check(matches(withText(context.getString(R.string.text_view_1_var_1))));
    }

    @After
    public void unregisterIdlingResource() {
        if (countingIdlingResource != null) {
            Espresso.unregisterIdlingResources(countingIdlingResource);
        }
    }
}
