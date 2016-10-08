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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.net.MalformedURLException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DataFileLoader}
 */
@RunWith(MockitoJUnitRunner.class)
public class DataFileLoaderTest {

    @Mock private DataFileService datafileService;
    @Mock private DataFileCache dataFileCache;
    @Mock private DataFileClient dataFileClient;
    @Mock private DataFileLoadedListener dataFileLoadedListener;
    @Mock private Logger logger;

    @Test
    public void existingDataFileWhenRequestingFromClient() throws MalformedURLException {
        DataFileLoader.RequestDataFileFromClientTask task =
                new DataFileLoader.RequestDataFileFromClientTask("1", datafileService, dataFileCache,
                        dataFileClient, dataFileLoadedListener, logger);

        String url = String.format(DataFileLoader.RequestDataFileFromClientTask.FORMAT_CDN_URL, "1");
        when(dataFileClient.request(url)).thenReturn("");
        when(dataFileCache.exists()).thenReturn(false);
        when(dataFileCache.save("")).thenReturn(true);
        String dataFile = task.doInBackground();
        assertEquals("", dataFile);
        verify(dataFileClient).request(url);
        verify(dataFileCache).save("");
    }

    @Test
    public void existingDataFileWhenRequestingFromClientFailsToDelete() throws MalformedURLException {
        DataFileLoader.RequestDataFileFromClientTask task =
                new DataFileLoader.RequestDataFileFromClientTask("1", datafileService, dataFileCache,
                        dataFileClient, dataFileLoadedListener, logger);

        String url = String.format(DataFileLoader.RequestDataFileFromClientTask.FORMAT_CDN_URL, "1");
        when(dataFileClient.request(url)).thenReturn("");
        when(dataFileCache.exists()).thenReturn(true);
        when(dataFileCache.delete()).thenReturn(false);
        String dataFile = task.doInBackground();
        assertEquals(null, dataFile);
        verify(logger).warn("Unable to delete old data file");
    }

    @Test
    public void existingDataFileWhenRequestingFromClientFailsToSave() throws MalformedURLException {
        DataFileLoader.RequestDataFileFromClientTask task =
                new DataFileLoader.RequestDataFileFromClientTask("1", datafileService, dataFileCache,
                        dataFileClient, dataFileLoadedListener, logger);

        String url = String.format(DataFileLoader.RequestDataFileFromClientTask.FORMAT_CDN_URL, "1");
        when(dataFileClient.request(url)).thenReturn("");
        when(dataFileCache.exists()).thenReturn(false);
        when(dataFileCache.save("")).thenReturn(false);
        String dataFile = task.doInBackground();
        assertEquals(null, dataFile);
        verify(logger).warn("Unable to save new data file");
    }

    @Test
    public void handlesNullDataFile() {
        DataFileLoader.RequestDataFileFromClientTask task =
                new DataFileLoader.RequestDataFileFromClientTask("1", datafileService, dataFileCache,
                        dataFileClient, dataFileLoadedListener, logger);

        when(datafileService.isBound()).thenReturn(false);
        task.onPostExecute(null);
        verify(dataFileLoadedListener, never()).onDataFileLoaded("");
        verify(datafileService).stop();
    }

    @Test
    public void handlesNullListener() {
        DataFileLoader.RequestDataFileFromClientTask task =
                new DataFileLoader.RequestDataFileFromClientTask("1", datafileService, dataFileCache,
                        dataFileClient, null, logger);

        when(datafileService.isBound()).thenReturn(false);
        task.onPostExecute("");
        verify(datafileService).stop();
        verify(dataFileLoadedListener, never()).onDataFileLoaded("");
    }

    @Test
    public void loadFromCache() {
        DataFileLoader.LoadDataFileFromCacheTask task = new DataFileLoader.LoadDataFileFromCacheTask(dataFileCache, dataFileLoadedListener);

        task.doInBackground();
        verify(dataFileCache).load();
    }

    @Test
    public void loadFromCacheNullDataFile() {
        DataFileLoader.LoadDataFileFromCacheTask task = new DataFileLoader.LoadDataFileFromCacheTask(dataFileCache, dataFileLoadedListener);

        task.onPostExecute(null);
        verify(dataFileLoadedListener, never()).onDataFileLoaded(any(String.class));
    }

    @Test
    public void getDataFile() {
        DataFileLoader.TaskChain taskChain = mock(DataFileLoader.TaskChain.class);
        DataFileLoader dataFileLoader = new DataFileLoader(taskChain, logger);
        assertTrue(dataFileLoader.getDataFile("1", dataFileLoadedListener));
        verify(taskChain).start("1", dataFileLoadedListener);
        verify(logger).info("Refreshing data file");
    }
}
