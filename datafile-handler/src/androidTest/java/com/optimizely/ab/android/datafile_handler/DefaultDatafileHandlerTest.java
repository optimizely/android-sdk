package com.optimizely.ab.android.datafile_handler;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.android.shared.ProjectId;

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
 * Tests for {@link DefaultDatafileHandler}
 */
@RunWith(AndroidJUnit4.class)
public class DefaultDatafileHandlerTest {

    DatafileHandler datafileHandler = mock(DefaultDatafileHandler.class);

    @Before
    public void setup() {
        datafileHandler = new DefaultDatafileHandler();
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

        ProjectId projectId = new ProjectId("1");
        datafileHandler.saveDatafile(appContext, projectId, "{}");
        assertTrue(datafileHandler.isDatafileSaved(appContext, projectId));
        assertNotNull(datafileHandler.loadSavedDatafile(appContext, projectId));
        datafileHandler.removeSavedDatafile(appContext, projectId);
        assertFalse(datafileHandler.isDatafileSaved(appContext, projectId));
        assertEquals("com.optimizely.ab.android.datafile_handler.test", appContext.getPackageName());
    }

    @Test
    public void testDownload() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        String datafile = datafileHandler.downloadDatafile(appContext, new ProjectId("1"));

        assertNull(datafile);
    }

    @Test
    public void testAsyncDownload() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        datafileHandler.downloadDatafile(appContext, new ProjectId("1"), new DatafileLoadedListener() {
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

        datafileHandler.startBackgroundUpdates(appContext, new ProjectId("1"), 24 * 60 * 60L);

        assertTrue(true);

        datafileHandler.stopBackgroundUpdates(appContext,  new ProjectId("1"));

        assertTrue(true);
    }
}
