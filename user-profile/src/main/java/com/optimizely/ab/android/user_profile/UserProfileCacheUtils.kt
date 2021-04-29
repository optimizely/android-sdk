/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
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
package com.optimizely.ab.android.user_profile

import com.optimizely.ab.bucketing.UserProfileService
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * A Utils class to help transform a user profile JSONObject to a map and vice versa.
 */
object UserProfileCacheUtils {
    /**
     * Transform a user profile JSONObject to a user profile map.
     *
     * @param userProfilesJson JSONObject representing the user profile from UserProfileService
     * @return map of user id key and experiments with variations the user has participated in.
     * @throws JSONException Exception if there is a json parsing problem
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun convertJSONObjectToMap(userProfilesJson: JSONObject): Map<String, Map<String, Any>> {
        val userIdToUserProfileMap: MutableMap<String, Map<String, Any>> = ConcurrentHashMap()
        val userIdIterator = userProfilesJson.keys()
        while (userIdIterator.hasNext()) {
            val userId = userIdIterator.next()
            val userProfileJson = userProfilesJson.getJSONObject(userId)
            val experimentBucketMap: MutableMap<String, Map<String, String>> = ConcurrentHashMap()
            val experimentBucketMapJson = userProfileJson.getJSONObject(UserProfileService.experimentBucketMapKey)
            val experimentIdIterator = experimentBucketMapJson.keys()
            while (experimentIdIterator.hasNext()) {
                val experimentId = experimentIdIterator.next()
                val experimentBucketMapEntryJson = experimentBucketMapJson.getJSONObject(experimentId)
                val variationId = experimentBucketMapEntryJson.getString(UserProfileService.variationIdKey)
                val decisionMap: MutableMap<String, String> = ConcurrentHashMap()
                decisionMap[UserProfileService.variationIdKey] = variationId
                experimentBucketMap[experimentId] = decisionMap
            }
            val userProfileMap: MutableMap<String, Any> = ConcurrentHashMap()
            userProfileMap[UserProfileService.userIdKey] = userId
            userProfileMap[UserProfileService.experimentBucketMapKey] = experimentBucketMap
            userIdToUserProfileMap[userId] = userProfileMap
        }
        return userIdToUserProfileMap
    }

    /**
     * Transform a user profile map to a user profile JSONObject.
     *
     * @param userProfilesMap map with user id as key and experiments variation map from there.
     * @return JSONObject of the user profile service
     * @throws Exception if the json is malformed or the map is malformed, an exception can occur.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun convertMapToJSONObject(userProfilesMap: Map<String, Map<String, Any>>): JSONObject {
        val userProfilesJson = JSONObject()
        for ((_, value) in userProfilesMap) {
            val userProfileMap = value
            val userId = userProfileMap[UserProfileService.userIdKey] as String?
            val experimentBucketMap = userProfileMap[UserProfileService.experimentBucketMapKey] as Map<String, Map<String, String>>?
            val experimentBucketMapJson = JSONObject()
            for ((key, decisionsMap) in experimentBucketMap!!) {
                val decisionJson = JSONObject()
                decisionJson.put(UserProfileService.variationIdKey, decisionsMap[UserProfileService.variationIdKey])
                experimentBucketMapJson.put(key, decisionJson)
            }
            val userProfileJson = JSONObject()
            userProfileJson.put(UserProfileService.userIdKey, userId)
            userProfileJson.put(UserProfileService.experimentBucketMapKey, experimentBucketMapJson)

            // Add user profile to JSONObject of all user profiles.
            userProfilesJson.put(userId, userProfileJson)
        }
        return userProfilesJson
    }
}