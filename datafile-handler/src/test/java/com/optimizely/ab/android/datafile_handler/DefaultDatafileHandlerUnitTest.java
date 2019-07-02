package com.optimizely.ab.android.datafile_handler;

import android.content.Context;

import com.optimizely.ab.android.shared.DatafileConfig;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class DefaultDatafileHandlerUnitTest {

    DatafileHandler handler = mock(DefaultDatafileHandler.class);

    @Test
    public void testHandler() throws Exception {
        handler = new DefaultDatafileHandler();
        Context context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(context);
        assertFalse(handler.isDatafileSaved(context, new DatafileConfig("1", null)));
    }
}
