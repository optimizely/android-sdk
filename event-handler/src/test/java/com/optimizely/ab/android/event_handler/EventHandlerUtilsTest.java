package com.optimizely.ab.android.event_handler;

import static org.junit.Assert.assertEquals;

import androidx.work.Data;

import org.junit.Test;

import java.io.IOException;

public class EventHandlerUtilsTest {

    @Test
    public void compressAndUncompress() throws IOException {
        String str = makeRandomString(1000);

        byte[] compressed = EventHandlerUtils.compress(str);
        assert(compressed.length < (str.length() * 0.5));

        String uncompressed = EventHandlerUtils.uncompress(compressed);
        assertEquals(str, uncompressed);
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
