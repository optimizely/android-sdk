/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
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
package com.optimizely.ab.event;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Represents Optimizely tracking and activation events.
 */
@Immutable
public class LogEvent {

    private final RequestMethod requestMethod;
    private final String endpointUrl;
    private final Map<String, String> requestParams;
    private final String body;

    public LogEvent(@Nonnull RequestMethod requestMethod,
                    @Nonnull String endpointUrl,
                    @Nonnull Map<String, String> requestParams,
                    @Nonnull String body) {
        this.requestMethod = requestMethod;
        this.endpointUrl = endpointUrl;
        this.requestParams = requestParams;
        this.body = body;
    }

    //======== Getters ========//

    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public Map<String, String> getRequestParams() {
        return requestParams;
    }

    public String getBody() {
        return body;
    }

    //======== Overriding method ========//

    @Override
    public String toString() {
        return "LogEvent{" +
               "requestMethod=" + requestMethod +
               ", endpointUrl='" + endpointUrl + '\'' +
               ", requestParams=" + requestParams +
               ", body='" + body + '\'' +
               '}';
    }

    //======== Helper classes ========//

    /**
     * The HTTP verb to use when dispatching the log event.
     */
    public enum RequestMethod {
        GET,
        POST
    }
}
