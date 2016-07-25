package com.optimizely.android;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by jdeffibaugh on 7/25/16 for Optimizely.
 *
 * Proxies {@link URL} because this class is final and not easily mockable when testing.
 */
class URLProxy {
    private URL url;

    URLProxy(URL url) {
        this.url = url;
    }

    URLConnection openConnection() throws IOException {
        return this.url.openConnection();
    }
}
