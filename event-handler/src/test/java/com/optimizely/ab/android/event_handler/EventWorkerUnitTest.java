/****************************************************************************
 * Copyright 2021, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.android.event_handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

import org.mockito.MockedStatic;


import android.content.Context;

import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.optimizely.ab.android.shared.EventHandlerUtils;
import com.optimizely.ab.event.LogEvent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

/**
 * Tests {@link EventWorker}
 */
@RunWith(MockitoJUnitRunner.class)
public class EventWorkerUnitTest {

    private WorkerParameters mockWorkParams = mock(WorkerParameters.class);
    private EventWorker eventWorker = new EventWorker(mock(Context.class), mockWorkParams);

    private String host = "http://www.foo.com";
    private String smallBody = "{\"account_id\":\"1234\"}";

    @Test
    public void dataForEvent() {
        Data data1 = EventWorker.dataForEvent(host, "body-string");
        assertEquals(data1.getString("url"), host);
        assertEquals(data1.getString("body"), "body-string");
        assertNull(data1.getByteArray("bodyCompressed"));
    }

    @Test
    public void dataForCompressedEvent() {
        String base64 = "abc123";
        Data data = EventWorker.dataForCompressedEvent(host, base64);
        assertEquals(data.getString("url"), host);
        assertEquals(data.getString("bodyCompressed"), base64);
        assertNull(data.getString("body"));
    }
    @Test
    public void compressEvent() throws IOException {
        String base64 = "abc123";

        // Mocking the static method compress in EventHandlerUtils
        try (MockedStatic<EventHandlerUtils> mockedStatic = mockStatic(EventHandlerUtils.class)) {
            mockedStatic.when(() -> EventHandlerUtils.compress(anyString())).thenReturn(base64);

            Data data = EventWorker.compressEvent(host, smallBody);

            // Verify the results
            assertEquals(data.getString("url"), host);
            assertEquals(data.getString("bodyCompressed"), base64);
            assertNull(data.getString("body"));

            // Optionally, verify that the method was called
            mockedStatic.verify(() -> EventHandlerUtils.compress(anyString()));
        }
    }


    @Test
    public void compressEventWithCompressionFailure() throws IOException {
        try (MockedStatic<EventHandlerUtils> mockedStatic = mockStatic(EventHandlerUtils.class)) {
            mockedStatic.when(() -> EventHandlerUtils.compress(anyString())).thenThrow(new IOException());
            // return original body if compress fails
            Data data = EventWorker.compressEvent(host, smallBody);
            assertEquals(data.getString("url"), host);
            assertEquals(data.getString("body"), smallBody);
            assertNull(data.getByteArray("bodyCompressed"));
        }
    }

    @Test
    public void getDataWithSmallEvent() throws IOException {
        LogEvent mockEvent = Mockito.mock(LogEvent.class);
        when(mockEvent.getEndpointUrl()).thenReturn(host);

        int[] sizes = {100, 8000, 9000};
        for (int size : sizes){
            String str = makeLongString(size);

            when(mockEvent.getBody()).thenReturn(str);

            Data data = EventWorker.getData(mockEvent);
            assertEquals(data.getString("url"), host);
            assertEquals(data.getString("body"), str);
            assertNull(data.getByteArray("bodyCompressed"));
        }
    }

    @Test
    public void getDataWithLargeEvent() throws IOException {
        LogEvent mockEvent = Mockito.mock(LogEvent.class);
        when(mockEvent.getEndpointUrl()).thenReturn(host);

        int[] sizes = {10000, 100000};
        for (int size : sizes){
            String str = makeLongString(size);
            String compressed = EventHandlerUtils.compress(str);
            System.out.println("compressed: " + size + " -> " + compressed.length());

            when(mockEvent.getBody()).thenReturn(str);

            Data data = EventWorker.getData(mockEvent);
            assertEquals(data.getString("url"), host);
            assertEquals(data.getString("bodyCompressed"), compressed);
            assertNull(data.getString("body"));
        }
    }

    @Test
    public void getEventBodyFromInputData() throws Exception {
        Data data = EventWorker.dataForEvent(host, smallBody);
        String str = eventWorker.getEventBodyFromInputData(data);
        assertEquals(str, smallBody);
    }

    @Test
    public void getEventBodyFromInputDataCompressed() {
        Data data = EventWorker.compressEvent(host, smallBody);
        String str = eventWorker.getEventBodyFromInputData(data);
        assertEquals(str, smallBody);
    }

    @Test
    public void getEventBodyFromInputDataDecompressFailure() throws Exception {
        Data data = EventWorker.compressEvent(host, smallBody);

        try (MockedStatic<EventHandlerUtils> mockedStatic = mockStatic(EventHandlerUtils.class)) {
            mockedStatic.when(() -> EventHandlerUtils.compress(anyString())).thenThrow(new IOException());

            // return original body if compress fails

            EventHandlerUtils.decompress(any());

            String str = eventWorker.getEventBodyFromInputData(data);
            assertNull(str);

        }
    }

    @Test
    public void isEventValid() {
        assertFalse(eventWorker.isEventValid(null, "string"));
        assertFalse(eventWorker.isEventValid("", "string"));
        assertFalse(eventWorker.isEventValid("string", null));
        assertFalse(eventWorker.isEventValid("string", ""));
        assert(eventWorker.isEventValid("string", "string"));
    }

    @Test
    public void getDataWithRetryInterval() {
        String host = "host-1";
        String body = "body-1";
        LogEvent event = Mockito.mock(LogEvent.class);
        when(event.getEndpointUrl()).thenReturn(host);
        when(event.getBody()).thenReturn(body);

        Data data = EventWorker.getData(event, 123L);
        assertEquals(eventWorker.getUrlFromInputData(data), host);
        assertEquals(eventWorker.getEventBodyFromInputData(data), body);
        assertEquals(eventWorker.getRetryIntervalFromInputData(data), 123);

        data = EventWorker.getData(event, -1L);
        assertEquals(eventWorker.getUrlFromInputData(data), host);
        assertEquals(eventWorker.getEventBodyFromInputData(data), body);
        assertEquals(eventWorker.getRetryIntervalFromInputData(data), -1);

        // compressed data

        body = makeLongString(20000);
        when(event.getBody()).thenReturn(body);

        data = EventWorker.getData(event, 123L);
        assertEquals(eventWorker.getUrlFromInputData(data), host);
        assertEquals(eventWorker.getEventBodyFromInputData(data), body);
        assertEquals(eventWorker.getRetryIntervalFromInputData(data), 123);

        data = EventWorker.getData(event, 0L);
        assertEquals(eventWorker.getUrlFromInputData(data), host);
        assertEquals(eventWorker.getEventBodyFromInputData(data), body);
        assertEquals(eventWorker.getRetryIntervalFromInputData(data), -1);

        data = EventWorker.getData(event, 0L);
        assertEquals(eventWorker.getUrlFromInputData(data), host);
        assertEquals(eventWorker.getEventBodyFromInputData(data), body);
        assertEquals(eventWorker.getRetryIntervalFromInputData(data), -1);
    }

    // Helpers

    private String makeLongString(int maxSize) {
        StringBuilder builder = new StringBuilder();
        String str = "random-string";
        int repeat = (maxSize / str.length()) + 1;
        for (int i=0; i<repeat; i++) builder.append(str);
        return builder.toString();
    }

}
