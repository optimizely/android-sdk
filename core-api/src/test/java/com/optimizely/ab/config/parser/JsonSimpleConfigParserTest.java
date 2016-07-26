/**
 *
 *    Copyright 2016, Optimizely
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
package com.optimizely.ab.config.parser;

import com.optimizely.ab.config.ProjectConfig;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJson;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfig;
import static com.optimizely.ab.config.ProjectConfigTestUtils.verifyProjectConfig;

/**
 * Tests for {@link JsonSimpleConfigParser}.
 */
public class JsonSimpleConfigParserTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void parseProjectConfig() throws Exception {
        JsonSimpleConfigParser parser = new JsonSimpleConfigParser();
        ProjectConfig actual = parser.parseProjectConfig(validConfigJson());
        ProjectConfig expected = validProjectConfig();

        verifyProjectConfig(actual, expected);
    }

    /**
     * Verify that internal parser exceptions are wrapped and rethrown as a {@link ConfigParseException}.
     */
    @Test
    public void exceptionWrapping() throws Exception {
        thrown.expect(ConfigParseException.class);

        JsonSimpleConfigParser parser = new JsonSimpleConfigParser();
        parser.parseProjectConfig("invalid config");
    }
}