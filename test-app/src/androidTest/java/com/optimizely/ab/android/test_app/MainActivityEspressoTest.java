/****************************************************************************
 * Copyright 2016-2017, Optimizely, Inc. and contributors                   *
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

package com.optimizely.ab.android.test_app;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;
import android.view.WindowManager;

import com.optimizely.ab.android.datafile_handler.DatafileService;
import com.optimizely.ab.android.event_handler.EventIntentService;
import com.optimizely.ab.android.shared.CountingIdlingResourceInterface;
import com.optimizely.ab.android.shared.CountingIdlingResourceManager;
import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.ab.bucketing.UserProfileService;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
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

    private ActivityTestRule<SplashScreenActivity> activityTestRule = new ActivityTestRule<>(SplashScreenActivity.class);
    @Rule public TestRule chain = RuleChain
            .outerRule(new ExternalResource() {
                @Override
                protected void before() throws Throwable {
                    super.before();
                    countingIdlingResource = new CountingIdlingResource("optly", true);
                    CountingIdlingResourceInterface wrapper = new CountingIdlingResourceInterface() {
                        @Override
                        public void increment() {
                            countingIdlingResource.increment();
                        }

                        @Override
                        public void decrement() {
                            countingIdlingResource.decrement();
                        }
                    };
                    CountingIdlingResourceManager.setIdlingResource(wrapper);
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
                    context.deleteFile(String.format("optly-user-profile-%s.json", MyApplication.PROJECT_ID));
                }
            })
            .around(new ExternalResource() {
                @Override
                protected void before() throws Throwable {
                    super.before();

                    dataFileServiceIntent = new Intent(context, DatafileService.class);
                    dataFileServiceIntent.putExtra(DatafileService.EXTRA_PROJECT_ID, MyApplication.PROJECT_ID);

                    eventIntentService = new Intent(context, EventIntentService.class);
                    eventIntentService.putExtra(DatafileService.EXTRA_PROJECT_ID, MyApplication.PROJECT_ID);

                    Context applicationContext = context.getApplicationContext();
                    ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler.PendingIntentFactory(applicationContext);
                    AlarmManager alarmManager = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);
                    serviceScheduler = new ServiceScheduler(applicationContext, pendingIntentFactory, LoggerFactory.getLogger(ServiceScheduler.class));
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
            .around(activityTestRule)
            .around(new ExternalResource() {
                @Override
                protected void before() throws Throwable {
                    super.before();
                    Runnable wakeUpDevice = new Runnable() {
                        public void run() {
                            activityTestRule.getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    };
                    activityTestRule.getActivity().runOnUiThread(wakeUpDevice);
                }

                @Override
                protected void after() {
                    super.after();
                }
            });

    @Test
    public void experimentActivationForWhitelistUser() throws Exception {
        IdlingPolicies.setMasterPolicyTimeout(3, TimeUnit.MINUTES);
        IdlingPolicies.setIdlingResourceTimeout(3, TimeUnit.MINUTES);

        // Check that the text was changed.
        // These tests are pointed at a real project.
        // The user 'test_user` is in the whitelist for variation_a for experiment background_experiment
        onView(withId(R.id.tv_variation_a_text_1))
                .check(matches(isDisplayed()));


        // here i am rescheduling the data file service.  this is because in the splash activity after optimizely startup
        // the app unschedules the data file service.
        serviceScheduler.schedule(dataFileServiceIntent, TimeUnit.DAYS.toMillis(1L));

        // Espresso will wait for Optimizely to start due to the registered idling resources
        assertTrue(serviceScheduler.isScheduled(dataFileServiceIntent));

        onView(withId(R.id.btn_variation_conversion)) // withId(R.id.my_view) is a ViewMatcher
                .perform(click()); // click() is a ViewAction

        List<Pair<String, String>> events = CountingIdlingResourceManager.getEvents();
        assertTrue(events.size() == 2);
        Iterator<Pair<String, String>> iterator = events.iterator();
        while (iterator.hasNext()) {
            Pair<String, String> event = iterator.next();
            final String url = event.first;
            final String payload = event.second;
            if (url.equals("https://logx.optimizely.com/log/decision") && payload.contains("8126664113") && payload.contains("8146590584")
                    || url.equals("https://logx.optimizely.com/log/event") && payload.contains("sample_conversion")) {
                iterator.remove();
            }
        }
        assertTrue(events.isEmpty());
        MyApplication myApplication = (MyApplication) activityTestRule.getActivity().getApplication();
        UserProfileService userProfileService = myApplication.getOptimizelyManager().getUserProfileService();
        // Being in the white list should override user profile
        Map<String, Object> userProfileMap = userProfileService.lookup("test_user");
        assertNull(userProfileMap);
    }
}
