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

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import com.optimizely.ab.android.event_handler.EventIntentService;
import com.optimizely.ab.android.sdk.DataFileService;
import com.optimizely.ab.android.shared.CountingIdlingResourceManager;
import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.ab.bucketing.UserExperimentRecord;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityEspressoTest {

    private Context context = InstrumentationRegistry.getTargetContext();
    private CountingIdlingResource countingIdlingResource;
    private ServiceScheduler serviceScheduler;
    private Intent dataFileServiceIntent, eventIntentService;

    private ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class);
    @Rule public TestRule chain = RuleChain
            .outerRule(new ExternalResource() {
                @Override
                protected void before() throws Throwable {
                    super.before();
                    countingIdlingResource = CountingIdlingResourceManager.getIdlingResource();
                    // To prove that the test fails, omit this call:
                    Espresso.registerIdlingResources(countingIdlingResource);
                }

                @Override
                protected void after() {
                    super.after();
                    Espresso.unregisterIdlingResources(countingIdlingResource);
                }
            })
            .around(new ExternalResource() {
                @Override
                protected void before() throws Throwable {
                    super.before();
                    // Set the user's id to the test user that is in the whitelist.
                    context.getSharedPreferences("user", Context.MODE_PRIVATE).edit()
                            .putString("userId", "test_user")
                            .apply();
                }

                @Override
                protected void after() {
                    super.after();
                    context.getSharedPreferences("user", Context.MODE_PRIVATE).edit().apply();
                    // Clear sticky bucketing
                    context.deleteFile(String.format("optly-user-experiment-record-%s.json", MyApplication.PROJECT_ID));
                }
            })
            .around(new ExternalResource() {
                @Override
                protected void before() throws Throwable {
                    super.before();

                    dataFileServiceIntent = new Intent(context, DataFileService.class);
                    dataFileServiceIntent.putExtra(DataFileService.EXTRA_PROJECT_ID, MyApplication.PROJECT_ID);

                    eventIntentService = new Intent(context, EventIntentService.class);
                    eventIntentService.putExtra(DataFileService.EXTRA_PROJECT_ID, MyApplication.PROJECT_ID);

                    Context applicationContext = context.getApplicationContext();
                    ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler.PendingIntentFactory(applicationContext);
                    AlarmManager alarmManager = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);
                    serviceScheduler = new ServiceScheduler(alarmManager, pendingIntentFactory, LoggerFactory.getLogger(ServiceScheduler.class));
                }

                @Override
                protected void after() {
                    super.after();
                    serviceScheduler.unschedule(dataFileServiceIntent);
                    assertFalse(serviceScheduler.isScheduled(dataFileServiceIntent));
                    assertFalse(serviceScheduler.isScheduled(eventIntentService));
                    CountingIdlingResourceManager.clearEvents();
                }
            })
            .around(activityTestRule);

    @Test
    public void experimentActivationForWhitelistUser() throws InterruptedException {
        // Check that the text was changed.
        // These tests are pointed at a real project.
        // The user 'test_user` is in the whitelist for variation 1 for experiment 0 and experiment 1.
        onView(withId(R.id.button_1))
                .check(matches(withText(context.getString(R.string.button_1_text_var_1))));

        // Espresso will wait for Optimizely to start due to the registered idling resources
        onView(withId(R.id.text_view_1))
                .check(matches(withText(context.getString(R.string.text_view_1_var_1))));

        assertTrue(serviceScheduler.isScheduled(dataFileServiceIntent));

        onView(withId(R.id.button_1))      // withId(R.id.my_view) is a ViewMatcher
                .perform(click());         // click() is a ViewAction

        List<Pair<String, String>> events = CountingIdlingResourceManager.getEvents();
        assertTrue(events.size() == 4);
        Iterator<Pair<String, String>> iterator = events.iterator();
        while (iterator.hasNext()) {
            Pair<String, String> event = iterator.next();
            final String url = event.first;
            final String payload = event.second;
            if (url.equals("https://p13nlog.dz.optimizely.com/log/decision") && payload.contains("7676481120") && payload.contains("7661891902")
                    || url.equals("https://p13nlog.dz.optimizely.com/log/decision") && payload.contains("7651112186") && payload.contains("7674261140")
                    || url.equals("https://p13nlog.dz.optimizely.com/log/event") && payload.contains("experiment_0")
                    || url.equals("https://p13nlog.dz.optimizely.com/log/event") && payload.contains("experiment_1")) {
                iterator.remove();
            }
        }
        assertTrue(events.isEmpty());
        MyApplication myApplication = (MyApplication) activityTestRule.getActivity().getApplication();
        UserExperimentRecord userExperimentRecord = myApplication.getOptimizelyManager().getUserExperimentRecord();
        // Being in the white list should override user experiment record
        assertNull(userExperimentRecord.lookup("test_user", "experiment_0"));
        assertNull(userExperimentRecord.lookup("test_user", "experiment_1"));
    }
}
