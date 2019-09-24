package com.optimizely.ab.fsc_app.bdd.support.userprofileservices;

import com.optimizely.ab.bucketing.UserProfileService;

import java.util.Collection;

public interface TestUserProfileService extends UserProfileService {
    Collection getUserProfiles();
}
