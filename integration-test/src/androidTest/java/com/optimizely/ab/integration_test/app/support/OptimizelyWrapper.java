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

package com.optimizely.ab.integration_test.app.support;

import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.event.EventProcessor;
import com.optimizely.ab.integration_test.app.models.ApiOptions;
import com.optimizely.ab.integration_test.app.optlyplugins.ProxyEventDispatcher;
import com.optimizely.ab.integration_test.app.support.apis.*;
import com.optimizely.ab.integration_test.app.models.responses.BaseResponse;
import com.optimizely.ab.integration_test.app.optlyplugins.userprofileservices.NoOpService;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.notification.UpdateConfigNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.optimizely.ab.integration_test.app.optlyplugins.OptimizelyUtils.getDatafileConfigManager;
import static com.optimizely.ab.integration_test.app.optlyplugins.OptimizelyUtils.getEventProcessor;
import static com.optimizely.ab.integration_test.app.optlyplugins.OptimizelyUtils.getNotificationCenter;
import static com.optimizely.ab.integration_test.app.support.Utils.parseYAML;

public class OptimizelyWrapper {
    Logger logger = LoggerFactory.getLogger(OptimizelyWrapper.class);
    private static final long DEFAULT_BLOCKING_TIMEOUT = 10000;
    private static final long DEFAULT_AWAIT_TIMEOUT = 5000;
    public final static String OPTIMIZELY_PROJECT_ID = "123123";
    private final static long DEFAULT_DATAFILE_DOWNLOAD_INTERVAL = 5000L;

    private OptimizelyManager optimizelyManager;
    private static final Map<String, OptimizelyManager> optimizelyInstanceMap = new HashMap<>();
    private static final Map<String, ProxyEventDispatcher> eventHandlerInstanceMap = new HashMap<>();
    private static final Map<String, List<Map<String, Object>>> notificationsInstanceMap = new HashMap<>();

    private List<Map<String, Object>> notifications = new ArrayList<>();

    public void addNotification(Map<String, Object> notificationMap) {
        notifications.add(notificationMap);
    }

    private void setNotifications(List<Map<String, Object>> notifications) {
        this.notifications = notifications;
    }

    public List<Map<String, Object>> getNotifications() {
        return notifications;
    }

    public OptimizelyManager getOptimizelyManager() {
        return optimizelyManager;
    }

    public void initializeOptimizely(ApiOptions apiOptions) {
        String sessionId = apiOptions.getSessionId();
        if (sessionId != null && !sessionId.isEmpty() && optimizelyInstanceMap.containsKey(sessionId)) {
            // Use cached instance / eventHandler
            optimizelyManager = optimizelyInstanceMap.get(sessionId);
            if (eventHandlerInstanceMap.containsKey(apiOptions.getSessionId()))
                apiOptions.setDispatchedEvents(eventHandlerInstanceMap.get(apiOptions.getSessionId()).getDispatchedEvents());

            if (notificationsInstanceMap.containsKey(sessionId)) {
                setNotifications(notificationsInstanceMap.get(sessionId));
            }
        } else {
            UserProfileService userProfileService = null;
            if (apiOptions.getUserProfileService() != null) {
                try {
                    Class<?> userProfileServiceClass = Class.forName("com.optimizely.ab.integration_test.app.optlyplugins.userprofileservices." + apiOptions.getUserProfileService());
                    Constructor<?> serviceConstructor = userProfileServiceClass.getConstructor(ArrayList.class);
                    userProfileService = UserProfileService.class.cast(serviceConstructor.newInstance(apiOptions.getUserProfiles()));
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
            if (userProfileService == null) {
                userProfileService = new NoOpService();
            }
            if (apiOptions.getDatafile() == null) {
                if (apiOptions.getDatafileName() == null)
                    apiOptions.setDatafileName("datafile.json");
                apiOptions.setDatafile(apiOptions.getDatafileName());
            }

            NotificationCenter notificationCenter = getNotificationCenter(apiOptions.getWithListener(), this);
            ProxyEventDispatcher eventHandler = new ProxyEventDispatcher(apiOptions.getDispatchedEvents());
            DatafileConfig datafileConfig = getDatafileConfigManager(apiOptions);
            EventProcessor eventProcessor = getEventProcessor(apiOptions, eventHandler, notificationCenter);
            Integer updateInterval = null;
            if (apiOptions.getDatafileOptions() != null) {
                updateInterval = (Integer) apiOptions.getDatafileOptions().get("update_interval");
            }
            optimizelyManager = OptimizelyManager.builder(OPTIMIZELY_PROJECT_ID)
                    .withEventHandler(eventHandler)
                    .withNotificationCenter(notificationCenter)
                    .withEventProcessor(eventProcessor)
                    .withUserProfileService(userProfileService)
                    .withDatafileConfig(datafileConfig)
                    .withDatafileDownloadInterval(updateInterval == null ? DEFAULT_DATAFILE_DOWNLOAD_INTERVAL : updateInterval.longValue())
                    .build(apiOptions.getContext());

            optimizelyManager.initialize(apiOptions.getContext(),
                    apiOptions.getDatafile()
            );
            if (apiOptions.getDatafileOptions() != null) {

                String mode = (String) apiOptions.getDatafileOptions().get("mode");
                Integer timeout = (Integer) apiOptions.getDatafileOptions().get("timeout");
                Integer revision = (Integer) apiOptions.getDatafileOptions().get("revision");
                revision = revision == null ? 1 : revision;
                revision += apiOptions.getDatafile() == null ? 0 : 1;

                long blockingTimeout = DEFAULT_BLOCKING_TIMEOUT;
                long awaitTimeout = DEFAULT_AWAIT_TIMEOUT;
                CountDownLatch countDownLatch = new CountDownLatch(revision);
                notificationCenter.addNotificationHandler(UpdateConfigNotification.class, x -> {
                    System.out.println("Making request for id: " + apiOptions.getRequestId());
                    countDownLatch.countDown();
                });

                switch (mode == null ? "null" : mode) {
                    case "wait_for_on_ready":
                        blockingTimeout = timeout == null ? blockingTimeout : timeout.longValue();
                        try {
                            countDownLatch.await(blockingTimeout, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            logger.warn("Timeout expired waiting for update config to be triggered. CountDownLatch: {}", countDownLatch);
                        }
                        break;
                    case "wait_for_config_update":
                        awaitTimeout = timeout == null ? awaitTimeout : timeout.longValue();
                        try {
                            countDownLatch.await(awaitTimeout, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            logger.warn("Timeout expired waiting for update config to be triggered. CountDownLatch: {}", countDownLatch);
                        }
                        break;
                    default:
                        countDownLatch.countDown();
                }
                OptlyDataHelper.initializeProjectConfig(optimizelyManager.getOptimizely().getProjectConfig());
            }
            if (sessionId != null && !sessionId.isEmpty()) {
                optimizelyInstanceMap.put(sessionId, optimizelyManager);
                eventHandlerInstanceMap.put(sessionId, eventHandler);
                notificationsInstanceMap.put(sessionId, getNotifications());
            }
        }
    }

    public BaseResponse callApi(ApiOptions apiOptions) {
        if (optimizelyManager == null) {
            initializeOptimizely(apiOptions);
        }

        Object argumentsObj = parseYAML(apiOptions.getArguments());
        try {
            switch (apiOptions.getApi()) {
                case "activate":
                    return new ActivateAPICall().invokeAPI(this, argumentsObj);
                case "track":
                    return new TrackAPICall().invokeAPI(this, argumentsObj);
                case "is_feature_enabled":
                    return new IsFeatureEnabledAPICall().invokeAPI(this, argumentsObj);
                case "get_variation":
                    return new GetVariationAPICall().invokeAPI(this, argumentsObj);
                case "get_enabled_features":
                    return new GetEnabledFeaturesAPICall().invokeAPI(this, argumentsObj);
                case "get_feature_variable_double":
                    return new GetFeatureVariableDoubleAPICall().invokeAPI(this, argumentsObj);
                case "get_feature_variable_boolean":
                    return new GetFeatureVariableBooleanAPICall().invokeAPI(this, argumentsObj);
                case "get_feature_variable_integer":
                    return new GetFeatureVariableIntegerAPICall().invokeAPI(this, argumentsObj);
                case "get_feature_variable_string":
                    return new GetFeatureVariableStringAPICall().invokeAPI(this, argumentsObj);
                case "get_forced_variation":
                    return new GetForcedVariationAPICall().invokeAPI(this, argumentsObj);
                case "set_forced_variation":
                    return new ForcedVariationAPICall().invokeAPI(this, argumentsObj);
                case "close":
                    return new CloseAPICall().invokeAPI(this, argumentsObj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
