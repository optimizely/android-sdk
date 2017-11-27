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

import com.optimizely.ab.HttpClientUtils;
import com.optimizely.ab.NamedThreadFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.CheckForNull;

/**
 * {@link EventHandler} implementation that queues events and has a separate pool of threads responsible
 * for the dispatch.
 */
public class AsyncEventHandler implements EventHandler, Closeable {

    // The following static values are public so that they can be tweaked if necessary.
    // These are the recommended settings for http protocol.  https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
    // The maximum number of connections allowed across all routes.
    private int maxTotalConnections = 200;
    // The maximum number of connections allowed for a route
    private int maxPerRoute = 20;
    // Defines period of inactivity in milliseconds after which persistent connections must be re-validated prior to being leased to the consumer.
    private int validateAfterInactivity = 5000;

    private static final Logger logger = LoggerFactory.getLogger(AsyncEventHandler.class);
    private static final ProjectConfigResponseHandler EVENT_RESPONSE_HANDLER = new ProjectConfigResponseHandler();

    private final CloseableHttpClient httpClient;
    private final ExecutorService workerExecutor;
    private final BlockingQueue<LogEvent> logEventQueue;

    public AsyncEventHandler(int queueCapacity, int numWorkers) {
        this(queueCapacity, numWorkers, 200, 20, 5000);
    }

    public AsyncEventHandler(int queueCapacity, int numWorkers, int maxConnections, int connectionsPerRoute, int validateAfter) {
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queue capacity must be > 0");
        }

        this.maxTotalConnections = maxConnections;
        this.maxPerRoute = connectionsPerRoute;
        this.validateAfterInactivity = validateAfter;

        this.logEventQueue = new ArrayBlockingQueue<LogEvent>(queueCapacity);
        this.httpClient = HttpClients.custom()
            .setDefaultRequestConfig(HttpClientUtils.DEFAULT_REQUEST_CONFIG)
                .setConnectionManager(poolingHttpClientConnectionManager())
            .disableCookieManagement()
            .build();

        this.workerExecutor = Executors.newFixedThreadPool(
            numWorkers, new NamedThreadFactory("optimizely-event-dispatcher-thread-%s", true));

        // create dispatch workers
        for (int i = 0; i < numWorkers; i++) {
            EventDispatchWorker worker = new EventDispatchWorker();
            workerExecutor.submit(worker);
        }
    }

    private PoolingHttpClientConnectionManager poolingHttpClientConnectionManager()
    {
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(maxTotalConnections);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(maxPerRoute);
        poolingHttpClientConnectionManager.setValidateAfterInactivity(validateAfterInactivity);
        return poolingHttpClientConnectionManager;
    }

    @Override
    public void dispatchEvent(LogEvent logEvent) {
        // attempt to enqueue the log event for processing
        boolean submitted = logEventQueue.offer(logEvent);
        if (!submitted) {
            logger.error("unable to enqueue event because queue is full");
        }
    }

    @Override
    public void close() throws IOException {
        logger.info("closing event dispatcher");

        // "close" all workers and the http client
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.error("unable to close the event handler httpclient cleanly", e);
        } finally {
            workerExecutor.shutdownNow();
        }
    }

    //======== Helper classes ========//

    private class EventDispatchWorker implements Runnable {

        @Override
        public void run() {
            boolean terminate = false;

            logger.info("starting event dispatch worker");
            // event loop that'll block waiting for events to appear in the queue
            //noinspection InfiniteLoopStatement
            while (!terminate) {
                try {
                    LogEvent event = logEventQueue.take();
                    HttpRequestBase request;
                    if (event.getRequestMethod() == LogEvent.RequestMethod.GET) {
                        request = generateGetRequest(event);
                    } else {
                        request = generatePostRequest(event);
                    }
                    httpClient.execute(request, EVENT_RESPONSE_HANDLER);
                } catch (InterruptedException e) {
                    logger.info("terminating event dispatcher event loop");
                    terminate = true;
                } catch (Throwable t) {
                    logger.error("event dispatcher threw exception but will continue", t);
                }
            }
        }

        /**
         * Helper method that generates the event request for the given {@link LogEvent}.
         */
        private HttpGet generateGetRequest(LogEvent event) throws URISyntaxException {

            URIBuilder builder = new URIBuilder(event.getEndpointUrl());
            for (Map.Entry<String, String> param : event.getRequestParams().entrySet()) {
                builder.addParameter(param.getKey(), param.getValue());
            }

            return new HttpGet(builder.build());
        }

        private HttpPost generatePostRequest(LogEvent event) throws UnsupportedEncodingException {
            HttpPost post = new HttpPost(event.getEndpointUrl());
            post.setEntity(new StringEntity(event.getBody()));
            post.addHeader("Content-Type", "application/json");
            return post;
        }
    }

    /**
     * Handler for the event request that returns nothing (i.e., Void)
     */
    private static final class ProjectConfigResponseHandler implements ResponseHandler<Void> {

        @Override
        public @CheckForNull Void handleResponse(HttpResponse response) throws IOException {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                // read the response, so we can close the connection
                response.getEntity();
                return null;
            } else {
                throw new ClientProtocolException("unexpected response from event endpoint, status: " + status);
            }
        }
    }
}