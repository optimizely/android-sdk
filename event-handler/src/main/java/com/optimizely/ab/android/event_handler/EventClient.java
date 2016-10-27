/*
 * Copyright 2016, Optimizely
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.optimizely.ab.android.event_handler;

import com.optimizely.ab.android.shared.Client;

import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

/*
 * Created by jdeffibaugh on 7/21/16 for Optimizely.
 * <p>
 * Makes network requests related to events
 */
class EventClient {
    private Client client;
    // Package private and non final so it can easily be mocked for tests
    private Logger logger;

    EventClient(Client client, Logger logger) {
        this.client = client;
        this.logger = logger;
    }

    boolean sendEvent(final Event event) {
        Client.Request<Boolean> request = new Client.Request<Boolean>() {
            @Override
            public Boolean execute() {
                try {
                    logger.info("Dispatching event: {}", event);
                    HttpURLConnection urlConnection = client.openConnection(event.getURL());
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/json");
                    urlConnection.setDoOutput(true);
                    OutputStream outputStream = urlConnection.getOutputStream();
                    outputStream.write(event.getRequestBody().getBytes());
                    outputStream.flush();
                    outputStream.close();
                    int status = urlConnection.getResponseCode();
                    if (status >= 200 && status < 300) {
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        in.close();
                        return Boolean.TRUE;
                    } else {
                        logger.error("Unexpected response from event endpoint, status: " + status);
                        return Boolean.FALSE;
                    }
                } catch (IOException e) {
                    logger.error("Unable to send event: {}", event, e);
                    return Boolean.FALSE;
                }
            }
        };

        Boolean success = client.execute(request, 2, 5);
        if (success == null) {
            success = false;
        }
        return success;
    }
}
