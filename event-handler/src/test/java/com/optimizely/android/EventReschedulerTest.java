package com.optimizely.android;

import android.content.Context;
import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 7/26/16 for Optimizely.
 *
 * Unit tests for {@link EventRescheduler}
 */
@RunWith(MockitoJUnitRunner.class)
public class EventReschedulerTest {

    @Mock Context context;
    @Mock Intent intent;
    @Mock Logger logger;
    EventRescheduler rescheduler;

    @Before
    public void setupEventRescheduler() {
        rescheduler = new EventRescheduler();
        rescheduler.logger = logger;
    }

    @Test
    public void onReceiveNullIntent() {
        rescheduler.onReceive(context, null);
        verify(logger).warn("Received invalid broadcast to event rescheduler");
    }

    @Test
    public void onReceiveNullContext() {
        rescheduler.onReceive(null, intent);
        verify(logger).warn("Received invalid broadcast to event rescheduler");
    }

    @Test
    public void onReceiveInvalidAction() {
        when(intent.getAction()).thenReturn("invalid");
        rescheduler.onReceive(context, intent);
        verify(logger).warn("Received invalid broadcast to event rescheduler");
    }

    @Test
    public void onReceiveValidBootComplete() {
        when(intent.getAction()).thenReturn(Intent.ACTION_BOOT_COMPLETED);
        rescheduler.onReceive(context, intent);
        verify(logger).info("Rescheduling event flushing if necessary");
    }

    @Test
    public void onReceiveValidPackageReplaced() {
        when(intent.getAction()).thenReturn(Intent.ACTION_PACKAGE_REPLACED);
        rescheduler.onReceive(context, intent);
        verify(logger).info("Rescheduling event flushing if necessary");
    }
}
