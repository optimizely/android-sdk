package com.optimizely.ab.android.project_watcher;

import org.junit.Before;
import org.junit.Test;
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
 * Created by jdeffibaugh on 8/3/16 for Optimizely.
 *
 * Tests for {@link DataFileLoader}
 */
public class DataFileLoaderTest {

    DataFileService datafileService;
    DataFileCache dataFileCache;
    DataFileClient dataFileClient;
    OnDataFileLoadedListener onDataFileLoadedListener;
    Logger logger;

    @Before
    public void setup() {
        datafileService = mock(DataFileService.class);
        dataFileCache = mock(DataFileCache.class);
        dataFileClient = mock(DataFileClient.class);
        onDataFileLoadedListener = mock(OnDataFileLoadedListener.class);
        logger = mock(Logger.class);
    }

    @Test
    public void existingDataFileWhenRequestingFromClient() throws MalformedURLException {
        DataFileLoader.RequestDataFileFromClientTask task =
                new DataFileLoader.RequestDataFileFromClientTask("1", datafileService, dataFileCache,
                        dataFileClient, onDataFileLoadedListener, logger);

        String url = String.format(DataFileLoader.RequestDataFileFromClientTask.FORMAT_CDN_URL, "1");
        when(dataFileClient.request(url)).thenReturn("");
        String dataFile = task.doInBackground(null);
        assertEquals("", dataFile);
        verify(dataFileClient).request(url);
        verify(dataFileCache).delete();
        verify(dataFileCache).save("");
    }

    @Test
    public void handlesNullDataFile() {
        DataFileLoader.RequestDataFileFromClientTask task =
                new DataFileLoader.RequestDataFileFromClientTask("1", datafileService, dataFileCache,
                        dataFileClient, onDataFileLoadedListener, logger);

        when(datafileService.isBound()).thenReturn(false);
        task.onPostExecute(null);
        verify(onDataFileLoadedListener, never()).onDataFileLoaded("");
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
        verify(onDataFileLoadedListener, never()).onDataFileLoaded("");
    }

    @Test
    public void noStopIfBound() {
        DataFileLoader.RequestDataFileFromClientTask task =
                new DataFileLoader.RequestDataFileFromClientTask("1", datafileService, dataFileCache,
                        dataFileClient, onDataFileLoadedListener, logger);

        when(datafileService.isBound()).thenReturn(true);
        task.onPostExecute("");
        verify(datafileService, never()).stop();
    }

    @Test
    public void loadFromCache() {
        DataFileLoader.RequestDataFileFromClientTask requstTask = mock(DataFileLoader.RequestDataFileFromClientTask.class);
        DataFileLoader.LoadDataFileFromCacheTask task = new DataFileLoader.LoadDataFileFromCacheTask(dataFileCache, requstTask, onDataFileLoadedListener);

        task.doInBackground(null);
        verify(dataFileCache).load();
    }

    @Test
    public void loadFromCacheNullDataFile() {
        DataFileLoader.RequestDataFileFromClientTask requestTask = mock(DataFileLoader.RequestDataFileFromClientTask.class);
        DataFileLoader.LoadDataFileFromCacheTask task = new DataFileLoader.LoadDataFileFromCacheTask(dataFileCache, requestTask, onDataFileLoadedListener);

        task.onPostExecute(null);
        verify(onDataFileLoadedListener, never()).onDataFileLoaded(any(String.class));
        verify(requestTask).start();
    }

    @Test
    public void getDataFile() {
        DataFileLoader.TaskChain taskChain = mock(DataFileLoader.TaskChain.class);
        DataFileLoader dataFileLoader = new DataFileLoader(taskChain, logger);
        DataFileLoader.LoadDataFileFromCacheTask task = mock(DataFileLoader.LoadDataFileFromCacheTask.class);
        when(taskChain.get("1", onDataFileLoadedListener)).thenReturn(task);
        assertTrue(dataFileLoader.getDataFile("1", onDataFileLoadedListener));
        verify(taskChain).get("1", onDataFileLoadedListener);
        verify(task).start();
        verify(logger).info("Refreshing data file");
    }
}
