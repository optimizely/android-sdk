package com.optimizely.ab.android.event_handler;

import static org.junit.Assert.assertEquals;

import androidx.work.Data;

import org.junit.Test;

import java.io.IOException;

public class EventHandlerUtilsTest {

    @Test
    public void compressAndUncompress() throws Exception {
        String str = makeRandomString(1000);

        byte[] compressed = EventHandlerUtils.compress(str);
        assert(compressed.length < (str.length() * 0.5));

        String uncompressed = EventHandlerUtils.decompress(compressed);
        assertEquals(str, uncompressed);
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
            byte[] byteArray = EventHandlerUtils.compress(body);
            EventHandlerUtils.decompress(byteArray);
        }
        end = System.currentTimeMillis();
        float delayUncompress = ((float)(end - start))/count - delayCompress;
        System.out.println("Uncompression Delay: " + String.valueOf(delayUncompress) + " millisecs");
        assert(delayUncompress < 10);  // less than 1ms for 100KB (set 10ms upperbound)
    }

    public static String makeRandomString(int maxSize) {
        StringBuilder builder = new StringBuilder();

        // for high compression rate, pick from a small set
        int[] randoms = {1000000, 1000001, 1000002};

        int size = 0;
        for(int i=0;; i++) {
            String str = String.valueOf(randoms[i%3]);
            size += str.length();
            if (size > maxSize) {
                break;
            }
            builder.append(str);
        }

        return builder.toString();
    }

}