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

package com.optimizely.ab.android.shared;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;


/**
 * Tests for {@link ServiceScheduler}
 */
@RunWith(AndroidJUnit4.class)
public class ServiceSchedulerTest {

    private Context context;

    public static class MyIntent extends IntentService {

        // if you want to schedule an intent for Android O or greater, the intent has to have the public
        // job id or it will not be scheduled.
        public static final Integer JOB_ID = 2112;

        public MyIntent() {
            super("MyItentServiceTest");

        }

        @Override
        protected void onHandleIntent(@Nullable Intent intent) {

        }
    }

    @Before
    public void setup() {
        context = getInstrumentation().getTargetContext();
    }

    @Test
    public void testScheduler() {

        ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler
                .PendingIntentFactory(context.getApplicationContext());
        ServiceScheduler serviceScheduler = new ServiceScheduler(context, pendingIntentFactory,
                LoggerFactory.getLogger(ServiceScheduler.class));


        Intent intent = new Intent(context, MyIntent.class);

        serviceScheduler.schedule(intent, 30L);
        assertTrue(serviceScheduler.isScheduled(intent));

        serviceScheduler.unschedule(intent);

        assertFalse(serviceScheduler.isScheduled(intent));
    }
}
