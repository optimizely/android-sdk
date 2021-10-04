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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.app.Instrumentation;
import android.content.Context;

import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.optimizely.ab.event.LogEvent;
import com.optimizely.ab.event.internal.payload.EventBatch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Tests {@link EventWorker}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ EventWorker.class, EventHandlerUtils.class, WorkerParameters.class })
@PowerMockIgnore("jdk.internal.reflect.*")
public class EventWorkerTest {

    private WorkerParameters mockWorkParams = PowerMockito.mock(WorkerParameters.class);
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
        PowerMockito.mockStatic(EventHandlerUtils.class);
        when(EventHandlerUtils.compress(anyString())).thenReturn(base64);

        Data data = EventWorker.compressEvent(host, smallBody);
        assertEquals(data.getString("url"), host);
        assertEquals(data.getString("bodyCompressed"), base64);
        assertNull(data.getString("body"));
    }

    @Test
    public void compressEventWithCompressionFailure() throws IOException {
        PowerMockito.mockStatic(EventHandlerUtils.class);
        PowerMockito.doThrow(new IOException()).when(EventHandlerUtils.class);
        EventHandlerUtils.compress(anyString());  // PowerMockito throws exception on this static method

        // return original body if compress fails

        Data data = EventWorker.compressEvent(host, smallBody);
        assertEquals(data.getString("url"), host);
        assertEquals(data.getString("body"), smallBody);
        assertNull(data.getByteArray("bodyCompressed"));
    }

    @Test
    public void getDataWithSmallEvent() throws IOException {
        LogEvent mockEvent = Mockito.mock(LogEvent.class);
        when(mockEvent.getEndpointUrl()).thenReturn(host);

        int[] sizes = {100, 8000, 9000};
        for (int size : sizes){
            String str = EventHandlerUtilsTest.makeRandomString(size);

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
            String str = EventHandlerUtilsTest.makeRandomString(size);
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

        PowerMockito.mockStatic(EventHandlerUtils.class);
        PowerMockito.doThrow(new IOException()).when(EventHandlerUtils.class);
        EventHandlerUtils.decompress(any());  // PowerMockito throws exception on this static method

        String str = eventWorker.getEventBodyFromInputData(data);
        assertNull(str);
    }

    @Test
    public void isEventValid() {
        assertFalse(eventWorker.isEventValid(null, "string"));
        assertFalse(eventWorker.isEventValid("", "string"));
        assertFalse(eventWorker.isEventValid("string", null));
        assertFalse(eventWorker.isEventValid("string", ""));
        assert(eventWorker.isEventValid("string", "string"));
    }

}
