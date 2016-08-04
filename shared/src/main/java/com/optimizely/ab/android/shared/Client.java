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
 * Created by jdeffibaugh on 8/1/16 for Optimizely.
 *
 * Functionality common to all clients
 */
public class Client {

    static String LAST_MODIFIED_HEADER_KEY = "com.optimizely.ab.android.LAST_MODIFIED_HEADER";

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
