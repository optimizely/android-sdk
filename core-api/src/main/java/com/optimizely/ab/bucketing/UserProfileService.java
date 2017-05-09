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

import java.util.HashMap;
import java.util.Map;

/**
 * Class encapsulating user profile service functionality.
 *
 * Override with your own implementation for storing and retrieving the user profile.
 */
public interface UserProfileService {

    /**
     * A class representing a user's profile.
     */
    class UserProfile {

        /** A user's ID. */
        public final String userId;
        /** The bucketing decisions of the user. */
        public final Map<String, String> decisions;

        /**
         * Construct a User Profile instance from explicit components.
         * @param userId The ID of the user.
         * @param decisions The bucketing decisions of the user.
         */
        public UserProfile(String userId, Map<String, String> decisions) {
            this.userId = userId;
            this.decisions = decisions;
        }

        /**
         * Construct a User Profile instance from a Map.
         * @param userProfileMap A {@code Map<String, Object>} containing the properties of the user profile.
         */
        @SuppressWarnings("unchecked")
        public UserProfile(Map<String, Object> userProfileMap) {
            this((String) userProfileMap.get(userIdKey), (Map<String, String>) userProfileMap.get(decisionsKey));
        }

        /**
         * Convert a User Profile instance to a Map.
         * @return A map representation of the user profile instance.
         */
        Map<String, Object> toMap() {
            Map<String, Object> userProfileMap = new HashMap<String, Object>(2);
            userProfileMap.put(userIdKey, userId);
            userProfileMap.put(decisionsKey, decisions);
            return userProfileMap;
        }
    }

    /** The key for the user ID. Returns a String.*/
    String userIdKey = "user_id";
    /** The key for the decisions Map. Returns a {@code Map<String, String>}.*/
    String decisionsKey = "decisions";

    /**
     * Fetch the user profile map for the user ID.
     *
     * @param userId The ID of the user whose profile will be retrieved.
     * @return a Map representing the user's profile.
     * The returned {@code Map<String, Object>} of the user profile will have the following structure.
     * {
     *     userIdKey : String userId,
     *     decisionsKey : {@code Map<String, String>} decisions {
     *          String experimentId : String variationId
     *     }
     * }
     * @throws Exception Passes on whatever exceptions the implementation may throw.
     */
    Map<String, Object> lookup(String userId) throws Exception;

    /**
     * Save the user profile Map sent to this method.
     *
     * @param userProfile The Map representing the user's profile.
     * @throws Exception Can throw an exception if the user profile was not saved properly.
     */
    void save(Map<String, Object> userProfile) throws Exception;
}
