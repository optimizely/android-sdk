package com.optimizely.ab.android.event_handler;

import android.content.Context;
import android.content.Intent;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.event.LogEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by jdeffibaugh on 7/25/16 for Optimizely.
 *
 * Tests for {@link OptlyEventHandler}
 */
@RunWith(AndroidJUnit4.class)
public class OptlyEventHandlerTest {

    Context context;
    Logger logger;

    OptlyEventHandler optlyEventHandler;
    String url = "http://www.foo.com";
    String requestBody = "key1=val1&key2=val2&key3=val3";

    @Before
    public void setupEventHandler() {
        context = mock(Context.class);
        logger = mock(Logger.class);
        optlyEventHandler = OptlyEventHandler.getInstance(context);
        optlyEventHandler.logger = logger;
    }

    @Test
    public void dispatchEventSuccess() throws MalformedURLException {
        optlyEventHandler.dispatchEvent(new LogEvent(null, url, null, requestBody));
        verify(context).startService(any(Intent.class));
        verify(logger).info("Sent url {} to the event handler service", "http://www.foo.com");
    }

    @Test public void dispatchEventNullURL() {
       optlyEventHandler.dispatchEvent(new LogEvent(null, null, null, requestBody));
       verify(logger).error("Event dispatcher received a null url");
    }

    @Test public void dispatchEventNullParams() {
        optlyEventHandler.dispatchEvent(new LogEvent(null, url, null, null));
        verify(logger).error("Event dispatcher received a null request body");
    }

    @Test public void dispatchEmptyUrlString() {
        optlyEventHandler.dispatchEvent(new LogEvent(null, "", null, requestBody));
        verify(logger).error("Event dispatcher received an empty url");
    }

    @Test public void dispatchEmptyParams() {
        optlyEventHandler.dispatchEvent(new LogEvent(null, url, null, ""));
        verify(context).startService(any(Intent.class));
        verify(logger).info("Sent url {} to the event handler service", "http://www.foo.com");
    }
}
