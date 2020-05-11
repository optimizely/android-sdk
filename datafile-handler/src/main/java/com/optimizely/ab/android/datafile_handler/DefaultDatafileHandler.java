/****************************************************************************
 * Copyright 2017-2018, Optimizely, Inc. and contributors                        *
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
import android.content.Intent;
import android.os.Build;
import android.os.FileObserver;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.OptlyStorage;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.ab.config.DatafileProjectConfig;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.ProjectConfigManager;
import com.optimizely.ab.config.parser.ConfigParseException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * The default implementation of {@link DatafileHandler} and the main
 * interaction point to the datafile-handler module.
 */
public class DefaultDatafileHandler implements DatafileHandler, ProjectConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(DefaultDatafileHandler.class);
    private ProjectConfig currentProjectConfig;
    private DatafileServiceConnection datafileServiceConnection;
    private FileObserver fileObserver;

    /**
     * Synchronous call to download the datafile.
     * Gets the file on the current thread from the Optimizely CDN.
     *
     * @param context   application context
     * @param datafileConfig DatafileConfig for the datafile
     * @return a valid datafile or null
     */
    public String downloadDatafile(Context context, DatafileConfig datafileConfig) {
        DatafileClient datafileClient = new DatafileClient(
                new Client(new OptlyStorage(context), LoggerFactory.getLogger(OptlyStorage.class)),
                LoggerFactory.getLogger(DatafileClient.class));

        String datafileUrl = datafileConfig.getUrl();

        return datafileClient.request(datafileUrl);
    }

    /**
     * Asynchronous download data file.
     * <p>
     * We create a DatafileService intent, create a DataService connection, and bind it to the application context.
     * After we receive the datafile, we unbind the service and cleanup the service connection.
     * This gets the project file from the Optimizely CDN.
     *
     * @param context   application context
     * @param datafileConfig DatafileConfig for the datafile to get
     * @param listener  listener to call when datafile download complete
     */
    public void downloadDatafile(final Context context, DatafileConfig datafileConfig, final DatafileLoadedListener listener) {
        final Intent intent = new Intent(context.getApplicationContext(), DatafileService.class);
        if (datafileServiceConnection == null) {
            this.datafileServiceConnection = new DatafileServiceConnection(datafileConfig, context.getApplicationContext(),
                    new DatafileLoadedListener() {
                        @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
                        @Override
                        public void onDatafileLoaded(@Nullable String dataFile) {
                            if (listener != null) {
                                listener.onDatafileLoaded(dataFile);
                            }

                            if (datafileServiceConnection != null && datafileServiceConnection.isBound()) {
                                context.getApplicationContext().unbindService(datafileServiceConnection);
                                datafileServiceConnection = null;
                            }

                        }
             });
            context.getApplicationContext().bindService(intent, datafileServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void downloadDatafileToCache(final Context context, DatafileConfig datafileConfig, boolean updateConfigOnNewDatafile) {
        if (updateConfigOnNewDatafile) {
            enableUpdateConfigOnNewDatafile(context, datafileConfig, null);
        }

        downloadDatafile(context, datafileConfig, null);
    }

    /**
     * Start background checks if the the project datafile jas been updated.  This starts an alarm service that checks to see if there is a
     * new datafile to download at interval provided.  If there is a update, the new datafile is cached.
     *
     * @param context        application context
     * @param datafileConfig DatafileConfig for the datafile
     * @param updateInterval frequency of updates in seconds
     */
    public void startBackgroundUpdates(Context context, DatafileConfig datafileConfig, Long updateInterval, DatafileLoadedListener listener) {
        // if already running, stop it
        stopBackgroundUpdates(context, datafileConfig);

        // save the project id background start is set.  If we get a reboot or a replace, we can restart via the
        // DatafileRescheduler
        enableBackgroundCache(context, datafileConfig);

        ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler
                .PendingIntentFactory(context.getApplicationContext());
        ServiceScheduler serviceScheduler = new ServiceScheduler(context, pendingIntentFactory,
                LoggerFactory.getLogger(ServiceScheduler.class));

        Intent intent = new Intent(context.getApplicationContext(), DatafileService.class);
        intent.putExtra(DatafileService.EXTRA_DATAFILE_CONFIG, datafileConfig.toJSONString());
        serviceScheduler.schedule(intent, updateInterval * 1000);

        storeInterval(context, updateInterval * 1000);

        enableUpdateConfigOnNewDatafile(context, datafileConfig, listener);
    }

    public void enableUpdateConfigOnNewDatafile(Context context, DatafileConfig datafileConfig, DatafileLoadedListener listener) {
        // do not restart observer if already set
        if (fileObserver != null) {
            return;
        }

        DatafileCache datafileCache = new DatafileCache(
                datafileConfig.getKey(),
                new Cache(context, LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(DatafileCache.class)
        );

        File filesFolder = context.getFilesDir();
        fileObserver = new FileObserver(filesFolder.getPath()) {
            @Override
            public void onEvent(int event, @Nullable String path) {

                logger.debug("EVENT: " + String.valueOf(event) + path + datafileCache.getFileName());
                if (event == MODIFY && path.equals(datafileCache.getFileName())) {
                    JSONObject newConfig = datafileCache.load();
                    if (newConfig == null) {
                        logger.error("Cached datafile is empty or corrupt");
                        return;
                    }
                    String config = newConfig.toString();
                    setDatafile(config);
                    if (listener != null) {
                        listener.onDatafileLoaded(config);
                    }
                }
            }
        };
        fileObserver.startWatching();
    }

    private static void storeInterval(Context context, long interval) {
        OptlyStorage storage = new OptlyStorage(context);
        storage.saveLong("DATAFILE_INTERVAL", interval);
    }

    public static long getUpdateInterval(Context context) {
        OptlyStorage storage = new OptlyStorage(context);
        return storage.getLong("DATAFILE_INTERVAL", -1);
    }

    /**
     * Stop the background updates.
     *
     * @param context   application context
     * @param datafileConfig DatafileConfig for the datafile
     */
    public void stopBackgroundUpdates(Context context, DatafileConfig datafileConfig) {
        ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler
                .PendingIntentFactory(context.getApplicationContext());
        ServiceScheduler serviceScheduler = new ServiceScheduler(context.getApplicationContext(), pendingIntentFactory,
                LoggerFactory.getLogger(ServiceScheduler.class));
        Intent intent = new Intent(context.getApplicationContext(), DatafileService.class);
        serviceScheduler.unschedule(intent);

        clearBackgroundCache(context, datafileConfig);

        storeInterval(context, -1);

        if (fileObserver != null) {
            fileObserver.stopWatching();
            fileObserver = null;
        }
    }

    private void enableBackgroundCache(Context context, DatafileConfig datafileConfig) {
        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(
                new Cache(context, LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(BackgroundWatchersCache.class));
        backgroundWatchersCache.setIsWatching(datafileConfig, true);
    }

    private void clearBackgroundCache(Context context, DatafileConfig projectId) {
        BackgroundWatchersCache backgroundWatchersCache = new BackgroundWatchersCache(
                new Cache(context, LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(BackgroundWatchersCache.class));
        backgroundWatchersCache.setIsWatching(projectId, false);
    }


    /**
     * Save the datafile to cache.
     *
     * @param context   application context
     * @param datafileConfig DatafileConfig for the datafile
     * @param dataFile  the datafile to save
     */
    public void saveDatafile(Context context, DatafileConfig datafileConfig, String dataFile) {
        DatafileCache datafileCache = new DatafileCache(
                datafileConfig.getKey(),
                new Cache(context, LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(DatafileCache.class)
        );

        datafileCache.delete();
        datafileCache.save(dataFile);
    }

    /**
     * Load a cached datafile if it exists.
     *
     * @param context   application context
     * @param datafileConfig DatafileConfig for the datafile
     * @return the datafile cached or null if it was not available
     */
    public String loadSavedDatafile(Context context, DatafileConfig datafileConfig) {
        DatafileCache datafileCache = new DatafileCache(
                datafileConfig.getKey(),
                new Cache(context, LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(DatafileCache.class)
        );

        JSONObject datafile = datafileCache.load();
        if (datafile != null) {
            return datafile.toString();
        }

        return null;
    }

    /**
     * Is the datafile cached locally?
     *
     * @param context   application context
     * @param datafileConfig DatafileConfig for the datafile
     * @return true if cached false if not
     */
    public Boolean isDatafileSaved(Context context, DatafileConfig datafileConfig) {
        DatafileCache datafileCache = new DatafileCache(
                datafileConfig.getKey(),
                new Cache(context, LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(DatafileCache.class)
        );

        return datafileCache.exists();
    }

    /**
     * Remove the datafile in cache.
     *
     * @param context   application context
     * @param datafileConfig DatafileConfig of the current datafile.
     */
    public void removeSavedDatafile(Context context, DatafileConfig datafileConfig) {
        DatafileCache datafileCache = new DatafileCache(
                datafileConfig.getKey(),
                new Cache(context, LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(DatafileCache.class)
        );

        if (datafileCache.exists()) {
            datafileCache.delete();
        }
    }

    public void setDatafile(String datafile) {

        if (datafile == null) {
            logger.info("datafile is null, ignoring update");
            return;
        }

        if (datafile.isEmpty()) {
            logger.info("datafile is empty, ignoring update");
            return;
        }

        try {
            currentProjectConfig = new DatafileProjectConfig.Builder().withDatafile(datafile).build();

            logger.info("Datafile successfully loaded with revision: {}", currentProjectConfig.getRevision());
        } catch (ConfigParseException ex) {
            logger.error("Unable to parse the datafile", ex);
            logger.info("Datafile is invalid");
        }
    }
    @Override
    public ProjectConfig getConfig() {
        return currentProjectConfig;
    }
}
