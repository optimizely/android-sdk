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
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.core.deps.guava.util.concurrent.ListeningExecutorService;
import android.support.test.espresso.core.deps.guava.util.concurrent.MoreExecutors;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.Client;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DatafileLoader}
 */
@RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
@RunWith(AndroidJUnit4.class)
public class DatafileLoaderTest {

    private DatafileService datafileService;
    private DatafileCache datafileCache;
    private DatafileClient datafileClient;
    private Client client;
    private Logger logger;
    private DatafileLoadedListener datafileLoadedListener;
    Context context = InstrumentationRegistry.getTargetContext();

    @Before
    public void setup() {
        datafileService = mock(DatafileService.class);
        logger = mock(Logger.class);
        final Context targetContext = InstrumentationRegistry.getTargetContext();
        datafileCache = new DatafileCache("1", new Cache(targetContext, logger), logger);
        client = mock(Client.class);
        datafileClient = new DatafileClient(client, logger);
        datafileLoadedListener = mock(DatafileLoadedListener.class);
        when(datafileService.getApplicationContext()).thenReturn(targetContext);
        when(datafileService.isBound()).thenReturn(true);
    }

    @After
    public void tearDown() {
        datafileCache.delete();
    }

    @Test
    public void loadFromCDNWhenNoCachedFile() throws MalformedURLException, JSONException {
        final ListeningExecutorService executor = MoreExecutors.newDirectExecutorService();
        DatafileLoader datafileLoader =
                new DatafileLoader(datafileService, datafileClient, datafileCache, executor, logger);

        when(client.execute(any(Client.Request.class), anyInt(), anyInt())).thenReturn("{}");

        datafileLoader.getDatafile("1", datafileLoadedListener);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }

        final JSONObject cachedDatafile = datafileCache.load();
        assertNotNull(cachedDatafile);
        assertEquals("{}", cachedDatafile.toString());
        verify(datafileLoadedListener, atMost(1)).onDatafileLoaded("{}");
    }

    @Test
    public void loadWhenCacheFileExistsAndCDNNotModified() {
        final ListeningExecutorService executor = MoreExecutors.newDirectExecutorService();
        DatafileLoader datafileLoader =
                new DatafileLoader(datafileService, datafileClient, datafileCache, executor, logger);
        datafileCache.save("{}");

        when(client.execute(any(Client.Request.class), anyInt(), anyInt())).thenReturn("");

        datafileLoader.getDatafile("1", datafileLoadedListener);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }

        final JSONObject cachedDatafile = datafileCache.load();
        assertNotNull(cachedDatafile);
        assertEquals("{}", cachedDatafile.toString());
        verify(datafileLoadedListener, atMost(1)).onDatafileLoaded("{}");
    }

    @Test
    public void noCacheAndLoadFromCDNFails() {
        final ListeningExecutorService executor = MoreExecutors.newDirectExecutorService();
        DatafileLoader datafileLoader =
                new DatafileLoader(datafileService, datafileClient, datafileCache, executor, logger);

        when(client.execute(any(Client.Request.class), anyInt(), anyInt())).thenReturn(null);

        datafileLoader.getDatafile("1", datafileLoadedListener);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }

        final JSONObject cachedDatafile = datafileCache.load();
        assertNull(cachedDatafile);
        verify(datafileLoadedListener, atMost(1)).onDatafileLoaded(null);
    }

    @Test
    public void warningsAreLogged() throws IOException {
        final ListeningExecutorService executor = MoreExecutors.newDirectExecutorService();
        Cache cache = mock(Cache.class);
        datafileCache = new DatafileCache("1", cache, logger);
        DatafileLoader datafileLoader =
                new DatafileLoader(datafileService, datafileClient, datafileCache, executor, logger);

        when(client.execute(any(Client.Request.class), anyInt(), anyInt())).thenReturn("{}");
        when(cache.exists(datafileCache.getFileName())).thenReturn(true);
        when(cache.delete(datafileCache.getFileName())).thenReturn(false);
        when(cache.save(datafileCache.getFileName(), "{}")).thenReturn(false);

        datafileLoader.getDatafile("1", datafileLoadedListener);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }

        verify(logger).warn("Unable to delete old datafile");
        verify(logger).warn("Unable to save new datafile");
        verify(datafileLoadedListener, atMost(1)).onDatafileLoaded("{}");
    }
}
