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
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import com.optimizely.ab.android.shared.Cache
import com.optimizely.ab.android.user_profile.UserProfileCache.DiskCache
import com.optimizely.ab.android.user_profile.UserProfileCache.LegacyDiskCache
import com.optimizely.ab.bucketing.UserProfileService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Android implementation of [UserProfileService]
 *
 *
 * Makes bucketing sticky. This module is what allows the SDK
 * to know if a user has already been bucketed for an experiment.
 * Once a user is bucketed they will stay bucketed unless the device's
 * storage is cleared. Bucketing information is stored in a simple file.
 */
class DefaultUserProfileService internal constructor(private val userProfileCache: UserProfileCache, private val logger: Logger) : UserProfileService {
    interface StartCallback {
        fun onStartComplete(userProfileService: UserProfileService?)
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    fun startInBackground(callback: StartCallback?) {
        val userProfileService = this
        val initUserProfileTask = object : AsyncTask<Void?, Void?, UserProfileService>() {
            override fun doInBackground(params: Array<Void?>): UserProfileService {
                userProfileService.start()
                return userProfileService
            }

            override fun onPostExecute(userProfileService: UserProfileService) {
                callback?.onStartComplete(userProfileService)
            }
        }
        try {
            initUserProfileTask.executeOnExecutor(Executors.newSingleThreadExecutor())
        } catch (e: Exception) {
            logger.error("Error loading user profile service from AndroidUserProfileServiceDefault")
            callback!!.onStartComplete(null)
        }
    }

    /**
     * Load the cache from disk to memory.
     */
    fun start() {
        userProfileCache.start()
    }

    /**
     * @param userId the user ID of the user profile
     * @return user profile from the cache if found
     * @see UserProfileService.lookup
     */
    override fun lookup(userId: String): Map<String, Any>? {
        if (userId == null) {
            logger.error("Received null user ID, unable to lookup activation.")
            return null
        } else if (userId.isEmpty()) {
            logger.error("Received empty user ID, unable to lookup activation.")
            return null
        }
        return userProfileCache.lookup(userId)
    }

    /**
     * Remove a user profile.
     *
     * @param userId the user ID of the decision to remove
     */
    fun remove(userId: String?) {
        userProfileCache.remove(userId)
    }

    fun removeInvalidExperiments(validExperiments: Set<String?>) {
        try {
            userProfileCache.removeInvalidExperiments(validExperiments)
        } catch (e: Exception) {
            logger.error("Error calling userProfileCache to remove invalid experiments", e)
        }
    }

    /**
     * Remove a decision from a user profile.
     *
     * @param userId the user ID of the decision to remove
     * @param experimentId the experiment ID of the decision to remove
     */
    fun remove(userId: String?, experimentId: String?) {
        userProfileCache.remove(userId, experimentId)
    }

    /**
     * @param userProfileMap map representation of user profile
     * @see UserProfileService.save
     */
    override fun save(userProfileMap: Map<String, Any>) {
        userProfileCache.save(userProfileMap)
    }

    companion object {
        /**
         * Gets a new instance of [DefaultUserProfileService].
         *
         * @param projectId your project's id
         * @param context   an instance of [Context]
         * @return the instance as [UserProfileService]
         */
        @JvmStatic
        fun newInstance(projectId: String, context: Context): UserProfileService {
            val userProfileCache = UserProfileCache(
                    DiskCache(Cache(context, LoggerFactory.getLogger(Cache::class.java)),
                            Executors.newSingleThreadExecutor(), LoggerFactory.getLogger(DiskCache::class.java),
                            projectId),
                    LoggerFactory.getLogger(UserProfileCache::class.java),
                    ConcurrentHashMap(),
                    LegacyDiskCache(Cache(context, LoggerFactory.getLogger(Cache::class.java)),
                            Executors.newSingleThreadExecutor(),
                            LoggerFactory.getLogger(LegacyDiskCache::class.java), projectId))
            return DefaultUserProfileService(userProfileCache,
                    LoggerFactory.getLogger(DefaultUserProfileService::class.java))
        }
    }
}