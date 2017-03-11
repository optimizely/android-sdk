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
package com.optimizely.ab.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Map;

public final class EventTagUtils {

    private static final Logger logger = LoggerFactory.getLogger(EventTagUtils.class);

    /**
     * Grab the revenue value from the event tags. "revenue" is a reserved keyword.
     * @param eventTags
     * @return Long
     */
    public static Long getRevenueValue(@Nonnull Map<String, ?> eventTags) {
        Long eventValue = null;
        if (eventTags.containsKey(ReservedEventKey.REVENUE.toString())) {
            Object rawValue = eventTags.get(ReservedEventKey.REVENUE.toString());
            if (Long.class.isInstance(rawValue)) {
                eventValue = (Long)rawValue;
                logger.info("Parsed revenue value \"{}\" from event tags.", eventValue);
            } else if (Integer.class.isInstance(rawValue)) {
                eventValue = ((Integer)rawValue).longValue();
                logger.info("Parsed revenue value \"{}\" from event tags.", eventValue);
            } else {
                logger.warn("Failed to parse revenue value \"{}\" from event tags.", rawValue);
            }
        }
        return eventValue;
    }
}
