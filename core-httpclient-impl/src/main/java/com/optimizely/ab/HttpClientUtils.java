/**
 *
 *    Copyright 2016, Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab;

import org.apache.http.client.config.RequestConfig;

/**
 * Provides defaults and utility methods for using {@link org.apache.http.client.HttpClient}.
 */
public final class HttpClientUtils {

    private static final int CONNECTION_TIMEOUT_MS = 10000;
    private static final int CONNECTION_REQUEST_TIMEOUT_MS = 5000;
    private static final int SOCKET_TIMEOUT_MS = 10000;

    private HttpClientUtils() { }

    public static final RequestConfig DEFAULT_REQUEST_CONFIG = RequestConfig.custom()
        .setConnectTimeout(CONNECTION_TIMEOUT_MS)
        .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
        .setSocketTimeout(SOCKET_TIMEOUT_MS)
        .build();
}
