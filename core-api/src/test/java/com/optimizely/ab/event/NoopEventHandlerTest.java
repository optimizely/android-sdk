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

import ch.qos.logback.classic.Level;

import com.optimizely.ab.internal.LogbackVerifier;

import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import static com.optimizely.ab.event.LogEvent.RequestMethod;

/**
 * Tests for {@link NoopEventHandler} -- mostly for coverage...
 */
public class NoopEventHandlerTest {

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    @Test
    public void dispatchEvent() throws Exception {
        NoopEventHandler noopEventHandler = new NoopEventHandler();
        noopEventHandler.dispatchEvent(
            new LogEvent(RequestMethod.GET, "blah", Collections.<String, String>emptyMap(), ""));
        logbackVerifier.expectMessage(Level.DEBUG, "Called dispatchEvent with URL: blah and params: {}");
    }
}