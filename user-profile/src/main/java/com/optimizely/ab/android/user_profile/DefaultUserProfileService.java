/****************************************************************************
 * Copyright 2017,2021, Optimizely, Inc. and contributors                   *
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

import android.os.Build;
import android.content.Context;
import android.os.AsyncTask;
import android.annotation.TargetApi;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.bucketing.UserProfileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Android implementation of {@link UserProfileService}
 * <p>
 * Makes bucketing sticky. This module is what allows the SDK
 * to know if a user has already been bucketed for an experiment.
 * Once a user is bucketed they will stay bucketed unless the device's
 * storage is cleared. Bucketing information is stored in a simple file.
 */
public class DefaultUserProfileService implements UserProfileService {

    @NonNull private final UserProfileCache userProfileCache;
    @NonNull private final Logger logger;

    DefaultUserProfileService(@NonNull UserProfileCache userProfileCache, @NonNull Logger logger) {
        this.userProfileCache = userProfileCache;
        this.logger = logger;
    }


    /**
     * Gets a new instance of {@link DefaultUserProfileService}.
     *
     * @param projectId your project's id
     * @param context   an instance of {@link Context}
     * @return the instance as {@link UserProfileService}
     */
    public static UserProfileService newInstance(@NonNull String projectId, @NonNull Context context) {
        UserProfileCache userProfileCache = new UserProfileCache(
                new UserProfileCache.DiskCache(new Cache(context, LoggerFactory.getLogger(Cache.class)),
                        Executors.newSingleThreadExecutor(), LoggerFactory.getLogger(UserProfileCache.DiskCache.class),
                        projectId),
                LoggerFactory.getLogger(UserProfileCache.class),
                new ConcurrentHashMap<String, Map<String, Object>>(),
                new UserProfileCache.LegacyDiskCache(new Cache(context, LoggerFactory.getLogger(Cache.class)),
                        Executors.newSingleThreadExecutor(),
                        LoggerFactory.getLogger(UserProfileCache.LegacyDiskCache.class), projectId));

        return new DefaultUserProfileService(userProfileCache,
                LoggerFactory.getLogger(DefaultUserProfileService.class));
    }

    public interface StartCallback {
        void onStartComplete(UserProfileService userProfileService);
    }

    public void startInBackground(final StartCallback callback) {
                final DefaultUserProfileService userProfileService = this;

                AsyncTask<Void, Void, UserProfileService> initUserProfileTask = new AsyncTask<Void, Void, UserProfileService>() {
            @Override
            protected UserProfileService doInBackground(Void[] params) {
                                userProfileService.start();
                                return userProfileService;
            }
            @Override
            protected void onPostExecute(UserProfileService userProfileService) {
                if (callback != null) {
                    callback.onStartComplete(userProfileService);
                }
            }
        };

        try {
            initUserProfileTask.executeOnExecutor(Executors.newSingleThreadExecutor());
        }
        catch (Exception e) {
            logger.error("Error loading user profile service from AndroidUserProfileServiceDefault");
            callback.onStartComplete(null);
        }

    }

    /**
     * Load the cache from disk to memory.
     */
    public void start() {
        userProfileCache.start();
    }

    /**
     * @param userId the user ID of the user profile
     * @return user profile from the cache if found
     * @see UserProfileService#lookup(String)
     */
    @Override
    @Nullable
    public Map<String, Object> lookup(String userId) {
        if (userId == null) {
            logger.error("Received null user ID, unable to lookup activation.");
            return null;
        } else if (userId.isEmpty()) {
            logger.error("Received empty user ID, unable to lookup activation.");
            return null;
        }
        return userProfileCache.lookup(userId);
    }

    /**
     * Remove a user profile.
     *
     * @param userId the user ID of the decision to remove
     */
    public void remove(String userId) {
        userProfileCache.remove(userId);
    }

    public void removeInvalidExperiments(Set<String> validExperiments) {
        try {
            userProfileCache.removeInvalidExperiments(validExperiments);
        }
        catch (Exception e) {
            logger.error("Error calling userProfileCache to remove invalid experiments", e);
        }
    }
    /**
     * Remove a decision from a user profile.
     *
     * @param userId the user ID of the decision to remove
     * @param experimentId the experiment ID of the decision to remove
     */
    public void remove(String userId, String experimentId) {
        userProfileCache.remove(userId, experimentId);
    }

    /**
     * @param userProfileMap map representation of user profile
     * @see UserProfileService#save(Map)
     */
    @Override
    public void save(Map<String, Object> userProfileMap) {
        userProfileCache.save(userProfileMap);
    }
}
