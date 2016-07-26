package com.optimizely.android;

import android.content.Context;
import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

/**
 * Created by jdeffibaugh on 7/25/16 for Optimizely.
 *
 * Tests for {@link OptlyEventHandler}
 */
@RunWith(MockitoJUnitRunner.class)
public class OptlyEventHandlerTest {

    @Mock Context context;
    @Mock Logger logger;

    OptlyEventHandler optlyEventHandler;
    String url = "http://www.foo.com";
    Map<String,String> params;

    @Before
    public void setupEventHandler() {
        optlyEventHandler = new OptlyEventHandler(context);
        optlyEventHandler.logger = logger;

        params = new HashMap<>();
        params.put("key1", "val1");
        params.put("key2", "val2");
        params.put("key3", "val3");
    }

    @Test
    public void dispatchEventSuccess() throws MalformedURLException {
        optlyEventHandler.dispatchEvent(url, params);
        verify(context).startService(any(Intent.class));
        verify(logger).info("Sent url {} to the event handler service", "http://www.foo.com?key1=val1&key2=val2&key3=val3");
    }

    @Test public void dispatchEventNullURL() {
       optlyEventHandler.dispatchEvent(null, params);
       verify(logger).error("Event dispatcher received a null url");
    }

    @Test public void dispatchEventNullParams() {
        optlyEventHandler.dispatchEvent(url, null);
        verify(logger).error("Event dispatcher received a null params map");
    }
}
