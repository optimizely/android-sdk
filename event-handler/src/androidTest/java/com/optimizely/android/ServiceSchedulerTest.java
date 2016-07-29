package com.optimizely.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

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
        when(pendingIntentFactory.hasPendingIntent(new Intent(context, EventIntentService.class))).thenReturn(false);
        PendingIntent pendingIntent = getPendingIntent();
        when(pendingIntentFactory.getPendingIntent(new Intent(context, EventIntentService.class))).thenReturn(pendingIntent);
        when(optlyStorage.getLong(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR)).thenReturn(AlarmManager.INTERVAL_HOUR);

        serviceScheduler.schedule(new Intent(context, EventIntentService.class), AlarmManager.INTERVAL_HOUR);

        verify(alarmManager).setInexactRepeating(AlarmManager.ELAPSED_REALTIME, AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_HOUR, pendingIntent);
        verify(logger).info("Scheduled {}", EventIntentService.class.getSimpleName());
        pendingIntent.cancel();
    }

    @Test
    public void invalidDuration() {
        serviceScheduler.schedule(new Intent(context, EventIntentService.class), 0);
        verify(logger).error("Tried to schedule an interval less than 1");
    }

    @Test
    public void testScheduleWithDurationExtra() {
        when(pendingIntentFactory.hasPendingIntent(new Intent(context, EventIntentService.class))).thenReturn(false);
        PendingIntent pendingIntent = getPendingIntent();
        when(pendingIntentFactory.getPendingIntent(new Intent(context, EventIntentService.class))).thenReturn(pendingIntent);
        when(optlyStorage.getLong(EventIntentService.EXTRA_INTERVAL, AlarmManager.INTERVAL_HOUR)).thenReturn(AlarmManager.INTERVAL_HOUR);

        long duration = AlarmManager.INTERVAL_DAY;
        Context context = InstrumentationRegistry.getTargetContext();
        Intent intent = new Intent(context, EventIntentService.class);
        intent.putExtra(EventIntentService.EXTRA_INTERVAL, duration);
        serviceScheduler.schedule(new Intent(context, EventIntentService.class), duration);

        verify(alarmManager).setInexactRepeating(AlarmManager.ELAPSED_REALTIME, duration, duration, pendingIntent);
        verify(logger).info("Scheduled {}", EventIntentService.class.getSimpleName());
        pendingIntent.cancel();
    }

    @Test
    public void testAlreadyScheduledAlarm() {
        when(pendingIntentFactory.hasPendingIntent(new Intent(context, EventIntentService.class))).thenReturn(true);

        serviceScheduler.schedule(new Intent(context, EventIntentService.class), AlarmManager.INTERVAL_HOUR);

        verify(logger).debug("Not scheduling {}. It's already scheduled", EventIntentService.class.getSimpleName());
    }

    @Test
    public void arePendingIntentsForTheSameComponentEqual() {
        Context context = InstrumentationRegistry.getTargetContext();
        PendingIntent pendingIntent1 = PendingIntent.getService(context, 0, new Intent(context, EventIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingIntent2 = PendingIntent.getService(context, 0, new Intent(context, EventIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        assertEquals(pendingIntent1, pendingIntent2);
        pendingIntent1.cancel();
        pendingIntent2.cancel();
    }

    @Test
    public void howDoesFlagNoCreateWorks() {
        Context context = InstrumentationRegistry.getTargetContext();
        PendingIntent pendingIntent1 = PendingIntent.getService(context, 0, new Intent(context, EventIntentService.class), PendingIntent.FLAG_NO_CREATE);
        assertNull(pendingIntent1);
        PendingIntent pendingIntent2 = PendingIntent.getService(context, 0, new Intent(context, EventIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingIntent3 = PendingIntent.getService(context, 0, new Intent(context, EventIntentService.class), PendingIntent.FLAG_NO_CREATE);
        assertEquals(pendingIntent2, pendingIntent3);
        pendingIntent2.cancel();
        pendingIntent3.cancel();
    }

    @Test
    public void hasPendingIntent() {
        Context context = InstrumentationRegistry.getTargetContext();
        ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler.PendingIntentFactory(context);
        assertFalse(pendingIntentFactory.hasPendingIntent(new Intent(context, EventIntentService.class)));
        PendingIntent pendingIntent1 = pendingIntentFactory.getPendingIntent(new Intent(context, EventIntentService.class));
        assertTrue(pendingIntentFactory.hasPendingIntent(new Intent(context, EventIntentService.class)));
        PendingIntent pendingIntent2 = pendingIntentFactory.getPendingIntent(new Intent(context, EventIntentService.class));
        assertEquals(pendingIntent1, pendingIntent2);
    }

    @Test
    public void testCancel() {
        PendingIntent pendingIntent = getPendingIntent();
        when(pendingIntentFactory.getPendingIntent(new Intent(context, EventIntentService.class))).thenReturn(pendingIntent);
        serviceScheduler.unschedule(new Intent(context, EventIntentService.class));
        verify(alarmManager).cancel(pendingIntent);
        verify(logger).info("Unscheduled {}", EventIntentService.class.getSimpleName());
    }


    // Mockito can't mock PendingIntent because it's final
    PendingIntent getPendingIntent() {
        final Context context = InstrumentationRegistry.getTargetContext();
        return PendingIntent.getService(context, 0, new Intent(context, EventIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
