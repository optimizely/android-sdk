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
package com.optimizely.ab.config.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Factory for generating {@link ConfigParser} instances, based on the json parser available on the classpath.
 */
public final class DefaultConfigParser {

    private static final Logger logger = LoggerFactory.getLogger(DefaultConfigParser.class);

    private DefaultConfigParser() { }

    public static ConfigParser getInstance() {
        return LazyHolder.INSTANCE;
    }

    //======== Helper methods ========//

    /**
     * Creates and returns a {@link ConfigParser} using a json parser available on the classpath.
     * @return the created config parser
     * @throws MissingJsonParserException if there are no supported json parsers available on the classpath
     */
    private static @Nonnull ConfigParser create() {
        ConfigParser configParser;

        if (isPresent("com.fasterxml.jackson.databind.ObjectMapper")) {
            configParser = new JacksonConfigParser();
        } else if (isPresent("com.google.gson.Gson")) {
            configParser = new GsonConfigParser();
        } else if (isPresent("org.json.simple.JSONObject")) {
            configParser = new JsonSimpleConfigParser();
        } else if (isPresent("org.json.JSONObject")) {
            configParser = new JsonConfigParser();
        } else {
            throw new MissingJsonParserException("unable to locate a JSON parser. "
                                                 + "Please see <link> for more information");
        }

        logger.info("using json parser: {}", configParser.getClass().getSimpleName());
        return configParser;
    }

    private static boolean isPresent(@Nonnull String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    //======== Lazy-init Holder ========//

    private static class LazyHolder {
        private static final ConfigParser INSTANCE = create();
    }
}
