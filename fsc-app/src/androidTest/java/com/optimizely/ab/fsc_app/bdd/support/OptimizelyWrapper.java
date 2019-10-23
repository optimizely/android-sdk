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
import com.optimizely.ab.fsc_app.bdd.models.ApiOptions;
import com.optimizely.ab.fsc_app.bdd.support.resources.*;
import com.optimizely.ab.fsc_app.bdd.models.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.optlyplugins.userprofileservices.NoOpService;

import java.lang.reflect.Constructor;
import java.util.*;

import static com.optimizely.ab.fsc_app.bdd.optlyplugins.TestCompositeService.setupListeners;
import static com.optimizely.ab.fsc_app.bdd.support.Utils.parseYAML;

public class OptimizelyWrapper {
    private final static String OPTIMIZELY_PROJECT_ID = "123123";
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

    public void initializeOptimizely(ApiOptions apiOptions) {
        UserProfileService userProfileService = null;
        if (apiOptions.getUserProfileService() != null) {
            try {
                Class<?> userProfileServiceClass = Class.forName("com.optimizely.ab.fsc_app.bdd.optlyplugins.userprofileservices." + apiOptions.getUserProfileService());
                Constructor<?> serviceConstructor = userProfileServiceClass.getConstructor(ArrayList.class);
                userProfileService = UserProfileService.class.cast(serviceConstructor.newInstance(apiOptions.getUserProfiles()));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        if (userProfileService == null) {
            userProfileService = new NoOpService();
        }

        optimizelyManager = OptimizelyManager.builder(OPTIMIZELY_PROJECT_ID)
                .withEventHandler(apiOptions.getEventHandler())
                .withUserProfileService(userProfileService)
                .build(apiOptions.getContext());

        optimizelyManager.initialize(apiOptions.getContext(),
                apiOptions.getDatafile()
        );
        setupListeners(apiOptions.getWithListener(), this);
    }

    public BaseResponse callApi(ApiOptions apiOptions) {
        if (optimizelyManager == null) {
            initializeOptimizely(apiOptions);
        }

        Object argumentsObj = parseYAML(apiOptions.getArguments());
        try {
            switch (apiOptions.getApi()) {
                case "activate":
                    return ActivateResource.getInstance().parseToCallApi(this, argumentsObj);
                case "track":
                    return TrackResource.getInstance().parseToCallApi(this, argumentsObj);
                case "is_feature_enabled":
                    return IsFeatureEnabledResource.getInstance().parseToCallApi(this, argumentsObj);
                case "get_variation":
                    return GetVariationResource.getInstance().parseToCallApi(this, argumentsObj);
                case "get_enabled_features":
                    return GetEnabledFeaturesResource.getInstance().parseToCallApi(this, argumentsObj);
                case "get_feature_variable_double":
                    return GetFeatureVariableDoubleResource.getInstance().parseToCallApi(this, argumentsObj);
                case "get_feature_variable_boolean":
                    return GetFeatureVariableBooleanResource.getInstance().parseToCallApi(this, argumentsObj);
                case "get_feature_variable_integer":
                    return GetFeatureVariableIntegerResource.getInstance().parseToCallApi(this, argumentsObj);
                case "get_feature_variable_string":
                    return GetFeatureVariableStringResource.getInstance().parseToCallApi(this, argumentsObj);
                case "get_forced_variation":
                    return GetForcedVariationResource.getInstance().parseToCallApi(this, argumentsObj);
                case "set_forced_variation":
                    return ForcedVariationResource.getInstance().parseToCallApi(this, argumentsObj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
