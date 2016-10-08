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
package com.optimizely.ab.android.shared;

import android.support.annotation.NonNull;

import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

/**
 * Functionality common to all clients
 */
public class Client {

    static final String LAST_MODIFIED_HEADER_KEY = "com.optimizely.ab.android.LAST_MODIFIED_HEADER";

    @NonNull private final OptlyStorage optlyStorage;
    @NonNull private final Logger logger;

    public Client(@NonNull OptlyStorage optlyStorage, @NonNull Logger logger) {
        this.optlyStorage = optlyStorage;
        this.logger = logger;
    }

    public HttpURLConnection openConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    public void setIfModifiedSince(@NonNull URLConnection urlConnection) {
        long lastModified = optlyStorage.getLong(LAST_MODIFIED_HEADER_KEY, 0);
        if (lastModified > 0) {
            urlConnection.setIfModifiedSince(lastModified);
        }
    }

    public void saveLastModified(@NonNull URLConnection urlConnection) {
        long lastModified = urlConnection.getLastModified();
        if (lastModified > 0) {
            optlyStorage.saveLong(LAST_MODIFIED_HEADER_KEY, urlConnection.getLastModified());
        } else {
            logger.warn("CDN response didn't have a last modified header");
        }
    }

    public String readStream(@NonNull URLConnection urlConnection) throws IOException {
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        Scanner s = new Scanner(in).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
