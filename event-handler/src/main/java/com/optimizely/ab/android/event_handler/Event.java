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
    public String toString() {
        return this.url.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null
                && getClass().isInstance(obj)
                && (url.equals(((Event) obj).url));
    }
}
