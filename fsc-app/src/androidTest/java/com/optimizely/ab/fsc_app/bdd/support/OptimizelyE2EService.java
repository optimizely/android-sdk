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

package com.optimizely.ab.fsc_app.bdd.support;

import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseListenerMethodResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodArrayResponse;
import com.optimizely.ab.fsc_app.bdd.optlyplugins.ProxyEventDispatcher;
import com.optimizely.ab.fsc_app.bdd.models.requests.OptimizelyRequest;
import com.optimizely.ab.fsc_app.bdd.support.resources.*;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.models.responses.ListenerMethodResponse;
import com.optimizely.ab.fsc_app.bdd.optlyplugins.userprofileservices.NoOpService;

import java.lang.reflect.Constructor;
import java.util.*;

import static com.optimizely.ab.fsc_app.bdd.models.Constants.*;
import static com.optimizely.ab.fsc_app.bdd.optlyplugins.TestCompositeService.setupListeners;
import static com.optimizely.ab.fsc_app.bdd.support.Utils.copyResponse;
import static com.optimizely.ab.fsc_app.bdd.support.Utils.parseYAML;

public class OptimizelyE2EService {
    private final static String OPTIMIZELY_PROJECT_ID = "123123";
    private BaseResponse result;
    private OptimizelyManager optimizelyManager;
    private List<Map<String, Object>> notifications = new ArrayList<>();

    public void addNotification(Map<String, Object> notificationMap) {
        notifications.add(notificationMap);
    }

    public List<Map<String, Object>> getNotifications() {
        return notifications;
    }

    public OptimizelyManager getOptimizelyManager() {
        return optimizelyManager;
    }

    public void initializeOptimizely(OptimizelyRequest optimizelyRequest) {
        UserProfileService userProfileService = null;
        if (optimizelyRequest.getUserProfileService() != null) {
            try {
                Class<?> userProfileServiceClass = Class.forName("com.optimizely.ab.fsc_app.bdd.optlyplugins.userprofileservices." + optimizelyRequest.getUserProfileService());
                Constructor<?> serviceConstructor = userProfileServiceClass.getConstructor(ArrayList.class);
                userProfileService = UserProfileService.class.cast(serviceConstructor.newInstance(optimizelyRequest.getUserProfiles()));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        if (userProfileService == null) {
            userProfileService = new NoOpService();
        }

        optimizelyManager = OptimizelyManager.builder(OPTIMIZELY_PROJECT_ID)
                .withEventDispatchInterval(60L * 10L)
                .withEventHandler(optimizelyRequest.getEventHandler())
                .withDatafileDownloadInterval(60L * 10L)
                .withUserProfileService(userProfileService)
                .build(optimizelyRequest.getContext());

        optimizelyManager.initialize(optimizelyRequest.getContext(),
                optimizelyRequest.getDatafile()
        );
        setupListeners(optimizelyRequest.getWithListener(), this);
    }

    public BaseResponse getResult() {
        return result;
    }

    public void callApi(OptimizelyRequest optimizelyRequest) {
        if (optimizelyManager == null) {
            initializeOptimizely(optimizelyRequest);
        }

        Object argumentsObj = parseYAML(optimizelyRequest.getArguments());
        try {
            switch (optimizelyRequest.getApi()) {
                case "activate":
                    result = ActivateResource.getInstance().convertToResourceCall(this, argumentsObj);
                    break;
                case "track":
                    result = TrackResource.getInstance().convertToResourceCall(this, argumentsObj);
                    break;
                case "is_feature_enabled":
                    result = IsFeatureEnabledResource.getInstance().convertToResourceCall(this, argumentsObj);
                    break;
                case "get_variation":
                    result = GetVariationResource.getInstance().convertToResourceCall(this, argumentsObj);
                    break;
                case "get_enabled_features":
                    result = GetEnabledFeaturesResource.getInstance().convertToResourceCall(this, argumentsObj);
                    break;
                case "get_feature_variable_double":
                    result = GetFeatureVariableDoubleResource.getInstance().convertToResourceCall(this, argumentsObj);
                    break;
                case "get_feature_variable_boolean":
                    result = GetFeatureVariableBooleanResource.getInstance().convertToResourceCall(this, argumentsObj);
                    break;
                case "get_feature_variable_integer":
                    result = GetFeatureVariableIntegerResource.getInstance().convertToResourceCall(this, argumentsObj);
                    break;
                case "get_feature_variable_string":
                    result = GetFeatureVariableStringResource.getInstance().convertToResourceCall(this, argumentsObj);
                    break;
                case "get_forced_variation":
                    result = GetForcedVariationResource.getInstance().convertToResourceCall(this, argumentsObj);
                    break;
                case "set_forced_variation":
                    result = ForcedVariationResource.getInstance().convertToResourceCall(this, argumentsObj);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Boolean compareFields(String field, int count, String args) {
        Object parsedArguments = parseYAML(args);
        switch (field) {
            case LISTENER_CALLED:
                return compareListenerCalled(count, parsedArguments);
            case DISPATCHED_EVENTS:
                try {
                    HashMap actualParams = (HashMap) ProxyEventDispatcher.getDispatchedEvents().get(0).get("params");
                    HashMap expectedParams = (HashMap) ((ArrayList) parsedArguments).get(0);
                    return Utils.containsSubset(expectedParams, actualParams);
                } catch (Exception e) {
                    return false;
                }
            case USER_PROFILES:
                try {
                    ArrayList<LinkedHashMap> actualParams = OptlyDataHelper.getUserProfiles(optimizelyManager);
                    ArrayList<LinkedHashMap> expectedParams = (ArrayList) parsedArguments;
                    return Utils.containsSubset(expectedParams, actualParams);
                } catch (Exception e) {
                    return false;
                }
            default:
                return false;
        }
    }

    private Boolean compareListenerCalled(int count, Object parsedArguments) {
        BaseListenerMethodResponse baseListenerMethodResponse;
        if (result instanceof BaseListenerMethodResponse)
            baseListenerMethodResponse = (BaseListenerMethodResponse) result;
        else
            return false;

        if (parsedArguments == baseListenerMethodResponse.getListenerCalled()) {
            return true;
        }

        try {
            Object expectedListenersCalled = copyResponse(count, parsedArguments);
            List actualListenersCalled = new ArrayList(baseListenerMethodResponse.getListenerCalled());
            baseListenerMethodResponse.getListenerCalled().clear();
            return Utils.containsSubset((List) expectedListenersCalled, actualListenersCalled);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return parsedArguments == baseListenerMethodResponse.getListenerCalled();
    }
}
