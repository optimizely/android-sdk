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
package com.optimizely.ab.android.sdk;

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
        DataFileLoader dataFileLoader = new DataFileLoader(new DataFileLoader.TaskChain(dataFileService), mock(Logger.class));
        DataFileLoadedListener dataFileLoadedListener = mock(DataFileLoadedListener.class);
        dataFileService.getDataFile("1", dataFileLoader, dataFileLoadedListener);

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
    @Ignore
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
