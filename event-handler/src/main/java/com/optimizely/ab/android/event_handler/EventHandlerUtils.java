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
        return compress(uncompressed, 1);
    }
    public static String decompress(@NonNull byte[] data) throws Exception {
        return decompress(data, 1);
    }


    public static byte[] compress(@NonNull String uncompressed, int idx) throws IOException {
        if (idx==1) {
            return compress_1(uncompressed);
        } else if (idx==2) {
            return compress_2(uncompressed);
        } else {
            return compress_3(uncompressed);
        }
    }

    public static String decompress(@NonNull byte[] data, int idx) throws Exception {
        if (idx==1) {
            return decompress_1(data);
        } else if (idx==2) {
            return decompress_2(data);
        } else {
            return decompress_3(data);
        }
    }

     public static byte[] compress_1(@NonNull String uncompressed) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(uncompressed.length());
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(uncompressed.getBytes());
        gos.close();
        byte[] compressed = os.toByteArray();
        os.close();
        return compressed;
    }

    public static String decompress_1(@NonNull byte[] compressed) throws IOException {
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

    public static byte[] compress_2(@NonNull String uncompressed) throws IOException {
        byte[] data = uncompressed.getBytes();

        final Deflater deflater = new Deflater();
        deflater.setLevel(BEST_COMPRESSION);
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

    public static String decompress_2(@NonNull byte[] data) throws Exception {
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

    public static byte[] compress_3(@NonNull String uncompressed) throws IOException {
        byte[] input = uncompressed.getBytes();

        byte[] output = new byte[100];
        Deflater compresser = new Deflater();
        compresser.setInput(input);
        compresser.finish();
        int compressedDataLength = compresser.deflate(output);
        compresser.end();

        return output;
    }

    public static String decompress_3(@NonNull byte[] data) throws Exception {
        Inflater decompresser = new Inflater();
        decompresser.setInput(data, 0, data.length);
        byte[] result = new byte[100];
        int resultLength = decompresser.inflate(result);
        decompresser.end();

        return new String(result, Charset.forName("UTF-8"));
    }

}
