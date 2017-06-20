package com.optimizely.ab.android.datafile_handler;

import android.text.format.Time;

import com.optimizely.ab.android.shared.DataFileLoadedListener;

import java.util.concurrent.TimeUnit;

/**
 * DatafileHandler
 * class that is used to interact with the datafile_handler module.  This interface can be
 * overridden so that the sdk user can provide a override for the default DatafileHandler.
 */
public interface DatafileHandler {
    /**
     * Syncrhonis call to get download the datafile.
     *
     * @param projectId - project id of the project for the datafile.
     * @return a valid datafile or null
     */
    public String dowloadDatafile(String projectId);

    /**
     * Asyncrhonis download data file.
     * @param projectId - project id of the datafile to get.
     * @param listener - listener to call when datafile download complete.
     */
    public void downloadDataFile(String projectId, DataFileLoadedListener listener);

    /**
     *  Startbackground updates to the project datafile .
     * @param updateInterval frequency of updates.
     * @param timeUnit at time interval.
     */
    public void startBackgroundUpdates(String projectId, Long updateInterval, TimeUnit timeUnit, DataFileLoadedListener listener);

    /**
     * Stop the background updates.
     * @param projectId project id of the datafile uploading.
     * @param listener listener that will be called after unscheduling updates.
     */
    public void stopBackgroundUpdates(String projectId, DataFileLoadedListener listener);


    /**
     * Save the datatfile to cache.
     * @param projectId project id of the datafile..
     * @param dataFile the datafile to save.
     */
    public void saveDatafile(String projectId, String dataFile);

    /**
     * Load a cached datafile if it exists
     * @param projectId project id of the datafile to try and get from cache
     * @return The datafile cached or null if it was not available.
     */
    public String loadSavedDatafile(String projectId);

}
