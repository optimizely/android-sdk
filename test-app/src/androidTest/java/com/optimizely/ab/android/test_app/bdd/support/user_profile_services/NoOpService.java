package com.optimizely.ab.android.test_app.bdd.support.user_profile_services;

import com.optimizely.ab.bucketing.UserProfileService;

import java.util.Collection;
import java.util.Map;

public class NoOpService implements UserProfileService, TestUserProfileService {
    @Override
    public Collection getUserProfiles() {
        return null;
    }

    @Override
    public Map<String, Object> lookup(String userId) throws Exception {
        return null;
    }

    @Override
    public void save(Map<String, Object> userProfile) throws Exception {

    }
}
