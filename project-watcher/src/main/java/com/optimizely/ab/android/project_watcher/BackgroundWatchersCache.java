package com.optimizely.ab.android.project_watcher;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by jdeffibaugh on 7/29/16 for Optimizely.
 *
 * Caches a json dict that saves state about which project IDs have background watching enabled.
 */
public class BackgroundWatchersCache {
    private static final String BACKGROUND_WATCHERS_FILE_NAME = "optly-background-watchers.json";

    @NonNull private final Cache cache;
    @NonNull private final Logger logger;

    public BackgroundWatchersCache(@NonNull Cache cache, @NonNull Logger logger) {
        this.cache = cache;
        this.logger = logger;
    }

    boolean setIsWatching(String projectId, boolean watching) {
        JSONObject backgroundWatchers = load();
        if (backgroundWatchers != null) {
            try {
                backgroundWatchers.put(projectId, watching);
                save(backgroundWatchers.toString());
                return true;
            } catch (JSONException e) {
                logger.error("Unable to parse background watchers file");
            }
        }

        return false;
    }

    boolean isWatching(String projectId) {
        JSONObject backgroundWatchers = load();
        if (backgroundWatchers != null) {
            try {
                return backgroundWatchers.getBoolean(projectId);
            } catch (JSONException e) {
                logger.error("Unable to retrieve value from json");
            }
        }

        return false;
    }

    List<String> getWatchingProjectIds() {
        List<String> projectIds = new ArrayList<>();
        JSONObject backgroundWatchers = load();
        if (backgroundWatchers != null) {
            Iterator<String> iterator = backgroundWatchers.keys();
            while (iterator.hasNext()) {
                final String projectId = iterator.next();
                try {
                    if (backgroundWatchers.getBoolean(projectId)) {
                        projectIds.add(projectId);
                    }
                } catch (JSONException e) {
                    logger.error("Unable to retrieve value from json");
                }
            }
        }

        return projectIds;
    }

    @Nullable
    private JSONObject load() {
        try {
            String backGroundWatchersFile = cache.load(BACKGROUND_WATCHERS_FILE_NAME);
            return new JSONObject(backGroundWatchersFile);
        } catch (JSONException e) {
            logger.error("Unable to parse background watchers file");
            return null;
        }
    }

    private boolean delete() {
        return cache.delete(BACKGROUND_WATCHERS_FILE_NAME);
    }

    private boolean save(String backgroundWatchersJson) {
        return cache.save(BACKGROUND_WATCHERS_FILE_NAME, backgroundWatchersJson);
    }
}
