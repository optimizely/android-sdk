package com.optimizely.android;

import java.io.IOException;
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

    public Event(URL url) {
        this.url = url;
    }

    public URLConnection send() throws IOException {
        return this.url.openConnection();
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
