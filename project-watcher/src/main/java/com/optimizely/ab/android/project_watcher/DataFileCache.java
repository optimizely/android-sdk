package com.optimizely.ab.android.project_watcher;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

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
    JSONObject load() {
        String optlyDataFile = cache.load(getFileName());
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

    boolean delete() {
        return cache.delete(getFileName());
    }

    boolean save(String dataFile) {
        return cache.save(getFileName(), dataFile);
    }


    String getFileName() {
        return String.format(OPTLY_DATA_FILE_NAME, projectId);
    }

}
