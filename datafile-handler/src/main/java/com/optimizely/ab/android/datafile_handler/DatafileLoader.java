/****************************************************************************
 * Copyright 2016-2021, Optimizely, Inc. and contributors                   *
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

package com.optimizely.ab.android.datafile_handler;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.optimizely.ab.android.shared.OptlyStorage;

import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles intents and bindings in {@link DatafileService}
 */
public class DatafileLoader {
    private static final String datafileDownloadTime = "optlyDatafileDownloadTime";
    private static Long minTimeBetweenDownloadsMilli = 60 * 1000L;

    @NonNull private final DatafileCache datafileCache;
    @NonNull private final DatafileClient datafileClient;
    @NonNull private final Logger logger;
    @NonNull private final OptlyStorage storage;
    @NonNull private final Context context;

    private boolean hasNotifiedListener = false;

    public DatafileLoader(@NonNull Context context,
                          @NonNull DatafileClient datafileClient,
                          @NonNull DatafileCache datafileCache,
                          @NonNull Logger logger) {
        this.context = context;
        this.logger = logger;
        this.datafileClient = datafileClient;
        this.datafileCache = datafileCache;

        this.storage = new OptlyStorage(context);

    }

    private boolean allowDownload(String url, DatafileLoadedListener datafileLoadedListener) {
        long time = storage.getLong(url + datafileDownloadTime, 1);
        Date last = new Date(time);
        Date now = new Date();
        if (now.getTime() - last.getTime() < minTimeBetweenDownloadsMilli && datafileCache.exists()) {
            logger.debug("Last download happened under 1 minute ago. Throttled to be at least 1 minute apart.");
            if (datafileLoadedListener != null) {
                notifyListener(datafileLoadedListener, getCachedDatafile());
            }
            return false;
        }

        return true;
    }

    private void saveDownloadTime(String url) {
        long time = new Date().getTime();
        storage.saveLong(url + datafileDownloadTime, time);
    }

    public void getDatafile(@NonNull String datafileUrl, @Nullable DatafileLoadedListener datafileLoadedListener) {
        if (!allowDownload(datafileUrl, datafileLoadedListener)) {
            return;
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // if there is a problem with the cached datafile, set last modified to 1970
                if (!datafileCache.exists() || (datafileCache.exists() && datafileCache.load() == null)) {
                    // create a wrapper for application context default storage.
                    OptlyStorage storage = new OptlyStorage(context);
                    // set the last modified for this url to 1 millisecond past Jan 1, 1970.
                    storage.saveLong(datafileUrl, 1);
                }
                String dataFile = datafileClient.request(datafileUrl);
                if (dataFile != null && !dataFile.isEmpty()) {
                    if (datafileCache.exists()) {
                        if (!datafileCache.delete()) {
                            logger.warn("Unable to delete old datafile");
                        }
                    }
                    if (!datafileCache.save(dataFile)) {
                        logger.warn("Unable to save new datafile");
                    }
                }
                else {
                    String cachedDatafile = getCachedDatafile();
                    if (cachedDatafile != null) {
                        dataFile = cachedDatafile;
                    }
                }


                // notify the listener asap and don't wait for the storage update of the time.
                notifyListener(datafileLoadedListener, dataFile);

                // set the download time and don't allow downloads to overlap less than a minute
                saveDownloadTime(datafileUrl);
                logger.info("Refreshing data file");
            }
        });
    }

    private void notifyListener(@Nullable DatafileLoadedListener datafileLoadedListener, @Nullable String dataFile) {
        // The listener should be notified ONCE and ONLY ONCE with a valid datafile or null
        // If there are no activities bound there is no need to notify
        if (datafileLoadedListener != null) {
            datafileLoadedListener.onDatafileLoaded(dataFile);
            this.hasNotifiedListener = true;
        }
    }

    private String getCachedDatafile() {
        String dataFile = null;

        JSONObject jsonFile = datafileCache.load();
        if (jsonFile != null) {
            dataFile = jsonFile.toString();
        }

        return dataFile;
    }
}
