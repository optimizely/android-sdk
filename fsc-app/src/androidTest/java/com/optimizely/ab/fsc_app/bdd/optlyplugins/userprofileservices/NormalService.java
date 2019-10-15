package com.optimizely.ab.fsc_app.bdd.optlyplugins.userprofileservices;

import com.optimizely.ab.bucketing.UserProfileService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class NormalService implements UserProfileService, TestUserProfileService {
    public Map<String, Map<String, Object>> userProfiles = new HashMap<>();

    public NormalService(ArrayList<Map> userProfileList) {
        if (userProfileList != null) {
            for (Map userProfile : userProfileList) {
                String userId = userProfile.get("user_id").toString();
                userProfiles.put(userId, userProfile);
            }
        }
    }

    public NormalService() {}

    public Map<String, Object> lookup(String userId) throws Exception {
        return userProfiles.get(userId);
    }

    public void save(Map<String, Object> userProfile) throws Exception {
        String userId = userProfile.get("user_id").toString();
        userProfiles.put(userId, userProfile);
    }

    public Collection<Map<String,Object>> getUserProfiles() {
        return userProfiles.values();
    }
}
