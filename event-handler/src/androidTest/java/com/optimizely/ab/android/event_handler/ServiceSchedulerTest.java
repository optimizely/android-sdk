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

package com.optimizely.ab.android.event_handler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobWorkItem;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.android.shared.JobWorkService;
import com.optimizely.ab.android.shared.OptlyStorage;
import com.optimizely.ab.android.shared.ServiceScheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ServiceScheduler}
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
public class ServiceSchedulerTest {

    private OptlyStorage optlyStorage;
    private ServiceScheduler.PendingIntentFactory pendingIntentFactory;
    private AlarmManager alarmManager;
    private JobScheduler jobScheduler;
    private Logger logger;
    private ServiceScheduler serviceScheduler;
    private Context context;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Before
    public void setup() {
        context      = mock(Context.class);
        optlyStorage = mock(OptlyStorage.class);
        alarmManager = mock(AlarmManager.class);
        jobScheduler = mock(JobScheduler.class);

        when(context.getApplicationContext()).thenReturn(context);

        when(context.getSystemService(Context.ALARM_SERVICE)).thenReturn(alarmManager);
        when(context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).thenReturn(jobScheduler);

        pendingIntentFactory = mock(ServiceScheduler.PendingIntentFactory.class);
        logger = mock(Logger.class);
        serviceScheduler = new ServiceScheduler(context, pendingIntentFactory, logger);
    }

    @Test
    public void testScheduleWithNoDurationExtra() {
        final Intent intent = new Intent(context, EventIntentService.class);
        when(pendingIntentFactory.hasPendingIntent(intent)).thenReturn(false);
        PendingIntent pendingIntent = getPendingIntent();
        when(pendingIntentFactory.getPendingIntent(intent)).thenReturn(pendingIntent);
        when(optlyStorage.getLong(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR)).thenReturn(AlarmManager.INTERVAL_HOUR);

        serviceScheduler.schedule(intent, AlarmManager.INTERVAL_HOUR);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ArgumentCaptor<JobInfo> jobInfoArgumentCaptor = ArgumentCaptor.forClass(JobInfo.class);

            verify(jobScheduler).schedule(jobInfoArgumentCaptor.capture());

            assertEquals(jobInfoArgumentCaptor.getValue().getIntervalMillis(), AlarmManager.INTERVAL_HOUR );

        }
        else {
            verify(alarmManager).setInexactRepeating(AlarmManager.ELAPSED_REALTIME, AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_HOUR, pendingIntent);
        }
        verify(logger).info("Scheduled {}", intent.getComponent().toShortString());
        pendingIntent.cancel();
    }

    @Test
    public void invalidDuration() {
        serviceScheduler.schedule(new Intent(context, EventIntentService.class), 0);
        verify(logger).error("Tried to schedule an interval less than 1");
    }

    @Test
    public void testScheduleWithDurationExtra() {
        final Intent intent = new Intent(context, EventIntentService.class);
        when(pendingIntentFactory.hasPendingIntent(intent)).thenReturn(false);
        PendingIntent pendingIntent = getPendingIntent();
        when(pendingIntentFactory.getPendingIntent(intent)).thenReturn(pendingIntent);
        when(optlyStorage.getLong(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR)).thenReturn(AlarmManager.INTERVAL_HOUR);

        long duration = AlarmManager.INTERVAL_DAY;
        intent.putExtra(EventIntentService.EXTRA_INTERVAL, duration);
        serviceScheduler.schedule(intent, duration);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ArgumentCaptor<JobInfo> jobInfoArgumentCaptor = ArgumentCaptor.forClass(JobInfo.class);

            verify(jobScheduler).schedule(jobInfoArgumentCaptor.capture());

            assertEquals(jobInfoArgumentCaptor.getValue().getIntervalMillis(), duration );
        }
        else {
            verify(alarmManager).setInexactRepeating(AlarmManager.ELAPSED_REALTIME, duration, duration, pendingIntent);
        }

        verify(logger).info("Scheduled {}", intent.getComponent().toShortString());
        pendingIntent.cancel();
    }

    @Test
    public void testAlreadyScheduledAlarm() {
        final Intent intent = new Intent(context, EventIntentService.class);
        when(pendingIntentFactory.hasPendingIntent(intent)).thenReturn(true);
        when(pendingIntentFactory.getPendingIntent(intent)).thenReturn(getPendingIntent());

        serviceScheduler.schedule(intent, AlarmManager.INTERVAL_HOUR);

        verify(logger).info("Unscheduled {}", intent.getComponent().toShortString());
    }

    @Test
    public void arePendingIntentsForTheSameComponentEqual() {
        Context context = InstrumentationRegistry.getTargetContext();
        final Intent intent = new Intent(context, EventIntentService.class);
        PendingIntent pendingIntent1 = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingIntent2 = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        assertEquals(pendingIntent1, pendingIntent2);
        pendingIntent1.cancel();
        pendingIntent2.cancel();
    }

    @Test
    public void howDoesFlagNoCreateWorks() {
        Context context = InstrumentationRegistry.getTargetContext();
        final Intent intent = new Intent(context, EventIntentService.class);
        PendingIntent pendingIntent1 = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
        assertNull(pendingIntent1);
        PendingIntent pendingIntent2 = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingIntent3 = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
        assertEquals(pendingIntent2, pendingIntent3);
        pendingIntent2.cancel();
        pendingIntent3.cancel();
    }

    @Test
    public void hasPendingIntent() {
        Context context = InstrumentationRegistry.getTargetContext();
        ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler.PendingIntentFactory(context);
        final Intent intent = new Intent(context, EventIntentService.class);
        assertFalse(pendingIntentFactory.hasPendingIntent(intent));
        PendingIntent pendingIntent1 = pendingIntentFactory.getPendingIntent(intent);
        assertTrue(pendingIntentFactory.hasPendingIntent(intent));
        PendingIntent pendingIntent2 = pendingIntentFactory.getPendingIntent(intent);
        assertEquals(pendingIntent1, pendingIntent2);
    }

    @Test
    public void testCancel() {
        PendingIntent pendingIntent = getPendingIntent();
        final Intent intent = new Intent(context, EventIntentService.class);
        when(pendingIntentFactory.getPendingIntent(intent)).thenReturn(pendingIntent);
        serviceScheduler.unschedule(intent);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            verify(jobScheduler).getAllPendingJobs();
        }
        else {
            verify(alarmManager).cancel(pendingIntent);
        }
        verify(logger).info("Unscheduled {}", intent.getComponent().toShortString());
    }


    // Mockito can't mock PendingIntent because it's final
    private PendingIntent getPendingIntent() {
        final Context context = InstrumentationRegistry.getTargetContext();
        return PendingIntent.getService(context, 0, new Intent(context, EventIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
