/**
 *
 *    Copyright 2017, Optimizely and contributors
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
package com.optimizely.ab.bucketing;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A Utils class to help transform maps to {@link UserProfile} instances.
 */
@SuppressWarnings("unchecked")
public class UserProfileUtils {

    /**
     * Validate whether a {@code Map<String, Object>} can be transformed into a {@link UserProfile}.
     * @param map The map to check.
     * @return True if the map can be converted into a {@link UserProfile}.
     *          False if the map cannot be converted.
     */
    static boolean isValidUserProfileMap(@Nonnull Map<String, Object> map) {
        // The Map must contain a value for the user ID
        if (!map.containsKey(UserProfileService.userIdKey)) {
            return false;
        }
        // The Map must contain a value for the experiment bucket map
        if (!map.containsKey(UserProfileService.experimentBucketMapKey)) {
            return false;
        }
        // The value for the experimentBucketMapKey must be a map
        if (!(map.get(UserProfileService.experimentBucketMapKey) instanceof Map)) {
            return false;
        }
        // Try and cast the experimentBucketMap value to a typed map
        Map<String, Map<String, String>> experimentBucketMap;
        try {
            experimentBucketMap = (Map<String, Map<String, String>>) map.get(UserProfileService.experimentBucketMapKey);
        }
        catch (ClassCastException classCastException) {
            return false;
        }

        // Check each Decision in the map to make sure it has a variation Id Key
        for (Map<String, String> decision : experimentBucketMap.values()) {
            if (!decision.containsKey(UserProfileService.variationIdKey)) {
                return false;
            }
        }

        // the map is good enough for us to use
        return true;
    }

    /**
     * Convert a Map to a {@link UserProfile} instance.
     * @param map The map to construct the User Profile from.
     * @return A User Profile instance.
     */
    static UserProfile convertMapToUserProfile(@Nonnull Map<String, Object> map) {
        String userId = (String) map.get(UserProfileService.userIdKey);
        Map<String, Map<String, String>> experimentBucketMap = (Map<String, Map<String, String>>) map.get(UserProfileService.experimentBucketMapKey);
        Map<String, Decision> decisions = new HashMap<String, Decision>(experimentBucketMap.size());
        for (Entry<String, Map<String, String>> entry : experimentBucketMap.entrySet()) {
            Decision decision = new Decision(entry.getValue().get(UserProfileService.variationIdKey));
            decisions.put(entry.getKey(), decision);
        }
        return new UserProfile(userId, decisions);
    }
}
