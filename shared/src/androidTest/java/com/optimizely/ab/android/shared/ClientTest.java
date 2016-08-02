package com.optimizely.ab.android.shared;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 8/1/16 for Optimizely.
 *
 * Tests for {@link com.optimizely.ab.android.shared.Client}
 */
@RunWith(AndroidJUnit4.class)
public class ClientTest {

    Client client;
    OptlyStorage optlyStorage;
    Logger logger;

    @Before
    public void setup() {
        optlyStorage = mock(OptlyStorage.class);
        logger = mock(Logger.class);
        client = new Client(optlyStorage, logger);
    }

    @Test
    public void setIfModifiedSinceHasValueInStorage() {
        when(optlyStorage.getLong(Client.LAST_MODIFIED_HEADER_KEY, 0)).thenReturn(100L);
        URLConnection urlConnection = mock(URLConnection.class);
        client.setIfModifiedSince(urlConnection);
        verify(urlConnection).setIfModifiedSince(100L);
    }

    @Test
    public void saveLastModifiedNoHeader() {
        URLConnection urlConnection = mock(URLConnection.class);
        when(urlConnection.getLastModified()).thenReturn(0L);
        client.saveLastModified(urlConnection);
        verify(logger).warn("CDN response didn't have a last modified header");
    }

    @Test
    public void saveLastModifiedWhenExists() {
        URLConnection urlConnection = mock(URLConnection.class);
        when(urlConnection.getLastModified()).thenReturn(100L);
        client.saveLastModified(urlConnection);
        verify(optlyStorage).saveLong(Client.LAST_MODIFIED_HEADER_KEY, 100L);
    }

    @Test
    public void readStreamReturnsString() throws IOException {
        String foo = "foo";
        InputStream is = new ByteArrayInputStream(foo.getBytes());
        URLConnection urlConnection = mock(URLConnection.class);
        when(urlConnection.getInputStream()).thenReturn(is);
        String readFoo = client.readStream(urlConnection);
        assertEquals(foo, readFoo);
    }
}
