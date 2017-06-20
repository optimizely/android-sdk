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

package com.optimizely.ab.android.datafile_handler;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.core.deps.guava.util.concurrent.MoreExecutors;
import android.support.test.rule.ServiceTestRule;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.Client;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.concurrent.TimeoutException;

import static junit.framework.Assert.assertEquals;
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

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    @Ignore
    public void testBinding() throws TimeoutException {
        Context context = InstrumentationRegistry.getTargetContext();
        Intent intent = new Intent(context, DataFileService.class);
        IBinder binder = mServiceRule.bindService(intent);
        final Context targetContext = InstrumentationRegistry.getTargetContext();
        Logger logger = mock(Logger.class);
        DataFileCache dataFileCache = new DataFileCache("1", new Cache(targetContext, logger), logger);
        Client client = mock(Client.class);
        DataFileClient dataFileClient = new DataFileClient(client, logger);
        DataFileLoadedListener dataFileLoadedListener = mock(DataFileLoadedListener.class);


        DataFileService dataFileService = ((DataFileService.LocalBinder) binder).getService();
        DataFileLoader dataFileLoader = new DataFileLoader(dataFileService, dataFileClient, dataFileCache, MoreExecutors.newDirectExecutorService(), mock(Logger.class));
        dataFileService.getDataFile("1", dataFileLoader, dataFileLoadedListener);

        assertTrue(dataFileService.isBound());
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
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

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
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

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
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

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testGetDatafileUrl(){
        // HARD-CODING link here to make sure we don't unintentionally mess up the datafile version
        // and url by accidentally changing those constants. Bumping datafile versions will force
        // us to update this test.
        String datafileUrl = DataFileService.getDatafileUrl("1");
        assertEquals("https://cdn.optimizely.com/public/1/datafile_v3.json", datafileUrl);
    }
}
