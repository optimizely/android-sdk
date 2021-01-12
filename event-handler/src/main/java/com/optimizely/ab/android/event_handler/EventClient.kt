/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                            *
 * *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.android.event_handler

import com.optimizely.ab.android.shared.Client
import org.slf4j.Logger
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection

/**
 * Makes network requests related to events
 */
class EventClient(private val client: Client, // Package private and non final so it can easily be mocked for tests
                           private val logger: Logger) {
    /**
     * Attempt to send the event to the event url.
     * @param event to send
     * @return true if successful
     */
    fun sendEvent(event: Event?): Boolean {
        var success = false
        val request = object : Client.Request<String> {

            override fun execute(): String {

                var urlConnection: HttpURLConnection? = null
            try {
                logger.info("Dispatching event: {}", event)
                if (event == null) {
                    return "Invalid Event"
                }

                urlConnection = client.openConnection(event.uRL)
                if (urlConnection == null) {
                    success = false
                    return ""
                }
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("Content-Type", "application/json")
                urlConnection.doOutput = true
                val outputStream = urlConnection.outputStream
                outputStream.write(event?.requestBody?.toByteArray())
                outputStream.flush()
                outputStream.close()
                val status = urlConnection.responseCode
                if (status >= 200 && status < 300) {
                    val `in`: InputStream = BufferedInputStream(urlConnection.inputStream)
                    `in`.close()
                    success =  java.lang.Boolean.TRUE
                    return ""
                } else {
                    logger.error("Unexpected response from event endpoint, status: $status")
                    success =  java.lang.Boolean.FALSE
                    return ""
                }
            } catch (e: IOException) {
                logger.error("Unable to send event: {}", event, e)
                success =  java.lang.Boolean.FALSE
                return ""
            } catch (e: Exception) {
                logger.error("Unable to send event: {}", event, e)
                success = java.lang.Boolean.FALSE
                return ""
            } finally {
                if (urlConnection != null) {
                    try {
                        urlConnection.disconnect()
                    } catch (e: Exception) {
                        logger.error("Unable to close connection", e)
                    }
                }
            }
            }
        }
        client.execute(request, 2, 5)
        logger.info("Successfully dispatched event: {}", event)
        return success
    }
}