package com.optimizely.ab.android.user_profile;

import android.content.Context;
import android.support.annotation.NonNull;

import com.optimizely.ab.bucketing.UserProfileService;

/**
 * Android implementation of {@link UserProfileService}
 * <p>
 * Makes bucketing sticky. This module is what allows the SDK
 * to know if a user has already been bucketed for an experiment.
 * Once a user is bucketed they will stay bucketed unless the device's
 * storage is cleared. Bucketing information is stored in a simple file.
 */

public interface AndroidUserProfileService extends UserProfileService {
    /**
     *
     */
    public void start();

    public void remove(String userId);

    public void remove(String userId, String experimentId);

    public AndroidUserProfileService getNewInstance(@NonNull String projectId, @NonNull Context context);
}
