/**
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
package com.optimizely.ab.android.event_handler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.android.shared.OptlyStorage;
import com.optimizely.ab.android.shared.ServiceScheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 7/27/16 for Optimizely.
 *
 * Tests for {@link ServiceScheduler}
 */
@RunWith(AndroidJUnit4.class)
public class ServiceSchedulerTest {

    OptlyStorage optlyStorage;
    ServiceScheduler.PendingIntentFactory pendingIntentFactory;
    AlarmManager alarmManager;
    Logger logger;
    ServiceScheduler serviceScheduler;
    Context context;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getTargetContext();
        optlyStorage = mock(OptlyStorage.class);
        alarmManager = mock(AlarmManager.class);
        pendingIntentFactory = mock(ServiceScheduler.PendingIntentFactory.class);
        logger = mock(Logger.class);
        serviceScheduler = new ServiceScheduler(alarmManager, pendingIntentFactory, logger);
    }

    @Test
    public void testScheduleWithNoDurationExtra() {
        final Intent intent = new Intent(context, EventIntentService.class);
        when(pendingIntentFactory.hasPendingIntent(intent)).thenReturn(false);
        PendingIntent pendingIntent = getPendingIntent();
        when(pendingIntentFactory.getPendingIntent(intent)).thenReturn(pendingIntent);
        when(optlyStorage.getLong(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR)).thenReturn(AlarmManager.INTERVAL_HOUR);

        serviceScheduler.schedule(intent, AlarmManager.INTERVAL_HOUR);

        verify(alarmManager).setInexactRepeating(AlarmManager.ELAPSED_REALTIME, AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_HOUR, pendingIntent);
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

        verify(alarmManager).setInexactRepeating(AlarmManager.ELAPSED_REALTIME, duration, duration, pendingIntent);
        verify(logger).info("Scheduled {}", intent.getComponent().toShortString());
        pendingIntent.cancel();
    }

    @Test
    public void testAlreadyScheduledAlarm() {
        final Intent intent = new Intent(context, EventIntentService.class);
        when(pendingIntentFactory.hasPendingIntent(intent)).thenReturn(true);

        serviceScheduler.schedule(intent, AlarmManager.INTERVAL_HOUR);

        verify(logger).debug("Not scheduling {}. It's already scheduled", intent.getComponent().toShortString());
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
        verify(alarmManager).cancel(pendingIntent);
        verify(logger).info("Unscheduled {}", intent.getComponent().toShortString());
    }


    // Mockito can't mock PendingIntent because it's final
    PendingIntent getPendingIntent() {
        final Context context = InstrumentationRegistry.getTargetContext();
        return PendingIntent.getService(context, 0, new Intent(context, EventIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
