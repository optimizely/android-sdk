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
 * Tests for {@link DataFileLoader}
 */
@RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
@RunWith(AndroidJUnit4.class)
public class DataFileLoaderTest {

    private DataFileService datafileService;
    private DataFileCache dataFileCache;
    private DataFileClient dataFileClient;
    private Client client;
    private Logger logger;
    private DataFileLoadedListener dataFileLoadedListener;

    @Before
    public void setup() {
        datafileService = Mockito.mock(DataFileService.class);
        logger = Mockito.mock(Logger.class);
        final Context targetContext = InstrumentationRegistry.getTargetContext();
        dataFileCache = new DataFileCache("1", new Cache(targetContext, logger), logger);
        client = Mockito.mock(Client.class);
        dataFileClient = new DataFileClient(client, logger);
        dataFileLoadedListener = Mockito.mock(DataFileLoadedListener.class);
        Mockito.when(datafileService.getApplicationContext()).thenReturn(targetContext);
        Mockito.when(datafileService.isBound()).thenReturn(true);
    }

    @After
    public void tearDown() {
        dataFileCache.delete();
    }

    @Test
    public void loadFromCDNWhenNoCachedFile() throws MalformedURLException, JSONException {
        final ListeningExecutorService executor = MoreExecutors.newDirectExecutorService();
        DataFileLoader dataFileLoader =
                new DataFileLoader(datafileService, dataFileClient, dataFileCache, executor, logger);

        Mockito.when(client.execute(Matchers.any(Client.Request.class), Matchers.anyInt(), Matchers.anyInt())).thenReturn("{}");

        dataFileLoader.getDataFile("1", dataFileLoadedListener);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }

        final JSONObject cachedDataFile = dataFileCache.load();
        assertNotNull(cachedDataFile);
        assertEquals("{}", cachedDataFile.toString());
        Mockito.verify(dataFileLoadedListener, Mockito.atMost(1)).onDataFileLoaded("{}");
    }

    @Test
    public void loadWhenCacheFileExistsAndCDNNotModified() {
        final ListeningExecutorService executor = MoreExecutors.newDirectExecutorService();
        DataFileLoader dataFileLoader =
                new DataFileLoader(datafileService, dataFileClient, dataFileCache, executor, logger);
        dataFileCache.save("{}");

        Mockito.when(client.execute(Matchers.any(Client.Request.class), Matchers.anyInt(), Matchers.anyInt())).thenReturn("");

        dataFileLoader.getDataFile("1", dataFileLoadedListener);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }

        final JSONObject cachedDataFile = dataFileCache.load();
        assertNotNull(cachedDataFile);
        assertEquals("{}", cachedDataFile.toString());
        Mockito.verify(dataFileLoadedListener, Mockito.atMost(1)).onDataFileLoaded("{}");
    }

    @Test
    public void noCacheAndLoadFromCDNFails() {
        final ListeningExecutorService executor = MoreExecutors.newDirectExecutorService();
        DataFileLoader dataFileLoader =
                new DataFileLoader(datafileService, dataFileClient, dataFileCache, executor, logger);

        Mockito.when(client.execute(Matchers.any(Client.Request.class), Matchers.anyInt(), Matchers.anyInt())).thenReturn(null);

        dataFileLoader.getDataFile("1", dataFileLoadedListener);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }

        final JSONObject cachedDataFile = dataFileCache.load();
        assertNull(cachedDataFile);
        Mockito.verify(dataFileLoadedListener, Mockito.atMost(1)).onDataFileLoaded(null);
    }

    @Test
    public void warningsAreLogged() throws IOException {
        final ListeningExecutorService executor = MoreExecutors.newDirectExecutorService();
        Cache cache = Mockito.mock(Cache.class);
        dataFileCache = new DataFileCache("1", cache, logger);
        DataFileLoader dataFileLoader =
                new DataFileLoader(datafileService, dataFileClient, dataFileCache, executor, logger);

        Mockito.when(client.execute(Matchers.any(Client.Request.class), Matchers.anyInt(), Matchers.anyInt())).thenReturn("{}");
        Mockito.when(cache.exists(dataFileCache.getFileName())).thenReturn(true);
        Mockito.when(cache.delete(dataFileCache.getFileName())).thenReturn(false);
        Mockito.when(cache.save(dataFileCache.getFileName(), "{}")).thenReturn(false);

        dataFileLoader.getDataFile("1", dataFileLoadedListener);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }

        Mockito.verify(logger).warn("Unable to delete old datafile");
        Mockito.verify(logger).warn("Unable to save new datafile");
        Mockito.verify(dataFileLoadedListener, Mockito.atMost(1)).onDataFileLoaded("{}");
    }
}
