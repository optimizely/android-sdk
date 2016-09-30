/**
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

import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * Created by jdeffibaugh on 7/21/16 for Optimizely.
 *
 * Makes network requests related to events
 */
public class EventClient {
    // Package private and non final so it can easily be mocked for tests
    private Logger logger;

    EventClient(Logger logger) {
        this.logger = logger;
    }

    public boolean sendEvent(Event event) {
        try {
            logger.info("Dispatching event: {}", event);
            HttpURLConnection urlConnection = (HttpURLConnection) event.send();
            int status = urlConnection.getResponseCode();
                if (status >= 200 && status < 300) {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    in.close();
                return true;
            } else {
                logger.error("Unexpected response from event endpoint, status: " + status);
                return false;
            }
        } catch (IOException e) {
            logger.error("Unable to send event: {}", event, e);
            return false;
        }
    }
}
