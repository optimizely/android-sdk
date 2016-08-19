package com.optimizely.ab.android.sdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by jdeffibaugh on 7/29/16 for Optimizely.
 * <p/>
 * Caches a json dict that saves state about which project IDs have background watching enabled.
 */
public class BackgroundWatchersCache {
    static final String BACKGROUND_WATCHERS_FILE_NAME = "optly-background-watchers.json";

    @NonNull private final Cache cache;
    @NonNull private final Logger logger;

    public BackgroundWatchersCache(@NonNull Cache cache, @NonNull Logger logger) {
        this.cache = cache;
        this.logger = logger;
    }

    public boolean setIsWatching(@NonNull String projectId, boolean watching) {
        if (projectId.isEmpty()) {
            logger.error("Passed in an empty string for projectId");
            return false;
        }

        try {
            JSONObject backgroundWatchers = load();
            if (backgroundWatchers != null) {
                backgroundWatchers.put(projectId, watching);
                return save(backgroundWatchers.toString());
            }
        } catch (JSONException e) {
            logger.error("Unable to update watching state for project id", e);
        }

        return false;
    }

    public boolean isWatching(@NonNull String projectId) {
        if (projectId.isEmpty()) {
            logger.error("Passed in an empty string for projectId");
            return false;
        }

        try {
            JSONObject backgroundWatchers = load();

            if (backgroundWatchers != null) {
                return backgroundWatchers.getBoolean(projectId);

            }
        } catch (JSONException e) {
            logger.error("Unable check if project id is being watched", e);
        }

        return false;
    }

    public List<String> getWatchingProjectIds() {
        List<String> projectIds = new ArrayList<>();
        try {
            JSONObject backgroundWatchers = load();
            if (backgroundWatchers != null) {
                Iterator<String> iterator = backgroundWatchers.keys();
                while (iterator.hasNext()) {
                    final String projectId = iterator.next();
                    if (backgroundWatchers.getBoolean(projectId)) {
                        projectIds.add(projectId);
                    }
                }
            }
        } catch (JSONException e) {
            logger.error("Unable to get watching project ids", e);
        }

        return projectIds;
    }

    @Nullable
    private JSONObject load() throws JSONException {
        String backGroundWatchersFile = null;
        try {
            backGroundWatchersFile = cache.load(BACKGROUND_WATCHERS_FILE_NAME);
        } catch (FileNotFoundException e) {
            logger.info("Creating background watchers file");
            backGroundWatchersFile = "{}";
        } catch (IOException e) {
            logger.error("Unable to load background watchers file", e);
        }

        if (backGroundWatchersFile == null) {
            return null;
        }

        return new JSONObject(backGroundWatchersFile);
    }

    private boolean delete() {
        return cache.delete(BACKGROUND_WATCHERS_FILE_NAME);
    }

    private boolean save(String backgroundWatchersJson) {
        try {
            return cache.save(BACKGROUND_WATCHERS_FILE_NAME, backgroundWatchersJson);
        } catch (IOException e) {
            logger.error("Unable to save background watchers file", e);
            return false;
        }
    }
}
