package com.optimizely.ab.android.shared;

import android.content.Context;
import android.support.annotation.NonNull;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    @NonNull
    public String load(String fileName) throws IOException {
        FileInputStream fis = context.openFileInput(fileName);
        InputStreamReader inputStreamReader = new InputStreamReader(fis);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    public boolean delete(String fileName) {
        return context.deleteFile(fileName);
    }

    public boolean exists(String fileName) {
        try {
            load(fileName);
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            logger.error("Unable to check if file exists", e);
            return false;
        }
    }

    public boolean save(String fileName, String data) throws IOException {
        FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
        fos.write(data.getBytes());
        return true;
    }
}