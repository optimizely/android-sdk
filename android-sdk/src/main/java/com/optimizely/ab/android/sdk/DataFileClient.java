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

package com.optimizely.ab.android.sdk;

import android.support.annotation.NonNull;

import com.optimizely.ab.android.shared.Client;

import org.slf4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/*
 * Makes requests to the Optly CDN to get the data file
 */
class DataFileClient {

    @NonNull private final Client client;
    @NonNull private final Logger logger;

    DataFileClient(@NonNull Client client, @NonNull Logger logger) {
        this.client = client;
        this.logger = logger;
    }

    String request(String urlString) {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(urlString);
            logger.info("Requesting data file from {}", url);
            urlConnection = client.openConnection(url);

            client.setIfModifiedSince(urlConnection);

            urlConnection.setConnectTimeout(5 * 1000);
            urlConnection.connect();

            int status = urlConnection.getResponseCode();
            if (status >= 200 && status < 300) {
                client.saveLastModified(urlConnection);
                return client.readStream(urlConnection);
            } else if (status == 304) {
                logger.info("Data file has not been modified on the cdn");
                return null;
            } else {
                logger.error("Unexpected response from data file cdn, status: {}", status);
                return null;
            }
        } catch (IOException e) {
            logger.error("Error making request", e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }


}
