package com.optimizely.ab.android.shared;

import android.content.Context;
import android.support.annotation.NonNull;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Cache {

    @NonNull private final Context context;
    @NonNull private final Logger logger;

    public Cache(@NonNull Context context, @NonNull Logger logger) {
        this.context = context;
        this.logger = logger;
    }

    public String load(String fileName) {
        try {
            FileInputStream fis = context.openFileInput(fileName);
            InputStreamReader inputStreamReader = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            logger.error("Error loading file", e);
            return null;
        }

    }

    public boolean delete(String fileName) {
        return context.deleteFile(fileName);
    }

    public boolean save(String fileName, String data) {
        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(data.getBytes());
            return true;
        } catch (IOException e) {
            logger.error("Unable to save optly data file to cache", e);
            return false;
        }
    }

}