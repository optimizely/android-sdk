package com.optimizely.ab.android.project_watcher;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 8/2/16 for Optimizely.
 *
 * Tests for {@link DataFileRescheduler}
 */
public class DataFileReschedulerTest {

    DataFileRescheduler dataFileRescheduler;
    Logger logger;

    @Before
    public void setup() {
        dataFileRescheduler = new DataFileRescheduler();
        logger = mock(Logger.class);
        dataFileRescheduler.logger = logger;
    }

    @Test
    public void receivingNullContext() {
        dataFileRescheduler.onReceive(null, mock(Intent.class));
        verify(logger).warn("Received invalid broadcast to data file rescheduler");
    }

    @Test
    public void receivingNullIntent() {
        dataFileRescheduler.onReceive(mock(Context.class), null);
        verify(logger).warn("Received invalid broadcast to data file rescheduler");
    }

    @Test
    public void receivedActionBootCompleted() {
        Context context = InstrumentationRegistry.getTargetContext();
        Intent intent = mock(Intent.class);
        when(intent.getAction()).thenReturn(Intent.ACTION_BOOT_COMPLETED);
        dataFileRescheduler.onReceive(context, intent);
        verify(logger).info("Received intent with action {}", Intent.ACTION_BOOT_COMPLETED);
    }

    @Test
    public void receivedActionMyPackageReplaced() {
        Context context = InstrumentationRegistry.getTargetContext();
        Intent intent = mock(Intent.class);
        when(intent.getAction()).thenReturn(Intent.ACTION_MY_PACKAGE_REPLACED);
        dataFileRescheduler.onReceive(context, intent);
        verify(logger).info("Received intent with action {}", Intent.ACTION_MY_PACKAGE_REPLACED);
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    @Test
    public void dispatchingOne() {
        Context context = InstrumentationRegistry.getTargetContext();
        Context mockContext = mock(Context.class);
        BackgroundWatchersCache backgroundWatchersCache = mock(BackgroundWatchersCache.class);
        when(backgroundWatchersCache.getWatchingProjectIds()).thenReturn(Arrays.asList("1"));
        Logger logger = mock(Logger.class);
        DataFileRescheduler.Dispatcher dispatcher = new DataFileRescheduler.Dispatcher(mockContext, backgroundWatchersCache, logger);
        Intent intent = new Intent(context, DataFileService.class);
        dispatcher.dispatch(intent);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext).startService(captor.capture());
        assertEquals("1", captor.getValue().getStringExtra(DataFileService.EXTRA_PROJECT_ID));
        verify(logger).info("Rescheduled data file watching for project {}", "1");
    }

    @Test
    public void dispatchingMany() {
        Context context = InstrumentationRegistry.getTargetContext();
        Context mockContext = mock(Context.class);
        BackgroundWatchersCache backgroundWatchersCache = mock(BackgroundWatchersCache.class);
        when(backgroundWatchersCache.getWatchingProjectIds()).thenReturn(Arrays.asList("1", "2", "3"));
        Logger logger = mock(Logger.class);
        DataFileRescheduler.Dispatcher dispatcher = new DataFileRescheduler.Dispatcher(mockContext, backgroundWatchersCache, logger);
        Intent intent = new Intent(context, DataFileService.class);
        dispatcher.dispatch(intent);
        verify(mockContext, times(3)).startService(any(Intent.class));
    }
}
