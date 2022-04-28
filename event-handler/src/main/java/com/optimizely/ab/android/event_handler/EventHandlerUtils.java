/****************************************************************************
 * Copyright 2022, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.android.event_handler;

import androidx.annotation.NonNull;

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class EventHandlerUtils {

    private static final int BUFFER_SIZE = 32*1024;

    public static String compress(@NonNull String decompressed) throws IOException {
        byte[] data = decompressed.getBytes();

        final Deflater deflater = new Deflater();
        deflater.setInput(data);

        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length)) {
            deflater.finish();
            final byte[] buffer = new byte[BUFFER_SIZE];
            while (!deflater.finished()) {
                final int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }

            byte[] bytes = outputStream.toByteArray();
            // encoded to Base64 (instead of byte[] since WorkManager.Data size is unexpectedly expanded with byte[]).
            return encodeToBase64(bytes);
        }
        finally {
            deflater.end();
        }
    }

    public static String decompress(@NonNull String base64) throws Exception {
        byte[] data = decodeFromBase64(base64);

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
        finally {
            inflater.end();
        }
    }

    static String encodeToBase64(byte[] bytes) {
        // - org.apache.commons.Base64 is used (instead of android.util.Base64) for unit testing
        // - encodeBase64() for backward compatibility (instead of encodeBase64String()).
        String base64 = "";
        if (bytes != null) {
            byte[] encoded = Base64.encodeBase64(bytes);
            base64 = new String(encoded);
        }
        return base64;
    }

    static byte[] decodeFromBase64(String base64) {
        return Base64.decodeBase64(base64.getBytes());
    }

}
