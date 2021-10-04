package com.optimizely.ab.android.event_handler;

import static org.junit.Assert.assertEquals;

import androidx.work.Data;

import org.junit.Test;

import java.io.IOException;

public class EventHandlerUtilsTest {

    @Test
    public void compressAndDecompress() throws Exception {
        String str = makeRandomString(1000);

        String compressed = EventHandlerUtils.compress(str);
        assert(compressed.length() < (str.length() * 0.5));

        String decompressed = EventHandlerUtils.decompress(compressed);
        assertEquals(str, decompressed);
    }

    @Test(timeout=30000)
    public void measureCompressionDelay() throws Exception {
        int maxEventSize = 100000;  // 100KB (~100 attributes)
        int count = 3000;

        String body = EventHandlerUtilsTest.makeRandomString(maxEventSize);

        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            EventHandlerUtils.compress(body);
        }
        long end = System.currentTimeMillis();
        float delayCompress = ((float)(end - start))/count;
        System.out.println("Compression Delay: " + String.valueOf(delayCompress) + " millisecs");
        assert(delayCompress < 10);   // less than 1ms for 100KB (set 10ms upperbound)

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            String compressed = EventHandlerUtils.compress(body);
            EventHandlerUtils.decompress(compressed);
        }
        end = System.currentTimeMillis();
        float delayDecompress = ((float)(end - start))/count - delayCompress;
        System.out.println("Decompression Delay: " + String.valueOf(delayDecompress) + " millisecs");
        assert(delayDecompress < 10);  // less than 1ms for 100KB (set 10ms upperbound)
    }

    public static String makeRandomString(int maxSize) {
        StringBuilder builder = new StringBuilder();

        // for high compression rate, shift repeated string window.
        int window = 100;
        int shift = 3;  // adjust (1...10) this for compression rate. smaller for higher rates.

        int start = 0;
        int end = start + window;
        int i = 0;

        int size = 0;
        while (true) {
            String str = String.valueOf(i);
            size += str.length();
            if (size > maxSize) {
                break;
            }
            builder.append(str);

            i++;
            if (i > end) {
                start = start + shift;
                end = start + window;
                i = start;
            }
        }

        return builder.toString();
    }

}
