package com.optimizely.ab.android.datafile_handler;

import android.content.Context;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class DatafileHandlerUnitTest {

    DatafileHandler handler = mock(DatafileHandlerDefault.class);

    @Test
    public void testHandler() throws Exception {
        handler = new DatafileHandlerDefault();
        Context context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(context);
        assertFalse(handler.isDatafileSaved(context, "1"));
    }
}