package com.optimizely.ab.android.project_watcher;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.concurrent.TimeoutException;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by jdeffibaugh on 8/2/16 for Optimizely.
 *
 * Test for {@link DataFileService}
 */
// TODO These tests will pass individually but they fail when run as group
    // Known bug https://code.google.com/p/android/issues/detail?id=180396
public class DatafileServiceTest {

    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();

    @Test
    @Ignore
    public void testBinding() throws TimeoutException {
        Context context = InstrumentationRegistry.getTargetContext();
        Intent intent = new Intent(context, DataFileService.class);
        IBinder binder = mServiceRule.bindService(intent);
        DataFileService dataFileService = ((DataFileService.LocalBinder) binder).getService();
        DataFileLoader dataFileLoader = mock(DataFileLoader.class);
        OnDataFileLoadedListener onDataFileLoadedListener = mock(OnDataFileLoadedListener.class);
        dataFileService.getDataFile("1", dataFileLoader, onDataFileLoadedListener);
        verify(dataFileLoader).getDataFile("1", onDataFileLoadedListener);

        assertTrue(dataFileService.isBound());
    }

    @Test
    @Ignore
    public void testValidStart() throws TimeoutException {
        Context context = InstrumentationRegistry.getTargetContext();
        Intent intent = new Intent(context, DataFileService.class);
        IBinder binder = mServiceRule.bindService(intent);
        intent.putExtra(DataFileService.EXTRA_PROJECT_ID, "1");
        DataFileService dataFileService = ((DataFileService.LocalBinder) binder).getService();
        Logger logger = mock(Logger.class);
        dataFileService.logger = logger;
        dataFileService.onStartCommand(intent, 0, 0);
        verify(logger).info("Started watching project {} in the background", "1");
    }

    @Test
    @Ignore
    public void testNullIntentStart() throws TimeoutException {
        Context context = InstrumentationRegistry.getTargetContext();
        Intent intent = new Intent(context, DataFileService.class);
        IBinder binder = mServiceRule.bindService(intent);
        DataFileService dataFileService = ((DataFileService.LocalBinder) binder).getService();
        Logger logger = mock(Logger.class);
        dataFileService.logger = logger;
        dataFileService.onStartCommand(null, 0, 0);
        verify(logger).warn("Data file service received a null intent");
    }

    @Test
    public void testNoProjectIdIntentStart() throws TimeoutException {
        Context context = InstrumentationRegistry.getTargetContext();
        Intent intent = new Intent(context, DataFileService.class);
        IBinder binder = mServiceRule.bindService(intent);
        DataFileService dataFileService = ((DataFileService.LocalBinder) binder).getService();
        Logger logger = mock(Logger.class);
        dataFileService.logger = logger;
        dataFileService.onStartCommand(intent, 0, 0);
        verify(logger).warn("Data file service received an intent with no project id extra");
    }
}
