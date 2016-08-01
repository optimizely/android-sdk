package com.optimizely.ab.android.project_watcher;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by jdeffibaugh on 7/29/16 for Optimizely.
 */
public class BackgroundWatchersCache {
    private static final String BACKGROUND_WATCHERS_FILE_NAME = "optly-background-watchers.json";

    @NonNull private Context context;
    @NonNull private Logger logger;

    public BackgroundWatchersCache(@NonNull Context context, @NonNull Logger logger) {
        this.context = context;
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
            // TODO pull into helper function
            FileInputStream fis = context.openFileInput(BACKGROUND_WATCHERS_FILE_NAME);
            InputStreamReader inputStreamReader = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return new JSONObject(sb.toString());
        } catch (FileNotFoundException e) {
            logger.error("Tried to load background watchers file that does not exist", e);
            return null;
        } catch (IOException e) {
            logger.error("Unable to load background watchers file", e);
            return null;
        } catch (JSONException e) {
            logger.error("Unable to parse background watchers file");
            return null;
        }
    }

    // TODO Pull into helper function
    private boolean delete() {
        return context.deleteFile(BACKGROUND_WATCHERS_FILE_NAME);
    }

    // TODO pull into helper function
    private boolean save(String backgroundWatchersJson) {
        try {
            FileOutputStream fos = context.openFileOutput(BACKGROUND_WATCHERS_FILE_NAME, Context.MODE_PRIVATE);
            fos.write(backgroundWatchersJson.getBytes());
            return true;
        } catch (IOException e) {
            logger.error("Unable to save optly data file to cache", e);
            return false;
        }
    }
}
