package com.optimizely.android;

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
class EventClient {
    // Package private and non final so it can easily be mocked for tests
    private Logger logger;

    EventClient(Logger logger) {
        this.logger = logger;
    }

    boolean sendEvent(URLProxy url) {
        try {
            logger.info("Dispatching event: {}", url);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
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
            logger.error("Unable to send event: {}", url, e);
            return false;
        }
    }

}
