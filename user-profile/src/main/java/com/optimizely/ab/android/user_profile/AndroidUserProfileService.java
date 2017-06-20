package com.optimizely.ab.android.user_profile;

import android.content.Context;
import android.support.annotation.NonNull;

import com.optimizely.ab.bucketing.UserProfileService;

/**
 * Created by tzurkan on 6/20/17.
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
