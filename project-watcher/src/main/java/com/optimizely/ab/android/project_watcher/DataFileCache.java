package com.optimizely.ab.android.project_watcher;

import android.content.Context;
import android.support.annotation.NonNull;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by jdeffibaugh on 7/28/16 for Optimizely.
 *
 * Abstracts the actual data "file" {@link java.io.File}
 */
public class DataFileCache {

    private static final String OPTLY_DATA_FILE_NAME = "optly-data-file-%s.json";

    @NonNull private Context context;
    @NonNull String projectId;
    @NonNull private Logger logger;

    public DataFileCache(@NonNull Context context, @NonNull String projectId, @NonNull Logger logger) {
        this.context = context;
        this.projectId = projectId;
        this.logger = logger;
    }

    String load() {
        try {
            FileInputStream fis = context.openFileInput(String.format(OPTLY_DATA_FILE_NAME, projectId));
            InputStreamReader inputStreamReader = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
            logger.error("Tried to load optly data file that does not exist", e);
            return null;
        } catch (IOException e) {
            logger.error("Unable to load optly data file", e);
            return null;
        }
    }

    boolean delete() {
        return context.deleteFile(OPTLY_DATA_FILE_NAME);
    }

    boolean save(String dataFile) {
        try {
            FileOutputStream fos = context.openFileOutput(String.format(OPTLY_DATA_FILE_NAME,
                    projectId), Context.MODE_PRIVATE);
            fos.write(dataFile.getBytes());
            return  true;
        } catch (IOException e) {
            logger.error("Unable to save optly data file to cache", e);
            return false;
        }
    }
}
