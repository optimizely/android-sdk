package com.optimizely.ab.android.datafile_handler;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class DatafileHandlerTest {

    DatafileHandler handler = mock(DefaultDatafileHandler.class);

    @Before
    public void setup() {
        handler = new DefaultDatafileHandler();
    }

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.optimizely.ab.android.datafile_handler.test", appContext.getPackageName());
    }

    @Test
    public void testSaveExistsRemove() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        handler.saveDatafile(appContext, "1", "{}");
        assertTrue(handler.isDatafileSaved(appContext, "1"));
        assertNotNull(handler.loadSavedDatafile(appContext, "1"));
        handler.removeSavedDatafile(appContext, "1");
        assertFalse(handler.isDatafileSaved(appContext, "1"));
        assertEquals("com.optimizely.ab.android.datafile_handler.test", appContext.getPackageName());
    }

    @Test
    public void testDownload() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        String datafile = handler.downloadDatafile(appContext, "1");

        assertNull(datafile);
    }

    @Test
    public void testAsyncDownload() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        handler.downloadDatafile(appContext, "1", new DatafileLoadedListener() {
            @Override
            public void onDatafileLoaded(@Nullable String dataFile) {
                assertNull(dataFile);
            }

            @Override
            public void onStop(Context context) {

            }
        });

    }

    @Test
    public void testBackground() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        handler.startBackgroundUpdates(appContext, "1", 24 * 60 * 60L);

        assertTrue(true);

        handler.stopBackgroundUpdates(appContext, "1");

        assertTrue(true);
    }
}
