/****************************************************************************
 * Copyright 2026, Optimizely, Inc. and contributors                        *
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

package com.optimizely.ab.android.shared;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.net.Socket;

import javax.net.ssl.SSLSocket;

public class TLSSocketFactoryTest {

    @Test
    public void createdSocketsOnlyEnableTLSv12() throws Exception {
        TLSSocketFactory factory = new TLSSocketFactory();

        // An unconnected socket is enough to check which protocols are enabled.
        Socket socket = factory.createSocket();
        try {
            assertTrue(socket instanceof SSLSocket);
            assertArrayEquals(new String[] {"TLSv1.2"}, ((SSLSocket) socket).getEnabledProtocols());
        } finally {
            socket.close();
        }
    }
}
