/****************************************************************************
 * Copyright 2019, Optimizely, Inc. and contributors                        *
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

package com.optimizely.ab.integration_test.app.models;

import android.content.Context;
import android.support.annotation.RawRes;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.integration_test.app.support.OptlyDataHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.android.sdk.OptimizelyManager.loadRawResource;


public class ApiOptions {
    private Context context;
    private String sessionId;
    private String requestId;
    private Integer responseDelay;
    private Map<String, Object> eventOptions;
    private String userProfileService;
    private String datafile;
    private String datafileName;
    private Map<String, ?> datafileOptions;
    private List<Map<String, Object>> dispatchedEvents = new ArrayList<>();
    private ArrayList<Map<String, String>> withListener = new ArrayList<>();
    private ArrayList<Map> userProfiles = new ArrayList<>();
    private String api;
    private String arguments;

    public ApiOptions(Context context) {
        this.context = context;
    }

    public ArrayList<Map> getUserProfiles() {
        return userProfiles;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * This function build a map of userProfile and add it in apiOptions.userProfiles
     *
     * @param userName
     * @param experimentKey To get experimentId from given datafile
     * @param variationKey  To get variationId from given datafile
     */
    public void addUserProfile(String userName, String experimentKey, String variationKey) {
        Experiment experiment = OptlyDataHelper.getExperimentByKey(experimentKey);
        String experimentId = "invalid_experiment";
        if (experiment != null) {
            experimentId = experiment.getId();
        }
        Variation variation = OptlyDataHelper.getVariationByKey(experiment, variationKey);
        String variationId = "invalid_variation";
        if (variation != null) {
            variationId = variation.getId();
        }

        Map<String, Object> userProfile = new HashMap<>();
        boolean foundMap = false;
        for (Map userProfileMap : getUserProfiles()) {
            if (userProfileMap.containsValue(userName)) {
                foundMap = true;
                userProfile = userProfileMap;
                getUserProfiles().remove(userProfileMap);
            }
        }
        Map<String, Object> expBucketMap = new HashMap<>();
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("variation_id", variationId);
        expBucketMap.put(experimentId, varMap);

        if (!foundMap) {
            userProfile.put("user_id", userName);
        } else {
            expBucketMap = (Map<String, Object>) userProfile.get("experiment_bucket_map");
            expBucketMap.put(experimentId, varMap);
        }
        userProfile.put("experiment_bucket_map", expBucketMap);

        userProfiles.add(userProfile);
    }

    public void setResponseDelay(Integer responseDelay) {
        this.responseDelay = responseDelay;
    }

    public Integer getResponseDelay() {
        return responseDelay;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setEventOptions(Map<String, Object> eventOptions) {
        this.eventOptions = eventOptions;
    }

    public void setDatafileOptions(Map<String, ?> datafileOptions) {
        this.datafileOptions = datafileOptions;
    }

    public Map<String, ?> getDatafileOptions() {
        return datafileOptions;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Map<String, Object> getEventOptions() {
        return eventOptions;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getApi() {
        return api;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public String getArguments() {
        return arguments;
    }

    public void setDispatchedEvents(List<Map<String, Object>> dispatchedEvents) {
        this.dispatchedEvents = dispatchedEvents;
    }

    public List<Map<String, Object>> getDispatchedEvents() {
        return dispatchedEvents;
    }

    public List<Map<String, String>> getWithListener() {
        return withListener;
    }

    public Context getContext() {
        return context;
    }

    public String getDatafile() {
        return datafile;
    }

    public String getUserProfileService() {
        return userProfileService;
    }

    public void setUserProfileService(String userProfileService) {
        this.userProfileService = userProfileService;
    }

    public void setDatafileName(String datafileName) {
        this.datafileName = datafileName;
    }

    public String getDatafileName() {
        return datafileName;
    }

    public void setDatafile(String datafileName) {
        try {
            @RawRes Integer intRawResID = context.getResources().
                getIdentifier(datafileName.split("\\.")[0],
                        "raw",
                        context.getPackageName());
            datafile = loadRawResource(context, intRawResID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addWithListener(HashMap<String, String> withListener) {
        this.withListener.add(withListener);
    }
}
