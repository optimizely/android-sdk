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

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by jdeffibaugh on 7/25/16 for Optimizely.
 *
 * Event model
 *
 * Proxies {@link URL} because this class is final and not easily mockable when testing.
 */
public class Event {
    private URL url;
    private String requestBody;

    public Event(URL url, String requestBody) {
        this.url = url;
        this.requestBody = requestBody;
    }

    public URLConnection send() throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) this.url.openConnection();

        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setDoOutput(true);
        OutputStream outputStream = urlConnection.getOutputStream();
        outputStream.write(this.requestBody.getBytes());
        outputStream.flush();
        outputStream.close();

        return urlConnection;
    }

    public String getRequestBody() {
        return this.requestBody;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null
                && getClass().isInstance(obj)
                && (url.equals(((Event) obj).url));
    }

    @Override
    public String toString() {
        return this.url.toString();
    }
}
