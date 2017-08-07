/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
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

import android.app.AlarmManager;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


/**
 * Tests for {@link ServiceScheduler}
 */
@RunWith(AndroidJUnit4.class)
public class ServiceSchedulerTest {

    private Context context;

    @Before
    public void setup() {
        context = getTargetContext();
    }

    @Test
    public void testScheduler() {

        class MyIntent extends IntentService {

            public MyIntent() {
                super("MyItentServiceTest");

            }

            @Override
            protected void onHandleIntent(@Nullable Intent intent) {

            }
        }


        AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler
                .PendingIntentFactory(context.getApplicationContext());
        ServiceScheduler serviceScheduler = new ServiceScheduler(alarmManager, pendingIntentFactory,
                LoggerFactory.getLogger(ServiceScheduler.class));


        Intent intent = new Intent(context, MyIntent.class);

        serviceScheduler.schedule(intent, 30L);
        assertTrue(serviceScheduler.isScheduled(intent));

        serviceScheduler.unschedule(intent);

        assertFalse(serviceScheduler.isScheduled(intent));
    }
}
