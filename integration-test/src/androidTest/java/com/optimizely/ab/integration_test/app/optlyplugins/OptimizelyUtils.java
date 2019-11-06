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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.optimizely.ab.integration_test.app.support.OptlyDataHelper.initializeProjectConfig;
import static com.optimizely.ab.notification.DecisionNotification.FeatureVariableDecisionNotificationBuilder.SOURCE_INFO;

public class OptimizelyUtils {
    private static final Logger logger = LoggerFactory.getLogger(OptimizelyUtils.class);
    private static final long DEFAULT_BLOCKING_TIMEOUT = 3000;

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

    public static DatafileConfig getDatafileConfigManager(ApiOptions apiOptions, NotificationCenter notificationCenter) {
        // If datafile_options is not specified then default to static AtomicProjectConfigManager.
        if (apiOptions.getDatafileOptions() == null) {
            return null;
        }

        if (apiOptions.getDatafile() == null) {
            // @TODO: rename when we rename on the FSC side - this is needed for naming the results files that we upload post test run
            apiOptions.setDatafile((String) apiOptions.getDatafileOptions().get("sdk_key"));
            initializeProjectConfig(apiOptions.getDatafile());
        }

        String sdkKey = (String) apiOptions.getDatafileOptions().get("sdk_key");
        String mode = (String) apiOptions.getDatafileOptions().get("mode");
        Integer timeout = (Integer) apiOptions.getDatafileOptions().get("timeout");
        String datafileHost = BuildConfig.DATAFILE_HOST;
        Integer revision = (Integer) apiOptions.getDatafileOptions().get("revision");

        String format = "http://localhost:3001/datafiles/%s.json?request_id=" + apiOptions.getRequestId();
        revision = revision == null ? 1 : revision;
        revision += apiOptions.getDatafile() == null ? 0 : 1;

        long blockingTimeout = DEFAULT_BLOCKING_TIMEOUT;
        long awaitTimeout = DEFAULT_BLOCKING_TIMEOUT;
        CountDownLatch countDownLatch = new CountDownLatch(revision);
        notificationCenter.addNotificationHandler(UpdateConfigNotification.class, x -> {
            System.out.println("Making request for id: " + apiOptions.getRequestId());
            countDownLatch.countDown();
        });

        switch (mode == null ? "null" : mode) {
            case "wait_for_on_ready":
                blockingTimeout = timeout == null ? blockingTimeout : timeout.longValue();
                break;
            case "wait_for_config_update":
                awaitTimeout = timeout == null ? awaitTimeout : timeout.longValue();
                break;
            default:
                countDownLatch.countDown();
        }

        DatafileConfig datafileConfig = new DatafileConfig(OptimizelyWrapper.OPTIMIZELY_PROJECT_ID, sdkKey, format);

        try {
            countDownLatch.await(awaitTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.warn("Timeout expired waiting for update config to be triggered. CountDownLatch: {}", countDownLatch);
        }

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
