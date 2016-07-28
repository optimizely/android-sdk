package com.optimizely.android;

import android.content.Context;

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

    private static final String OPTLY_DATA_FILE_NAME = "optly-data-file.json";

    private Context context;
    private Logger logger;

    public DataFileCache(Context context, Logger logger) {
        this.context = context;
        this.logger = logger;
    }

    String load() {
        try {
            FileInputStream fis = context.openFileInput(OPTLY_DATA_FILE_NAME);
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
            FileOutputStream fos = context.openFileOutput(OPTLY_DATA_FILE_NAME, Context.MODE_PRIVATE);
            fos.write(dataFile.getBytes());
            return  true;
        } catch (IOException e) {
            logger.error("Unable to save optly data file to cache", e);
            return false;
        }
    }
}
