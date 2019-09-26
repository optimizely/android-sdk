package com.optimizely.ab.fsc_app.bdd.support.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizely.ab.Optimizely;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.user_profile.DefaultUserProfileService;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.fsc_app.bdd.OptimizelyE2EService;
import com.optimizely.ab.fsc_app.bdd.support.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.support.responses.ListenerMethodArrayResponse;
import com.optimizely.ab.fsc_app.bdd.support.responses.ListenerMethodResponse;
import com.optimizely.ab.fsc_app.bdd.support.responses.ListenerResponse;
import com.optimizely.ab.fsc_app.bdd.support.userprofileservices.TestUserProfileService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BaseResource<TResponse> {

    protected ObjectMapper mapper;

    protected BaseResource() {
        mapper = new ObjectMapper();
    }

    ListenerMethodResponse sendResponse(TResponse result, OptimizelyE2EService optimizelyE2EService) {
        List<Map<String, Object>> responseCollection = setListenerResponseCollection(optimizelyE2EService);
        return new ListenerMethodResponse<TResponse>(result, responseCollection);
    }

    ListenerMethodArrayResponse sendArrayResponse(List<String> result, OptimizelyE2EService optimizelyE2EService) {
        List<Map<String, Object>> responseCollection = setListenerResponseCollection(optimizelyE2EService);
        return new ListenerMethodArrayResponse(result, responseCollection);
    }

    public static ArrayList<LinkedHashMap> getUserProfiles(OptimizelyManager optimizely) {
        UserProfileService service = optimizely.getUserProfileService();
        ArrayList<LinkedHashMap> userProfiles = new ArrayList<>();
        if (service.getClass() != DefaultUserProfileService.class) {
            TestUserProfileService testService = (TestUserProfileService) service;
            if (testService.getUserProfiles() != null)
                userProfiles = new ArrayList<LinkedHashMap>(testService.getUserProfiles());
        }
        return userProfiles;
    }

    private List<Map<String, Object>> setListenerResponseCollection(OptimizelyE2EService optimizelyE2EService) {
        List<Map<String, Object>> responseCollection = optimizelyE2EService.getNotifications();

        // FCS expects empty arrays to be null :/
        if (responseCollection != null && responseCollection.size() == 0) {
            responseCollection = null;
        }

        return responseCollection;
    }

}
