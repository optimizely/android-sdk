package com.optimizely.ab.fsc_app.bdd.support.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        ArrayList<Map<String, Object>> responseCollection = setListenerResponseCollection(optimizelyE2EService);
        return new ListenerMethodResponse<TResponse>(result, responseCollection);
    }

    ListenerMethodArrayResponse sendArrayResponse(List<String> result, OptimizelyE2EService optimizelyE2EService) {
        ArrayList<Map<String, Object>> responseCollection = setListenerResponseCollection(optimizelyE2EService);
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

    public static ArrayList<Map<String, Object>> setListenerResponseCollection(OptimizelyE2EService optimizelyE2EService) {
        ArrayList<Map<String, Object>> responseCollection = null;

        for (ListenerResponse listenerResponse : optimizelyE2EService.getListenerResponses()) {
            if (responseCollection == null) {
                responseCollection = new ArrayList<>();
            }
            Map<String, Object> listenerMap;
            if (listenerResponse.getListenerObject() instanceof Map) {
                listenerMap = (HashMap) listenerResponse.getListenerObject();
                boolean allNull = true;
                for (Object object : listenerMap.values()) {
                    if (object != null) allNull = false;
                }
                if (allNull) {
                    continue;
                }
                responseCollection.add(listenerMap);
            } else if (listenerResponse.getListenerObject() instanceof ArrayList) {
                responseCollection.addAll((ArrayList) listenerResponse.getListenerObject());
            }
        }
        if (responseCollection != null && responseCollection.size() == 0) {
            responseCollection = null;
        }
        //TODO: remove if unnecessary
//        if (optimizelyWrapper.getOptimizelyManager().getOptimizely().isValid())
//            optimizelyWrapper.getOptimizelyManager().getOptimizely().getNotificationCenter().clearAllNotificationListeners();

        return responseCollection;
    }

}
