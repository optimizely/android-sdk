/****************************************************************************
 * Copyright 2016,2021, Optimizely, Inc. and contributors                   *
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

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.android.shared.ServiceScheduler;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static android.app.Service.START_FLAG_REDELIVERY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DatafileService}
 */
// TODO These tests will pass individually but they fail when run as group
    // Known bug https://code.google.com/p/android/issues/detail?id=180396
    @Ignore
public class DatafileServiceTest {

    private ExecutorService executor;
    private static final int MAX_ITERATION = 100;

    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();

    @Before
    public void setup() {
        executor = Executors.newSingleThreadExecutor();
    }

    @Test
    @Ignore
    public void testBinding() throws TimeoutException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, DatafileService.class);
        IBinder binder = null;
        int it = 0;

        while((binder = mServiceRule.bindService(intent)) == null && it < MAX_ITERATION){
            it++;
        }

        final Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Logger logger = mock(Logger.class);
        DatafileCache datafileCache = new DatafileCache("1", new Cache(targetContext, logger), logger);
        Client client = mock(Client.class);
        DatafileClient datafileClient = new DatafileClient(client, logger);
        DatafileLoadedListener datafileLoadedListener = mock(DatafileLoadedListener.class);


        DatafileService datafileService = ((DatafileService.LocalBinder) binder).getService();
        DatafileLoader datafileLoader = new DatafileLoader(targetContext, datafileClient, datafileCache, mock(Logger.class));
        datafileService.getDatafile("1", datafileLoader, datafileLoadedListener);

        assertTrue(datafileService.isBound());
    }

    @Test
    public void testValidStart() throws TimeoutException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, DatafileService.class);
        IBinder binder = null;
        int it = 0;

        while((binder = mServiceRule.bindService(intent)) == null && it < MAX_ITERATION){
            it++;
        }

        intent.putExtra(DatafileService.EXTRA_DATAFILE_CONFIG, new DatafileConfig("1", null).toJSONString());
        DatafileService datafileService = ((DatafileService.LocalBinder) binder).getService();
        Logger logger = mock(Logger.class);
        datafileService.logger = logger;
        int val = datafileService.onStartCommand(intent, 0, 0);
        assertEquals(val, START_FLAG_REDELIVERY);
    }

    @Test
    @Ignore
    public void testNullIntentStart() throws TimeoutException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, DatafileService.class);
        IBinder binder = null;
        int it = 0;

        while((binder = mServiceRule.bindService(intent)) == null && it < MAX_ITERATION){
            it++;
        }
        mServiceRule.bindService(intent);
        DatafileService datafileService = ((DatafileService.LocalBinder) binder).getService();
        Logger logger = mock(Logger.class);
        datafileService.logger = logger;
        datafileService.onStartCommand(null, 0, 0);
        verify(logger).warn("Data file service received a null intent");
    }

    @Test
    @Ignore
    public void testNoProjectIdIntentStart() throws TimeoutException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, DatafileService.class);
        IBinder binder = null;
        int it = 0;

        while((binder = mServiceRule.bindService(intent)) == null && it < MAX_ITERATION){
            it++;
        }

        DatafileService datafileService = ((DatafileService.LocalBinder) binder).getService();
        Logger logger = mock(Logger.class);
        datafileService.logger = logger;
        datafileService.onStartCommand(intent, 0, 0);
        verify(logger).warn("Data file service received an intent with no project id extra");
    }

    @Test
    @Ignore
    public void testUnbind() throws TimeoutException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, DatafileService.class);
        IBinder binder = null;
        int it = 0;

        while((binder = mServiceRule.bindService(intent)) == null && it < MAX_ITERATION){
            it++;
        }

        DatafileService datafileService = ((DatafileService.LocalBinder) binder).getService();
        Logger logger = mock(Logger.class);
        datafileService.logger = logger;

        datafileService.onUnbind(intent);
        verify(logger).info("All clients are unbound from data file service");
    }

    @Test
    @Ignore
    public void testIntentExtraData(){
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("com.optly");
        ServiceScheduler serviceScheduler = mock(ServiceScheduler.class);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler
                .PendingIntentFactory(context);

        Intent intent = new Intent(context, DatafileService.class);
        intent.putExtra(DatafileService.EXTRA_DATAFILE_CONFIG, new DatafileConfig("1", null).toJSONString());
        serviceScheduler.schedule(intent, TimeUnit.HOURS.toMillis(1L));

        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Timed out");
        }

        verify(serviceScheduler).schedule(captor.capture(), eq(TimeUnit.HOURS.toMillis(1L)));

        Intent intent2 = captor.getValue();
        assertTrue(intent2.getComponent().getShortClassName().contains("DatafileService"));
    }

    @Test
    public void testGetDatafileUrl(){
        // HARD-CODING link here to make sure we don't unintentionally mess up the datafile version
        // and url by accidentally changing those constants.
        // us to update this test.
        String datafileUrl = new DatafileConfig("1", null).getUrl();
        assertEquals("https://cdn.optimizely.com/json/1.json", datafileUrl);
    }

    @Test
    public void testGetDatafileEnvironmentUrl(){
        // HARD-CODING link here to make sure we don't unintentionally mess up the datafile version
        // and url by accidentally changing those constants.
        // us to update this test.
        String datafileUrl = new DatafileConfig("1", "2").getUrl();
        assertEquals("https://cdn.optimizely.com/datafiles/2.json", datafileUrl);
    }
}
