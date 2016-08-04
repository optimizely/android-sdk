package com.optimizely.ab.android.project_watcher;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by jdeffibaugh on 7/28/16 for Optimizely.
 *
 * Abstracts the actual data "file" {@link java.io.File}
 */
public class DataFileCache {

    static final String OPTLY_DATA_FILE_NAME = "optly-data-file-%s.json";

    @NonNull private final Cache cache;
    @NonNull private final String projectId;
    @NonNull private final Logger logger;

    public DataFileCache(@NonNull String projectId, @NonNull Cache cache, @NonNull Logger logger) {
        this.cache = cache;
        this.projectId = projectId;
        this.logger = logger;
    }

    @Nullable
    public JSONObject load() {
        String optlyDataFile = null;
        try {
            optlyDataFile = cache.load(getFileName());
        } catch (FileNotFoundException e) {
            logger.info("No data file found");
        } catch (IOException e) {
            logger.error("Unable to load data file", e);
        }

        if (optlyDataFile == null) {
            return null;
        }
        try {
            return new JSONObject(optlyDataFile);
        } catch (JSONException e) {
            logger.error("Unable to parse data file", e);
            return null;
        }

    }

    public boolean delete() {
        return cache.delete(getFileName());
    }

    public boolean exists() {
        return cache.exists(getFileName());
    }

    public boolean save(String dataFile) {
        try {
            return cache.save(getFileName(), dataFile);
        } catch (IOException e) {
            logger.error("Unable to save data file", e);
            return false;
        }
    }


    public String getFileName() {
        return String.format(OPTLY_DATA_FILE_NAME, projectId);
    }

}
