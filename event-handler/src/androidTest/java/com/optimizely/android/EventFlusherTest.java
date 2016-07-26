package com.optimizely.android;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import static org.mockito.Mockito.mock;

/**
 * Created by jdeffibaugh on 7/25/16 for Optimizely.
 *
 * Tests {@link EventFlusher}
 */
@RunWith(AndroidJUnit4.class)
public class EventFlusherTest {

    EventFlusher eventFlusher;
    EventDAO eventDAO;
    EventClient eventClient;
    Logger logger;

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getTargetContext();
        eventDAO = mock(EventDAO.class);
        eventClient = mock(EventClient.class);
        logger = mock(Logger.class);
        eventFlusher = new EventFlusher(context, eventDAO, eventClient, logger);
    }

    @Test
    public void handleIntentSchedules() {

    }
}
