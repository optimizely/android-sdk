/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.android.user_profile;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.optimizely.ab.bucketing.UserProfileService.experimentBucketMapKey;
import static com.optimizely.ab.bucketing.UserProfileService.userIdKey;
import static com.optimizely.ab.bucketing.UserProfileService.variationIdKey;

/**
 * A Utils class to help transform a user profile JSONObject to a map and vice versa.
 */
public class UserProfileCacheUtils {

    /**
     * Transform a user profile JSONObject to a user profile map.
     *
     * @param userProfilesJson JSONObject representing the user profile from UserProfileService
     * @return map of user id key and experiments with variations the user has participated in.
     * @throws JSONException Exception if there is a json parsing problem
     */
    public static Map<String, Map<String, Object>> convertJSONObjectToMap(JSONObject userProfilesJson) throws
            JSONException {

        Map<String, Map<String, Object>> userIdToUserProfileMap = new ConcurrentHashMap<>();

        Iterator<String> userIdIterator = userProfilesJson.keys();
        while (userIdIterator.hasNext()) {
            String userId = userIdIterator.next();
            JSONObject userProfileJson = userProfilesJson.getJSONObject(userId);

            Map<String, Map<String, String>> experimentBucketMap = new ConcurrentHashMap<>();
            JSONObject experimentBucketMapJson = userProfileJson.getJSONObject(experimentBucketMapKey);
            Iterator<String> experimentIdIterator = experimentBucketMapJson.keys();
            while (experimentIdIterator.hasNext()) {
                String experimentId = experimentIdIterator.next();
                JSONObject experimentBucketMapEntryJson = experimentBucketMapJson.getJSONObject(experimentId);
                String variationId = experimentBucketMapEntryJson.getString(variationIdKey);

                Map<String, String> decisionMap = new ConcurrentHashMap<>();
                decisionMap.put(variationIdKey, variationId);
                experimentBucketMap.put(experimentId, decisionMap);
            }

            Map<String, Object> userProfileMap = new ConcurrentHashMap<>();
            userProfileMap.put(userIdKey, userId);
            userProfileMap.put(experimentBucketMapKey, experimentBucketMap);

            userIdToUserProfileMap.put(userId, userProfileMap);
        }

        return userIdToUserProfileMap;
    }

    /**
     * Transform a user profile map to a user profile JSONObject.
     *
     * @param userProfilesMap map with user id as key and experiments variation map from there.
     * @return JSONObject of the user profile service
     * @throws Exception if the json is malformed or the map is malformed, an exception can occur.
     */
    public static JSONObject convertMapToJSONObject(Map<String, Map<String, Object>> userProfilesMap) throws Exception {
        JSONObject userProfilesJson = new JSONObject();

        for (Map.Entry<String, Map<String, Object>> userProfileEntry : userProfilesMap.entrySet()) {
            Map<String, Object> userProfileMap = (Map<String, Object>) userProfileEntry.getValue();
            String userId = (String) userProfileMap.get(userIdKey);
            Map<String, Map<String, String>> experimentBucketMap = (Map<String, Map<String, String>>)
                    userProfileMap.get(experimentBucketMapKey);

            JSONObject experimentBucketMapJson = new JSONObject();
            for (Map.Entry<String, Map<String, String>> experimentBucketMapEntry : experimentBucketMap.entrySet()) {
                String experimentId = (String) experimentBucketMapEntry.getKey();
                Map<String, String> decisionsMap =  experimentBucketMapEntry.getValue();
                JSONObject decisionJson = new JSONObject();
                decisionJson.put(variationIdKey, decisionsMap.get
                        (variationIdKey));
                experimentBucketMapJson.put(experimentId, decisionJson);
            }

            JSONObject userProfileJson = new JSONObject();
            userProfileJson.put(userIdKey, userId);
            userProfileJson.put(experimentBucketMapKey, experimentBucketMapJson);

            // Add user profile to JSONObject of all user profiles.
            userProfilesJson.put(userId, userProfileJson);
        }

        return userProfilesJson;
    }
}
