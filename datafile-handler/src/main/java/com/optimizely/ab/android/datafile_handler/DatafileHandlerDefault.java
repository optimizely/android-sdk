package com.optimizely.ab.android.datafile_handler;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.OptlyStorage;
import com.optimizely.ab.android.shared.ServiceScheduler;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * DatafileHandlerDefault - This is the default implementation of DatafileHandler and is the main
 * interactive point to the datafile-handler module.
 */

public class DatafileHandlerDefault implements DatafileHandler {

    private DataFileServiceConnection dataFileServiceConnection;

    /**
     * Synchronous call to get download the datafile.
     * Gets the file on the current thread from the optimizley cdn.
     * @param context - application context.
     * @param projectId - project id of the project for the datafile.
     * @return a valid datafile or null
     */
    public String dowloadDatafile(Context context, String projectId) {
        DataFileClient dataFileClient = new DataFileClient(
                new Client(new OptlyStorage(context), LoggerFactory.getLogger(OptlyStorage.class)),
                LoggerFactory.getLogger(DataFileClient.class));

        String datafileUrl = DataFileService.getDatafileUrl(projectId);

        return dataFileClient.request(datafileUrl);
    }

    /**
     * Asynchronous download data file.
     *
     * We create a DataFileService intent, create a DataService connection, and bind it to the application context.
     * After we receive the datafile, we unbind the service and cleanup the service connection.
     * This gets the project file from the optimizely cdn.
     *
     * @param context - application context.
     * @param projectId - project id of the datafile to get.
     * @param listener - listener to call when datafile download complete.
     */
    public void downloadDatafile(final Context context, String projectId, final DataFileLoadedListener listener) {

        final Intent intent = new Intent(context.getApplicationContext(), DataFileService.class);
        if (dataFileServiceConnection == null) {
            this.dataFileServiceConnection = new DataFileServiceConnection(projectId, context.getApplicationContext(),
                    new DataFileLoadedListener() {
                        @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
                        @Override
                        public void onDataFileLoaded(@Nullable String dataFile) {
                            listener.onDataFileLoaded(dataFile);

                            if (dataFileServiceConnection != null && dataFileServiceConnection.isBound()) {
                                context.getApplicationContext().unbindService(dataFileServiceConnection);
                                dataFileServiceConnection = null;
                            }

                        }

                        @Override
                        public void onStop(Context context) {
                            listener.onStop(context);
                        }
                    });
            context.getApplicationContext().bindService(intent, dataFileServiceConnection, Context.BIND_AUTO_CREATE);
        }

    }

    /**
     *  Start background checks if the the project datafile jas been updated.  This starts an alarm service that checks to see if there is a
     *  new datafile to download at interval provided.  If there is a update, the new datafile is cached.
     *
     * @param context - application context.
     * @param updateInterval frequency of updates.
     * @param timeUnit at time interval.
     */
    public void startBackgroundUpdates(Context context, String projectId, Long updateInterval, TimeUnit timeUnit) {
        AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler
                .PendingIntentFactory(context.getApplicationContext());
        ServiceScheduler serviceScheduler = new ServiceScheduler(alarmManager, pendingIntentFactory,
                LoggerFactory.getLogger(ServiceScheduler.class));

        Intent intent = new Intent(context.getApplicationContext(), DataFileService.class);
        intent.putExtra("com.optimizely.ab.android.EXTRA_PROJECT_ID", projectId);
        serviceScheduler.schedule(intent, timeUnit.toMillis(updateInterval));

    }

    /**
     * Stop the background updates.
     * @param context - application context.
     * @param projectId project id of the datafile uploading.
     */
    public void stopBackgroundUpdates(Context context, String projectId) {
        AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        ServiceScheduler.PendingIntentFactory pendingIntentFactory = new ServiceScheduler
                .PendingIntentFactory(context.getApplicationContext());
        ServiceScheduler serviceScheduler = new ServiceScheduler(alarmManager, pendingIntentFactory,
                LoggerFactory.getLogger(ServiceScheduler.class));
        Intent intent = new Intent(context.getApplicationContext(), DataFileService.class);
        serviceScheduler.unschedule(intent);

    }


    /**
     * Save the datatfile to cache.
     * @param context - application context.
     * @param projectId project id of the datafile..
     * @param dataFile the datafile to save.
     */
    public void saveDatafile(Context context, String projectId, String dataFile) {
        DataFileCache dataFileCache = new DataFileCache(
                projectId,
                new Cache(context, LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(DataFileCache.class)
        );

        dataFileCache.delete();

        dataFileCache.save(dataFile);
    }

    /**
     * Load a cached datafile if it exists.
     * @param context - application context.
     * @param projectId project id of the datafile to try and get from cache
     * @return The datafile cached or null if it was not available.
     */
    public String loadSavedDatafile(Context context, String projectId) {
        DataFileCache dataFileCache = new DataFileCache(
                projectId,
                new Cache(context, LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(DataFileCache.class)
        );

        JSONObject datafile = dataFileCache.load();
        if (datafile != null) {
            return datafile.toString();
        }

        return null;
    }

    /**
     * Is the datafile cached locally?
     * @param context - application context.
     * @param projectId
     * @return true if cached false if not.
     */
    public Boolean isDatafileSaved(Context context, String projectId) {
        DataFileCache dataFileCache = new DataFileCache(
                projectId,
                new Cache(context, LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(DataFileCache.class)
        );

        return dataFileCache.exists();
    }

    /**
     * Remove the datatfile in cache.
     * @param projectId project id of the datafile..
     */
    public void removeSavedDatafile(Context context, String projectId) {
        DataFileCache dataFileCache = new DataFileCache(
                projectId,
                new Cache(context, LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(DataFileCache.class)
        );
        if (dataFileCache.exists()) {
            dataFileCache.delete();
        }
    }


}
