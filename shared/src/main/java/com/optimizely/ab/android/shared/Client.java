/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
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

package com.optimizely.ab.android.shared;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Functionality common to all clients
 * @hide
 */
public class Client {

    static final String LAST_MODIFIED_HEADER_KEY = "com.optimizely.ab.android.LAST_MODIFIED_HEADER";
    static final int MAX_BACKOFF_TIMEOUT = (int) Math.pow(2, 5);

    @NonNull private final OptlyStorage optlyStorage;
    @NonNull private final Logger logger;

    /**
     * Constructs a new Client instance
     *
     * @param optlyStorage an instance of {@link OptlyStorage}
     * @param logger       an instance of {@link Logger}
     * @hide
     */
    public Client(@NonNull OptlyStorage optlyStorage, @NonNull Logger logger) {
        this.optlyStorage = optlyStorage;
        this.logger = logger;
    }

    /**
     * Opens {@link HttpURLConnection} from a {@link URL}
     *
     * @param url a {@link URL} instance
     * @return an open {@link HttpURLConnection}
     * @hide
     */
    @Nullable
    public HttpURLConnection openConnection(URL url) {
        try {
            return (HttpURLConnection) url.openConnection();
        } catch (Exception e) {
            logger.warn("Error making request to {}.", url);
        }
        return null;
    }

    /**
     * Adds a if-modified-since header to the open {@link URLConnection} if this value is
     * stored in {@link OptlyStorage}.
     * @param urlConnection an open {@link URLConnection}
     * @hide
     */
    public void setIfModifiedSince(@NonNull URLConnection urlConnection) {
        long lastModified = optlyStorage.getLong(LAST_MODIFIED_HEADER_KEY, 0);
        if (lastModified > 0) {
            urlConnection.setIfModifiedSince(lastModified);
        }
    }

    /**
     * Retrieves the last-modified head from a {@link URLConnection} and saves it
     * in {@link OptlyStorage}.
     * @param urlConnection a {@link URLConnection} instance
     * @hide
     */
    public void saveLastModified(@NonNull URLConnection urlConnection) {
        long lastModified = urlConnection.getLastModified();
        if (lastModified > 0) {
            optlyStorage.saveLong(LAST_MODIFIED_HEADER_KEY, urlConnection.getLastModified());
        } else {
            logger.warn("CDN response didn't have a last modified header");
        }
    }

    @Nullable
    public String readStream(@NonNull URLConnection urlConnection) {
        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            Scanner s = new Scanner(in).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        } catch (Exception e) {
            logger.warn("Error reading urlConnection stream.", e);
            return null;
        }
    }

    /**
     * Executes a request with exponential backoff
     * @param request the request executable, would be a lambda on Java 8
     * @param timeout the numerical base for the exponential backoff
     * @param power the number of retries
     * @param <T> the response type of the request
     * @return the response
     */
    public <T> T execute(Request<T> request, int timeout, int power) {
        int baseTimeout = timeout;
        int maxTimeout = (int) Math.pow(baseTimeout, power);
        T response = null;
        while(timeout <= maxTimeout) {
            try {
                response = request.execute();
            } catch (Exception e) {
                logger.error("Request failed with error: ", e);
            }

            if (response == null || response == Boolean.FALSE) {
                try {
                    logger.info("Request failed, waiting {} seconds to try again", timeout);
                    Thread.sleep(TimeUnit.MILLISECONDS.convert(timeout, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    logger.warn("Exponential backoff failed", e);
                    break;
                }
                timeout = timeout * baseTimeout;
            } else {
                break;
            }
        }
        return response;
    }

    /**
     * Bundles up a request allowing it's execution to be deferred
     * @param <T> The response type of the request
     */
    public interface Request<T> {
        T execute();
    }
}
