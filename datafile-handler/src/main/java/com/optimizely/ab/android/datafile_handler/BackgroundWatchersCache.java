/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
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
 * Caches a json dict that saves state about which project IDs have background watching enabled.
 */
class BackgroundWatchersCache {
    static final String BACKGROUND_WATCHERS_FILE_NAME = "optly-background-watchers.json";

    @NonNull private final Cache cache;
    @NonNull private final Logger logger;

    BackgroundWatchersCache(@NonNull Cache cache, @NonNull Logger logger) {
        this.cache = cache;
        this.logger = logger;
    }

    boolean setIsWatching(@NonNull String projectId, boolean watching) {
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

    boolean isWatching(@NonNull String projectId) {
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

    List<String> getWatchingProjectIds() {
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
        String backGroundWatchersFile = cache.load(BACKGROUND_WATCHERS_FILE_NAME);
        if (backGroundWatchersFile == null) {
            backGroundWatchersFile = "{}";
            logger.info("Creating background watchers file {}.", BACKGROUND_WATCHERS_FILE_NAME);
        }

        return new JSONObject(backGroundWatchersFile);
    }

    private boolean delete() {
        return cache.delete(BACKGROUND_WATCHERS_FILE_NAME);
    }

    private boolean save(String backgroundWatchersJson) {
        logger.info("Saving background watchers file {}.", BACKGROUND_WATCHERS_FILE_NAME);
        boolean saved = cache.save(BACKGROUND_WATCHERS_FILE_NAME, backgroundWatchersJson);
        if (saved) {
            logger.info("Saved background watchers file {}.", BACKGROUND_WATCHERS_FILE_NAME);
        } else {
            logger.warn("Unable to save background watchers file {}.", BACKGROUND_WATCHERS_FILE_NAME);
        }
        return saved;
    }
}
