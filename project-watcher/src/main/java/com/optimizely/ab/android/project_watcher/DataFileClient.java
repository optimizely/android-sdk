package com.optimizely.ab.android.project_watcher;

import android.support.annotation.NonNull;

import com.optimizely.ab.android.shared.OptlyStorage;

import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by jdeffibaugh on 7/28/16 for Optimizely.
 * <p/>
 * Makes requests to the Optly CDN to get the data file
 */
public class DataFileClient {

    private static String LAST_MODIFIED_HEADER_KEY = "com.optimizely.ab.android.LAST_MODIFIED_HEADER";

    @NonNull String projectId;
    @NonNull private final OptlyStorage optlyStorage;
    @NonNull private final Logger logger;

    DataFileClient(@NonNull String projectId, @NonNull OptlyStorage optlyStorage, @NonNull Logger logger) {
        this.projectId = projectId;
        this.optlyStorage = optlyStorage;
        this.logger = logger;
    }

    String request() {
        HttpURLConnection urlConnection = null;
        try {
            String endPoint = String.format("https://cdn.optimizely.com/json/%s.json", projectId);
            URL url = new URL(endPoint);
            logger.info("Requesting data file from {}", endPoint);
            urlConnection = (HttpURLConnection) url.openConnection();

            setIfModifiedSince(urlConnection);

            urlConnection.connect();

            int status = urlConnection.getResponseCode();
            if (status >= 200 && status < 300) {
                saveLastModified(urlConnection);
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                return readStream(in);
            } else if (status == 304) {
                logger.info("Data file has not been modified on the cdn");
                return null;
            } else {
                logger.error("Unexpected response from data file cdn, status: {}", status);
                return null;
            }
        } catch (MalformedURLException e) {
            logger.error("Bad url", e);
            return null;
        } catch (IOException e) {
            logger.error("Error making request", e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private void setIfModifiedSince(URLConnection urlConnection) {
        long lastModified = optlyStorage.getLong(LAST_MODIFIED_HEADER_KEY, 0);
        if (lastModified > 0) {
            urlConnection.setIfModifiedSince(lastModified);
        }
    }

    private void saveLastModified(URLConnection urlConnection) {
        long lastModified = urlConnection.getLastModified();
        if (lastModified != 0) {
            optlyStorage.saveLong(LAST_MODIFIED_HEADER_KEY, urlConnection.getLastModified());
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        java.util.Scanner s = new java.util.Scanner(inputStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
