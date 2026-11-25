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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

public class TLSSocketFactoryTest {

    @Test
    public void defaultFactoryOnlyEnablesTLSv12() throws Exception {
        assertOnlyTLSv12(new TLSSocketFactory());
    }

    @Test
    public void socketsArePinnedToTLSv12EvenWhenTheContextAllowsMore() throws Exception {
        // "TLS" allows several protocol versions (TLSv1.3 and TLSv1.2 on the JVM running this test), so this
        // only passes if enableTLSOnSocket itself limits the sockets to TLSv1.2.
        SSLContext permissive = SSLContext.getInstance("TLS");
        permissive.init(null, null, null);
        SSLSocket unpinned = (SSLSocket) permissive.getSocketFactory().createSocket();
        try {
            assertTrue(unpinned.getEnabledProtocols().length > 1);
        } finally {
            unpinned.close();
        }

        assertOnlyTLSv12(new TLSSocketFactory(SSLContext.getInstance("TLS")));
    }

    private static void assertOnlyTLSv12(TLSSocketFactory factory) throws Exception {
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
