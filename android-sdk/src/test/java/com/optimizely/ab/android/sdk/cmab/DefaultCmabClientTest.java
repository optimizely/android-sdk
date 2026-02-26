// Copyright 2025, Optimizely, Inc. and contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.optimizely.ab.android.sdk.cmab;

import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.cmab.client.CmabFetchException;
import com.optimizely.ab.cmab.client.CmabInvalidResponseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DefaultCmabClient}
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultCmabClientTest {

    private Client mockClient;
    private CmabClientHelperAndroid mockCmabClientHelper;
    private DefaultCmabClient mockCmabClient;
    private String testRuleId = "test-rule-123";
    private String testUserId = "test-user-456";
    private String testCmabUuid = "test-uuid-789";
    private Map<String, Object> testAttributes;

    @Before
    public void setup() {
        mockClient = mock(Client.class);
        mockCmabClientHelper = spy(new CmabClientHelperAndroid());

        testAttributes = new HashMap<>();
        testAttributes.put("age", 25);
        testAttributes.put("country", "US");
    }

    @Test
    public void testConstructorWithClientOnly() {
        Client client = mock(Client.class);
        DefaultCmabClient cmabClient = new DefaultCmabClient(client);
        assertNotNull(cmabClient);
    }

    @Test
    public void testConstructorWithClientAndHelper() {
        Client client = mock(Client.class);
        CmabClientHelperAndroid helper = mock(CmabClientHelperAndroid.class);
        DefaultCmabClient cmabClient = new DefaultCmabClient(client, helper);
        assertNotNull(cmabClient);
    }

    @Test
    public void testConstructorWithContextOnly() {
        android.content.Context mockContext = mock(android.content.Context.class);
        DefaultCmabClient cmabClient = new DefaultCmabClient(mockContext);
        assertNotNull(cmabClient);
    }

    @Test
    public void testConstructorWithContextAndHelper() {
        android.content.Context mockContext = mock(android.content.Context.class);
        CmabClientHelperAndroid helper = mock(CmabClientHelperAndroid.class);
        DefaultCmabClient cmabClient = new DefaultCmabClient(mockContext, helper);
        assertNotNull(cmabClient);
    }

    @Test
    public void testFetchDecisionSuccess() throws Exception {
        HttpURLConnection mockUrlConnection = mock(HttpURLConnection.class);
        ByteArrayOutputStream mockOutputStream = mock(ByteArrayOutputStream.class);

        String mockResponseJson = "{\"variation_id\":\"variation_1\",\"status\":\"success\"}";
        when(mockClient.openConnection(any(URL.class))).thenReturn(mockUrlConnection);
        when(mockUrlConnection.getResponseCode()).thenReturn(200);
        when(mockClient.readStream(mockUrlConnection)).thenReturn(mockResponseJson);
        when(mockUrlConnection.getOutputStream()).thenReturn(mockOutputStream);
        when(mockClient.execute(any(Client.Request.class), anyInt(), anyInt())).thenAnswer(invocation -> {
            Client.Request<String> request = invocation.getArgument(0);
            return request.execute();
        });

        doReturn("{\"user_id\":\"test-user-456\"}")
            .when(mockCmabClientHelper)
            .buildRequestJson(any(), any(), any(), any());
        doReturn(true)
            .when(mockCmabClientHelper)
            .validateResponse(any());
        doReturn("variation_1")
            .when(mockCmabClientHelper)
            .parseVariationId(any());

        mockCmabClient = new DefaultCmabClient(mockClient, mockCmabClientHelper);

        String result = mockCmabClient.fetchDecision(testRuleId, testUserId, testAttributes, testCmabUuid);

        assertEquals("variation_1", result);

        verify(mockUrlConnection).setConnectTimeout(10*1000);
        verify(mockUrlConnection).setReadTimeout(60*1000);
        verify(mockUrlConnection).setRequestMethod("POST");
        verify(mockUrlConnection).setRequestProperty("content-type", "application/json");
        verify(mockUrlConnection).setDoOutput(true);
    }

    @Test(expected = CmabFetchException.class)
    public void testFetchDecisionConnectionFailure() throws CmabFetchException {
        // When openConnection returns null, should throw CmabFetchException
        when(mockClient.openConnection(any(URL.class))).thenReturn(null);
        when(mockClient.execute(any(Client.Request.class), anyInt(), anyInt())).thenAnswer(invocation -> {
            Client.Request<String> request = invocation.getArgument(0);
            return request.execute();
        });

        mockCmabClient = new DefaultCmabClient(mockClient, mockCmabClientHelper);

        // Should throw CmabFetchException when connection fails to open
        mockCmabClient.fetchDecision(testRuleId, testUserId, testAttributes, testCmabUuid);
    }

    @Test(expected = CmabFetchException.class)
    public void testFetchDecisionThrowsExceptionOn500Error() throws CmabFetchException {
        HttpURLConnection mockUrlConnection = mock(HttpURLConnection.class);
        ByteArrayOutputStream mockOutputStream = mock(ByteArrayOutputStream.class);

        when(mockClient.openConnection(any(URL.class))).thenReturn(mockUrlConnection);
        when(mockUrlConnection.getResponseCode()).thenReturn(500);
        when(mockUrlConnection.getResponseMessage()).thenReturn("Internal Server Error");
        when(mockUrlConnection.getOutputStream()).thenReturn(mockOutputStream);
        when(mockClient.execute(any(Client.Request.class), anyInt(), anyInt())).thenAnswer(invocation -> {
            Client.Request<String> request = invocation.getArgument(0);
            return request.execute();
        });

        doReturn("{\"user_id\":\"test-user-456\"}")
            .when(mockCmabClientHelper)
            .buildRequestJson(any(), any(), any(), any());

        mockCmabClient = new DefaultCmabClient(mockClient, mockCmabClientHelper);

        // Should throw CmabFetchException for 500 error
        mockCmabClient.fetchDecision(testRuleId, testUserId, testAttributes, testCmabUuid);
    }

    @Test(expected = CmabFetchException.class)
    public void testFetchDecisionThrowsExceptionOn400Error() throws CmabFetchException {
        HttpURLConnection mockUrlConnection = mock(HttpURLConnection.class);
        ByteArrayOutputStream mockOutputStream = mock(ByteArrayOutputStream.class);

        when(mockClient.openConnection(any(URL.class))).thenReturn(mockUrlConnection);
        when(mockUrlConnection.getResponseCode()).thenReturn(400);
        when(mockUrlConnection.getResponseMessage()).thenReturn("Bad Request");
        when(mockUrlConnection.getOutputStream()).thenReturn(mockOutputStream);
        when(mockClient.execute(any(Client.Request.class), anyInt(), anyInt())).thenAnswer(invocation -> {
            Client.Request<String> request = invocation.getArgument(0);
            return request.execute();
        });

        doReturn("{\"user_id\":\"test-user-456\"}")
            .when(mockCmabClientHelper)
            .buildRequestJson(any(), any(), any(), any());

        mockCmabClient = new DefaultCmabClient(mockClient, mockCmabClientHelper);

        // Should throw CmabFetchException for 400 error
        mockCmabClient.fetchDecision(testRuleId, testUserId, testAttributes, testCmabUuid);
    }

    @Test(expected = CmabFetchException.class)
    public void testFetchDecisionThrowsExceptionOnNetworkError() throws CmabFetchException {
        HttpURLConnection mockUrlConnection = mock(HttpURLConnection.class);
        ByteArrayOutputStream mockOutputStream = mock(ByteArrayOutputStream.class);

        when(mockClient.openConnection(any(URL.class))).thenReturn(mockUrlConnection);
        when(mockUrlConnection.getOutputStream()).thenReturn(mockOutputStream);
        when(mockUrlConnection.getResponseCode()).thenThrow(new IOException("Network error"));
        when(mockClient.execute(any(Client.Request.class), anyInt(), anyInt())).thenAnswer(invocation -> {
            Client.Request<String> request = invocation.getArgument(0);
            return request.execute();
        });

        doReturn("{\"user_id\":\"test-user-456\"}")
            .when(mockCmabClientHelper)
            .buildRequestJson(any(), any(), any(), any());

        mockCmabClient = new DefaultCmabClient(mockClient, mockCmabClientHelper);

        // Should throw CmabFetchException when network IOException occurs
        mockCmabClient.fetchDecision(testRuleId, testUserId, testAttributes, testCmabUuid);
    }

    @Test(expected = CmabInvalidResponseException.class)
    public void testFetchDecisionThrowsExceptionOnInvalidJson() throws CmabInvalidResponseException {
        HttpURLConnection mockUrlConnection = mock(HttpURLConnection.class);
        ByteArrayOutputStream mockOutputStream = mock(ByteArrayOutputStream.class);

        String invalidResponseJson = "{\"invalid\":\"response\"}";
        when(mockClient.openConnection(any(URL.class))).thenReturn(mockUrlConnection);
        when(mockUrlConnection.getResponseCode()).thenReturn(200);
        when(mockClient.readStream(mockUrlConnection)).thenReturn(invalidResponseJson);
        when(mockUrlConnection.getOutputStream()).thenReturn(mockOutputStream);
        when(mockClient.execute(any(Client.Request.class), anyInt(), anyInt())).thenAnswer(invocation -> {
            Client.Request<String> request = invocation.getArgument(0);
            return request.execute();
        });

        doReturn("{\"user_id\":\"test-user-456\"}")
            .when(mockCmabClientHelper)
            .buildRequestJson(any(), any(), any(), any());
        doReturn(false)  // Invalid response
            .when(mockCmabClientHelper)
            .validateResponse(any());

        mockCmabClient = new DefaultCmabClient(mockClient, mockCmabClientHelper);

        // Should throw CmabInvalidResponseException when response validation fails
        mockCmabClient.fetchDecision(testRuleId, testUserId, testAttributes, testCmabUuid);
    }

    @Test
    public void testRetryConfigurationPassedToClient() throws Exception {
        HttpURLConnection mockUrlConnection = mock(HttpURLConnection.class);
        ByteArrayOutputStream mockOutputStream = mock(ByteArrayOutputStream.class);

        String mockResponseJson = "{\"variation_id\":\"variation_1\",\"status\":\"success\"}";
        when(mockClient.openConnection(any(URL.class))).thenReturn(mockUrlConnection);
        when(mockUrlConnection.getResponseCode()).thenReturn(200);
        when(mockClient.readStream(mockUrlConnection)).thenReturn(mockResponseJson);
        when(mockUrlConnection.getOutputStream()).thenReturn(mockOutputStream);
        when(mockClient.execute(any(Client.Request.class), anyInt(), anyInt())).thenAnswer(invocation -> {
            Client.Request<String> request = invocation.getArgument(0);
            return request.execute();
        });

        doReturn("{\"user_id\":\"test-user-456\"}")
            .when(mockCmabClientHelper)
            .buildRequestJson(any(), any(), any(), any());
        doReturn(true)
            .when(mockCmabClientHelper)
            .validateResponse(any());
        doReturn("variation_1")
            .when(mockCmabClientHelper)
            .parseVariationId(any());

        mockCmabClient = new DefaultCmabClient(mockClient, mockCmabClientHelper);

        mockCmabClient.fetchDecision(testRuleId, testUserId, testAttributes, testCmabUuid);

        // Verify the retry configuration is passed to client.execute()
        verify(mockClient).execute(any(Client.Request.class), eq(DefaultCmabClient.REQUEST_BACKOFF_TIMEOUT), eq(DefaultCmabClient.REQUEST_RETRIES_POWER));
        assertEquals("REQUEST_BACKOFF_TIMEOUT should be 1", 1, DefaultCmabClient.REQUEST_BACKOFF_TIMEOUT);
        assertEquals("REQUEST_RETRIES_POWER should be 1", 1, DefaultCmabClient.REQUEST_RETRIES_POWER);
    }
}
