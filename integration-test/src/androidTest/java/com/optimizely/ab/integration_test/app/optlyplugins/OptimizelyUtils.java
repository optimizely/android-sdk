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

package com.optimizely.ab.integration_test.app.optlyplugins;

import android.support.test.espresso.core.deps.guava.base.CaseFormat;

import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.android.user_profile.DefaultUserProfileService;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.event.BatchEventProcessor;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.EventProcessor;
import com.optimizely.ab.event.ForwardingEventProcessor;
import com.optimizely.ab.integration_test.BuildConfig;
import com.optimizely.ab.integration_test.app.models.ApiOptions;
import com.optimizely.ab.integration_test.app.support.OptimizelyWrapper;
import com.optimizely.ab.integration_test.app.optlyplugins.userprofileservices.TestUserProfileService;
import com.optimizely.ab.notification.ActivateNotification;
import com.optimizely.ab.notification.DecisionNotification;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.notification.TrackNotification;
import com.optimizely.ab.notification.UpdateConfigNotification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.notification.DecisionNotification.FeatureVariableDecisionNotificationBuilder.SOURCE_INFO;

public class OptimizelyUtils {

    public static NotificationCenter getNotificationCenter(List<Map<String, String>> withListener, OptimizelyWrapper optimizelyWrapper) {
        NotificationCenter notificationCenter = new NotificationCenter();

        if (withListener == null) return notificationCenter;

        for (Map<String, String> map : withListener) {
            Object obj = map.get("count");
            int count = Integer.parseInt(obj.toString());

            switch (map.get("type")) {
                case "Activate":
                    for (int i = 0; i < count; i++) {
                        notificationCenter.addNotificationHandler(ActivateNotification.class, activateNotification -> {
                            Map<String, Object> notificationMap = new HashMap<>();
                            notificationMap.put("experiment_key", activateNotification.getExperiment().getKey());
                            notificationMap.put("user_id", activateNotification.getUserId());
                            notificationMap.put("attributes", activateNotification.getAttributes());
                            notificationMap.put("variation_key", activateNotification.getVariation().getKey());
                            optimizelyWrapper.addNotification(notificationMap);
                        });
                    }
                    break;
                case "Track":
                    for (int i = 0; i < count; i++) {
                        notificationCenter.addNotificationHandler(TrackNotification.class, trackNotification -> {
                            Map<String, Object> notificationMap = new HashMap<>();
                            notificationMap.put("event_key", trackNotification.getEventKey());
                            notificationMap.put("user_id", trackNotification.getUserId());
                            notificationMap.put("attributes", trackNotification.getAttributes());
                            notificationMap.put("event_tags", trackNotification.getEventTags());
                            optimizelyWrapper.addNotification(notificationMap);
                        });
                    }
                    break;
                case "Decision":
                    for (int i = 0; i < count; i++) {
                        notificationCenter.addNotificationHandler(DecisionNotification.class, decisionNotification -> {
                            Map<String, Object> notificationMap = new HashMap<>();
                            notificationMap.put("type", decisionNotification.getType());
                            notificationMap.put("user_id", decisionNotification.getUserId());
                            notificationMap.put("attributes", decisionNotification.getAttributes());
                            notificationMap.put("decision_info", convertKeysCamelCaseToSnakeCase(decisionNotification.getDecisionInfo()));
                            optimizelyWrapper.addNotification(notificationMap);
                        });
                    }
                    break;
                case "Config-update":
                    notificationCenter.addNotificationHandler(UpdateConfigNotification.class,
                            configNotification -> {
                                optimizelyWrapper.addNotification(Collections.emptyMap());
                            });
                    break;
            }
        }

        return notificationCenter;
    }

    public static EventProcessor getEventProcessor(ApiOptions apiOptions, EventHandler eventHandler, NotificationCenter notificationCenter) {

        if (apiOptions.getEventOptions() != null) {
            Map<String, Object> eventOptions = apiOptions.getEventOptions();

            BatchEventProcessor.Builder eventProcessorBuilder = BatchEventProcessor.builder()
                    .withEventHandler(eventHandler)
                    .withNotificationCenter(notificationCenter);

            if (eventOptions.get("batch_size") != null) {
                eventProcessorBuilder.withBatchSize((Integer) eventOptions.get("batch_size"));
            }
            if (eventOptions.get("flush_interval") != null) {
                if (eventOptions.get("flush_interval") instanceof Long) {
                    eventProcessorBuilder.withFlushInterval((long) eventOptions.get("flush_interval"));
                } else if (eventOptions.get("flush_interval") instanceof Integer) {
                    if (((Integer) eventOptions.get("flush_interval")) == -1) {
                        eventProcessorBuilder.withFlushInterval((long) 500000);
                    } else {
                        eventProcessorBuilder.withFlushInterval((long) ((Integer) eventOptions.get("flush_interval")));
                    }
                }
            }

            return eventProcessorBuilder.build();
        }
        return new ForwardingEventProcessor(eventHandler, notificationCenter);
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

    public static List<Map<String, Object>> setListenerResponseCollection(OptimizelyWrapper optimizelyWrapper) {
        List<Map<String, Object>> responseCollection = optimizelyWrapper.getNotifications();

        // FCS expects empty arrays to be null :/
        if (responseCollection != null && responseCollection.size() == 0) {
            responseCollection = null;
        }

        return responseCollection;
    }

    public static DatafileConfig getDatafileConfigManager(ApiOptions apiOptions) {
        // If datafile_options is not specified then default to static AtomicProjectConfigManager.
        if (apiOptions.getDatafileOptions() == null) {
            return null;
        }

        if (apiOptions.getDatafile() == null) {
            // @TODO: rename when we rename on the FSC side - this is needed for naming the results files that we upload post test run
            apiOptions.setDatafileName((String) apiOptions.getDatafileOptions().get("sdk_key"));
        }

        String sdkKey = (String) apiOptions.getDatafileOptions().get("sdk_key");

        if (apiOptions.getDatafileOptions().get("datafile_condition") != null) {
            sdkKey += apiOptions.getDatafileOptions().get("datafile_condition");
        }
        String datafileHost = BuildConfig.DATAFILE_HOST;

        String format = "http://" + datafileHost + "/datafiles/%s.json?request_id=" + apiOptions.getRequestId();

        DatafileConfig datafileConfig = new DatafileConfig(OptimizelyWrapper.OPTIMIZELY_PROJECT_ID, sdkKey, format);

        return datafileConfig;
    }


    private static Map<String, ?> convertKeysCamelCaseToSnakeCase(Map<String, ?> decisionInfo) {
        Map<String, Object> decisionInfoCopy = new HashMap<>(decisionInfo);

        if (decisionInfo.containsKey(SOURCE_INFO) && decisionInfo.get(SOURCE_INFO) instanceof Map) {
            Map<String, String> sourceInfo = (Map<String, String>) decisionInfoCopy.get(SOURCE_INFO);
            Map<String, String> sourceInfoCopy = new HashMap<>(sourceInfo);

            for (String key : sourceInfo.keySet()) {
                sourceInfoCopy.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key), sourceInfoCopy.remove(key));
            }
            decisionInfoCopy.remove(SOURCE_INFO);
            decisionInfoCopy.put(SOURCE_INFO, sourceInfoCopy);
        }

        for (String key : decisionInfo.keySet()) {
            decisionInfoCopy.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key), decisionInfoCopy.remove(key));
        }
        return decisionInfoCopy;
    }
}
