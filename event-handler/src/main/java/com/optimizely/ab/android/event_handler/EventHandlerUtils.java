package com.optimizely.ab.android.event_handler;

import static java.util.zip.Deflater.BEST_COMPRESSION;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

public class EventHandlerUtils {

    private static final int BUFFER_SIZE = 32*1024;

    public static byte[] compress(@NonNull String uncompressed) throws IOException {
        byte[] data = uncompressed.getBytes();

        final Deflater deflater = new Deflater();
        //deflater.setLevel(BEST_COMPRESSION);
        deflater.setInput(data);

        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length)) {
            deflater.finish();
            final byte[] buffer = new byte[BUFFER_SIZE];
            while (!deflater.finished()) {
                final int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }

            return outputStream.toByteArray();
        }
    }

    public static String decompress(@NonNull byte[] data) throws Exception {
        final Inflater inflater = new Inflater();
        inflater.setInput(data);

        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (!inflater.finished()) {
                final int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }

            return outputStream.toString();
        }
    }

}
