package com.optimizely.ab.android.project_watcher;

import android.support.annotation.NonNull;

import com.optimizely.ab.android.shared.Client;
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

    @NonNull String projectId;
    @NonNull private final Client client;
    @NonNull private final Logger logger;

    DataFileClient(@NonNull String projectId, @NonNull Client client, @NonNull Logger logger) {
        this.client = client;
        this.projectId = projectId;
        this.logger = logger;
    }

    String request() {
        HttpURLConnection urlConnection = null;
        try {
            String endPoint = String.format("https://cdn.optimizely.com/json/%s.json", projectId);
            URL url = new URL(endPoint);
            logger.info("Requesting data file from {}", endPoint);
            urlConnection = (HttpURLConnection) url.openConnection();

            client.setIfModifiedSince(urlConnection);

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


}
