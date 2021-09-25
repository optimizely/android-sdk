package com.optimizely.ab.android.event_handler;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class EventHandlerUtils {

    public static byte[] compress(@NonNull String uncompressed) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(uncompressed.length());
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(uncompressed.getBytes());
        gos.close();
        byte[] compressed = os.toByteArray();
        os.close();
        return compressed;
    }

    public static String uncompress(@NonNull byte[] compressed) throws IOException {
        //final int BUFFER_SIZE = 32;
        final int BUFFER_SIZE = 32*1024;

        ByteArrayInputStream is = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
        StringBuilder uncompressed = new StringBuilder();
        byte[] data = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = gis.read(data)) != -1) {
            uncompressed.append(new String(data, 0, bytesRead));
        }
        gis.close();
        is.close();
        return uncompressed.toString();
    }

}
