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

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

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
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DatafileLoader}
 */
@RunWith(AndroidJUnit4.class)
public class DatafileLoaderTest {

    private DatafileCache datafileCache;
    private DatafileClient datafileClient;
    private Client client;
    private Logger logger;
    private DatafileLoadedListener datafileLoadedListener;
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Before
    public void setup() {
        logger = mock(Logger.class);
        final Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        datafileCache = new DatafileCache("1", new Cache(targetContext, logger), logger);
        client = mock(Client.class);
        datafileClient = new DatafileClient(client, logger);
        datafileLoadedListener = mock(DatafileLoadedListener.class);
    }

    @After
    public void tearDown() {
        datafileCache.delete();
    }

    @Test
    public void loadFromCDNWhenNoCachedFile() throws MalformedURLException, JSONException {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        DatafileLoader datafileLoader =
                new DatafileLoader(context, datafileClient, datafileCache, logger);

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
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        DatafileLoader datafileLoader =
                new DatafileLoader(context, datafileClient, datafileCache, logger);
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
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        DatafileLoader datafileLoader =
                new DatafileLoader(context, datafileClient, datafileCache, logger);

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
    // flacky with lower API
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void warningsAreLogged() throws IOException {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        Cache cache = mock(Cache.class);
        datafileCache = new DatafileCache("warningsAreLogged", cache, logger);
        DatafileLoader datafileLoader =
                new DatafileLoader(context, datafileClient, datafileCache, logger);

        when(client.execute(any(Client.Request.class), anyInt(), anyInt())).thenReturn("{}");
        when(cache.exists(datafileCache.getFileName())).thenReturn(true);
        when(cache.delete(datafileCache.getFileName())).thenReturn(false);
        when(cache.save(datafileCache.getFileName(), "{}")).thenReturn(false);

        datafileLoader.getDatafile("warningsAreLogged", datafileLoadedListener);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }

        verify(logger).warn("Unable to delete old datafile");
        verify(logger).warn("Unable to save new datafile");
        verify(datafileLoadedListener, atMost(1)).onDatafileLoaded("{}");
    }

    @Test
    // flacky with lower API
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void debugLogged() throws IOException {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        Cache cache = mock(Cache.class);
        datafileCache = new DatafileCache("debugLogged", cache, logger);
        DatafileLoader datafileLoader =
                new DatafileLoader(context, datafileClient, datafileCache, logger);

        when(client.execute(any(Client.Request.class), anyInt(), anyInt())).thenReturn("{}");
        when(cache.save(datafileCache.getFileName(), "{}")).thenReturn(true);
        when(cache.exists(datafileCache.getFileName())).thenReturn(true);
        when(cache.load(datafileCache.getFileName())).thenReturn("{}");

        datafileLoader.getDatafile("debugLogged", datafileLoadedListener);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        datafileLoader.getDatafile("debugLogged", datafileLoadedListener);
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }

        verify(logger).debug("Last download happened under 1 minute ago. Throttled to be at least 1 minute apart.");
        verify(datafileLoadedListener, atMost(2)).onDatafileLoaded("{}");
        verify(datafileLoadedListener, atLeast(1)).onDatafileLoaded("{}");
    }

    @Test
    public void downloadAllowedNoCache() throws IOException {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        Cache cache = mock(Cache.class);
        datafileCache = new DatafileCache("downloadAllowedNoCache", cache, logger);
        DatafileLoader datafileLoader =
                new DatafileLoader(context, datafileClient, datafileCache, logger);

        when(client.execute(any(Client.Request.class), anyInt(), anyInt())).thenReturn("{}");
        when(cache.save(datafileCache.getFileName(), "{}")).thenReturn(false);
        when(cache.exists(datafileCache.getFileName())).thenReturn(false);
        when(cache.load(datafileCache.getFileName())).thenReturn("{}");

        datafileLoader.getDatafile("downloadAllowedNoCache", datafileLoadedListener);
        datafileLoader.getDatafile("downloadAllowedNoCache", datafileLoadedListener);
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }

        verify(logger, never()).debug("Last download happened under 1 minute ago. Throttled to be at least 1 minute apart.");
        verify(datafileLoadedListener, atMost(2)).onDatafileLoaded("{}");
        verify(datafileLoadedListener, atLeast(1)).onDatafileLoaded("{}");
    }

    @Test
    public void debugLoggedMultiThreaded() throws IOException {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        Cache cache = mock(Cache.class);
        datafileCache = new DatafileCache("debugLoggedMultiThreaded", cache, logger);
        DatafileLoader datafileLoader =
                new DatafileLoader(context, datafileClient, datafileCache, logger);

        when(client.execute(any(Client.Request.class), anyInt(), anyInt())).thenReturn("{}");
        when(cache.exists(datafileCache.getFileName())).thenReturn(true);
        when(cache.delete(datafileCache.getFileName())).thenReturn(true);
        when(cache.exists(datafileCache.getFileName())).thenReturn(true);
        when(cache.load(datafileCache.getFileName())).thenReturn("{}");
        when(cache.save(datafileCache.getFileName(), "{}")).thenReturn(true);

        datafileLoader.getDatafile("debugLoggedMultiThreaded", datafileLoadedListener);

        Runnable r = () -> datafileLoader.getDatafile("debugLoggedMultiThreaded", datafileLoadedListener);

        new Thread(r).start();
        new Thread(r).start();
        new Thread(r).start();
        new Thread(r).start();

        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }

        verify(datafileLoadedListener, atMost(5)).onDatafileLoaded("{}");
        verify(datafileLoadedListener, atLeast(1)).onDatafileLoaded("{}");
    }


    private void setTestDownloadFrequency(DatafileLoader datafileLoader, long value) {
        try {
            Field betweenDownloadsMilli = DatafileLoader.class.getDeclaredField("minTimeBetweenDownloadsMilli");
            betweenDownloadsMilli.setAccessible(true);

            //Field modifiersField;
            //modifiersField = Field.class.getDeclaredField("modifiers");
            //modifiersField.setAccessible(true);
            //modifiersField.setInt(betweenDownloadsMilli, betweenDownloadsMilli.getModifiers() & ~Modifier.FINAL);
            betweenDownloadsMilli.set(null, value);

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void allowDoubleDownload() throws IOException {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        Cache cache = mock(Cache.class);
        datafileCache = new DatafileCache("allowDoubleDownload", cache, logger);
        DatafileLoader datafileLoader =
                new DatafileLoader(context, datafileClient, datafileCache, logger);

        // set download time to 1 second
        setTestDownloadFrequency(datafileLoader, 1000L);

        when(client.execute(any(Client.Request.class), anyInt(), anyInt())).thenReturn("{}");

        datafileLoader.getDatafile("allowDoubleDownload", datafileLoadedListener);
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }

        datafileLoader.getDatafile("allowDoubleDownload", datafileLoadedListener);

        // reset back to normal.
        setTestDownloadFrequency(datafileLoader, 60 * 1000L);

        verify(logger, never()).debug("Last download happened under 1 minute ago. Throttled to be at least 1 minute apart.");
        verify(datafileLoadedListener, atMost(2)).onDatafileLoaded("{}");
        verify(datafileLoadedListener, atLeast(1)).onDatafileLoaded("{}");
    }
}
