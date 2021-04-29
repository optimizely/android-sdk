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

import android.annotation.TargetApi
import android.os.AsyncTask
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.optimizely.ab.android.shared.Cache
import com.optimizely.ab.bucketing.UserProfileService
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import kotlin.collections.HashMap

/**
 * Stores a map of user IDs to [com.optimizely.ab.bucketing.UserProfile] with write-back to a file.
 */
internal class UserProfileCache(@field:VisibleForTesting val diskCache: DiskCache, private val logger: Logger,
                                private val memoryCache: HashMap<String, Map<String, Any>>,
                                private val legacyDiskCache: LegacyDiskCache) {
    /**
     * Clear the in-memory and disk caches of all entries.
     */
    fun clear() {
        memoryCache.clear()
        diskCache.save(memoryCache)
        logger.info("User profile cache cleared.")
    }

    /**
     * Lookup a user profile map in the cache by user ID.
     *
     * @param userId the user ID of the user profile
     * @return user profile from the cache if found
     */
    fun lookup(userId: String?): Map<String, Any>? {
        if (userId == null) {
            logger.error("Unable to lookup user profile because user ID was null.")
            return null
        } else if (userId.isEmpty()) {
            logger.error("Unable to lookup user profile because user ID was empty.")
            return null
        }
        return memoryCache[userId]
    }

    /**
     * Migrate legacy user profiles if found.
     *
     *
     * Note: this will overwrite a newer `UserProfile` cache in the unlikely event that a legacy cache and new cache
     * both exist on disk.
     */
    @VisibleForTesting
    fun migrateLegacyUserProfiles() {
        val legacyUserProfilesJson = legacyDiskCache.load()
        if (legacyUserProfilesJson == null) {
            logger.info("No legacy user profiles to migrate.")
            return
        }
        try {
            val userIdIterator = legacyUserProfilesJson.keys()
            while (userIdIterator.hasNext()) {
                val userId = userIdIterator.next()
                val legacyUserProfileJson = legacyUserProfilesJson.getJSONObject(userId)
                val experimentBucketMap: MutableMap<String, Map<String, String>> = ConcurrentHashMap()
                val experimentIdIterator = legacyUserProfileJson.keys()
                while (experimentIdIterator.hasNext()) {
                    val experimentId = experimentIdIterator.next()
                    val variationId = legacyUserProfileJson.getString(experimentId)
                    val decisionMap: MutableMap<String, String> = ConcurrentHashMap()
                    decisionMap[UserProfileService.variationIdKey] = variationId
                    experimentBucketMap[experimentId] = decisionMap
                }
                val userProfileMap: MutableMap<String, Any> = ConcurrentHashMap()
                userProfileMap[UserProfileService.userIdKey] = userId
                userProfileMap[UserProfileService.experimentBucketMapKey] = experimentBucketMap
                save(userProfileMap)
            }
        } catch (e: JSONException) {
            logger.warn("Unable to deserialize legacy user profiles. Will delete legacy user profile cache file.", e)
        } finally {
            legacyDiskCache.delete()
        }
    }

    /**
     * Remove a user profile.
     *
     * @param userId the user ID of the user profile to remove
     */
    fun remove(userId: String?) {
        if (userId == null) {
            logger.error("Unable to remove user profile because user ID was null.")
        } else if (userId.isEmpty()) {
            logger.error("Unable to remove user profile because user ID was empty.")
        } else {
            if (memoryCache.containsKey(userId)) {
                memoryCache.remove(userId)
                diskCache.save(memoryCache)
                logger.info("Removed user profile for {}.", userId)
            }
        }
    }

    /**
     * Remove a decision from a user profile.
     *
     * @param userId the user ID of the decision to remove
     * @param experimentId the experiment ID of the decision to remove
     */
    fun remove(userId: String?, experimentId: String?) {
        if (userId == null) {
            logger.error("Unable to remove decision because user ID was null.")
        } else if (userId.isEmpty()) {
            logger.error("Unable to remove decision because user ID was empty.")
        } else if (experimentId == null) {
            logger.error("Unable to remove decision because experiment ID was null.")
        } else if (experimentId.isEmpty()) {
            logger.error("Unable to remove decision because experiment ID was empty.")
        } else {
            val userProfileMap = memoryCache[userId]
            if (userProfileMap != null) {
                val experimentBucketMap: MutableMap<String, Map<String, String>>? = userProfileMap[UserProfileService.experimentBucketMapKey] as ConcurrentHashMap<String, Map<String, String>>?
                if (experimentBucketMap!!.containsKey(experimentId)) {
                    experimentBucketMap.remove(experimentId)
                    diskCache.save(memoryCache)
                    logger.info("Removed decision for experiment {} from user profile for {}.", experimentId, userId)
                }
            }
        }
    }

    /**
     * Remove experiments that are no longer valid
     * @param validExperimentIds list of valid experiment ids.
     */
    fun removeInvalidExperiments(validExperimentIds: Set<String?>) {
        for (userId in memoryCache.keys) {
            val maps = memoryCache[userId]!!
            val experimentBucketMap: MutableMap<String, Map<String, String>>? = maps[UserProfileService.experimentBucketMapKey] as ConcurrentHashMap<String, Map<String, String>>?
            if (experimentBucketMap != null && experimentBucketMap.keys.size > 100) {
                for (experimentId in experimentBucketMap.keys) {
                    if (!validExperimentIds.contains(experimentId)) {
                        experimentBucketMap.remove(experimentId)
                    }
                }
            }
        }
        diskCache.save(memoryCache)
    }

    /**
     * Add a decision to a user profile.
     *
     * @param userProfileMap map representation of user profile
     */
    fun save(userProfileMap: Map<String, Any>) {
        val userId = userProfileMap[UserProfileService.userIdKey] as String?
        if (userId == null) {
            logger.error("Unable to save user profile because user ID was null.")
        } else if (userId.isEmpty()) {
            logger.error("Unable to save user profile because user ID was empty.")
        } else {
            memoryCache[userId] = userProfileMap
            diskCache.save(memoryCache)
            logger.info("Saved user profile for {}.", userId)
        }
    }

    /**
     * Load the cache from disk to memory.
     */
    fun start() {
        // Migrate legacy user profiles if found.
        migrateLegacyUserProfiles()
        try {
            val userProfilesJson = diskCache.load()
            val userProfilesMap = UserProfileCacheUtils.convertJSONObjectToMap(userProfilesJson)
            memoryCache.clear()
            memoryCache.putAll(userProfilesMap)
            logger.info("Loaded user profile cache from disk.")
        } catch (e: Exception) {
            clear()
            logger.error("Unable to parse user profile cache from disk.", e)
        }
    }

    /**
     * Write-through cache persisted on disk.
     */
    internal class DiskCache(private val cache: Cache, private val executor: Executor, private val logger: Logger,
                             private val projectId: String) {
        val fileName: String
            get() = String.format(FILE_NAME, projectId)

        @Throws(JSONException::class)
        fun load(): JSONObject {
            val cacheString = cache.load(fileName)
            if (cacheString == null) {
                logger.warn("Unable to load user profile cache from disk.")
                return JSONObject()
            }
            return JSONObject(cacheString)
        }

        /**
         * Save the in-memory cache to disk in a background thread.
         */
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        fun save(userProfilesMap: Map<String, Map<String, Any>>) {
            val task: AsyncTask<Void?, Void?, Boolean> = object : AsyncTask<Void?, Void?, Boolean>() {
                override fun doInBackground(params: Array<Void?>): Boolean {
                    val userProfilesJson: JSONObject
                    userProfilesJson = try {
                        UserProfileCacheUtils.convertMapToJSONObject(userProfilesMap)
                    } catch (e: Exception) {
                        logger.error("Unable to serialize user profiles to save to disk.", e)
                        return false
                    }

                    // Write to disk.
                    val saved = cache.save(fileName, userProfilesJson.toString())
                    if (saved) {
                        logger.info("Saved user profiles to disk.")
                    } else {
                        logger.warn("Unable to save user profiles to disk.")
                    }
                    return saved
                }
            }
            task.executeOnExecutor(executor)
        }

        companion object {
            private const val FILE_NAME = "optly-user-profile-service-%s.json"
        }
    }

    /**
     * Stores a map of userIds to a map of expIds to variationIds in a file.
     *
     */
    @Deprecated("This class is only used to migrate legacy user profiles to the new {@link UserProfileCache}.")
    internal class LegacyDiskCache(private val cache: Cache, private val executor: Executor, private val logger: Logger,
                                   private val projectId: String) {
        @get:VisibleForTesting
        val fileName: String
            get() = String.format(FILE_NAME, projectId)

        /**
         * Load legacy user profiles from disk if found.
         */
        fun load(): JSONObject? {
            val cacheString = cache.load(fileName)
            if (cacheString == null) {
                logger.info("Legacy user profile cache not found.")
                return null
            }
            return try {
                JSONObject(cacheString)
            } catch (e: JSONException) {
                logger.warn("Unable to parse legacy user profiles. Will delete legacy user profile cache file.", e)
                delete()
                null
            }
        }

        /**
         * Delete the legacy user profile cache from disk in a background thread.
         */
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        fun delete() {
            val task: AsyncTask<Void?, Void?, Boolean> = object : AsyncTask<Void?, Void?, Boolean>() {
                override fun doInBackground(params: Array<Void?>): Boolean {
                    val deleted = cache.delete(fileName)
                    if (deleted) {
                        logger.info("Deleted legacy user profile from disk.")
                    } else {
                        logger.warn("Unable to delete legacy user profile from disk.")
                    }
                    return deleted
                }
            }
            task.executeOnExecutor(executor)
        }

        companion object {
            private const val FILE_NAME = "optly-user-profile-%s.json"
        }
    }
}