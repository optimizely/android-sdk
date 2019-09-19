package com.optimizely.ab.android.test_app.bdd.support.resources;

import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.test_app.bdd.support.OptimizelyWrapper;
import com.optimizely.ab.android.test_app.bdd.support.response.ListenerMethodArrayResponse;
import com.optimizely.ab.android.test_app.bdd.support.response.ListenerMethodResponse;
import com.optimizely.ab.android.test_app.bdd.support.user_profile_services.TestUserProfileService;
import com.optimizely.ab.android.user_profile.DefaultUserProfileService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class BaseResource<TResponse>  {

    ListenerMethodResponse sendResponse(TResponse result, OptimizelyWrapper optimizelyWrapper) {
        ArrayList<Map<String, Object>> responseCollection = setListenerResponseCollection(optimizelyWrapper);
        return new ListenerMethodResponse<TResponse>(result, responseCollection);
    }

    ListenerMethodArrayResponse sendArrayResponse(List<String> result, OptimizelyWrapper optimizelyWrapper) {
        ArrayList<Map<String, Object>> responseCollection = setListenerResponseCollection(optimizelyWrapper);
        return new ListenerMethodArrayResponse(result, responseCollection);
    }

    public static ArrayList<LinkedHashMap> getUserProfiles(OptimizelyManager optimizely) {
        com.optimizely.ab.bucketing.UserProfileService service = optimizely.getUserProfileService();
        ArrayList<LinkedHashMap> userProfiles = new ArrayList<>();
        if (service.getClass() != DefaultUserProfileService.class) {
            TestUserProfileService testService = (TestUserProfileService) service;
            if(testService.getUserProfiles() != null)
                userProfiles = new ArrayList<LinkedHashMap>(testService.getUserProfiles());
        }
        return userProfiles;
    }

    public static ArrayList<Map<String, Object>> setListenerResponseCollection(OptimizelyWrapper optimizelyWrapper){
        ArrayList<Map<String, Object>> responseCollection = null;

        for (OptimizelyWrapper.ListenerResponse listenerResponse : optimizelyWrapper.getListenerResponses()) {
            if (responseCollection == null) {
                responseCollection = new ArrayList<>();
            }
            Map<String, Object> listenerMap;
            if (listenerResponse.getListenerObject() instanceof Map) {
                listenerMap = (HashMap) listenerResponse.getListenerObject();
                boolean allnull = true;
                for (Object object : listenerMap.values()) {
                    if (object != null) allnull = false;
                }
                if (allnull) {
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
        if (optimizelyWrapper.getOptimizelyManager().getOptimizely().isValid())
            optimizelyWrapper.getOptimizelyManager().getOptimizely().getNotificationCenter().clearAllNotificationListeners();

        return responseCollection;
    }

}
