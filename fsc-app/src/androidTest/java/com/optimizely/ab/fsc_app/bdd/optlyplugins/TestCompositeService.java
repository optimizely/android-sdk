package com.optimizely.ab.fsc_app.bdd.optlyplugins;

import android.support.test.espresso.core.deps.guava.base.CaseFormat;
import android.support.test.espresso.core.deps.guava.reflect.TypeToken;

import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.user_profile.DefaultUserProfileService;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.fsc_app.bdd.support.OptimizelyE2EService;
import com.optimizely.ab.fsc_app.bdd.optlyplugins.userprofileservices.TestUserProfileService;
import com.optimizely.ab.notification.ActivateNotification;
import com.optimizely.ab.notification.DecisionNotification;
import com.optimizely.ab.notification.TrackNotification;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gherkin.deps.com.google.gson.Gson;

import static com.optimizely.ab.notification.DecisionNotification.FeatureVariableDecisionNotificationBuilder.SOURCE_INFO;

public class TestCompositeService {

    public static void setupListeners(List<Map<String, String>> withListener, OptimizelyE2EService optimizelyE2EService) {
        if (withListener == null) return;
        if (!optimizelyE2EService.getOptimizelyManager().getOptimizely().isValid()) return;

        for (Map<String, String> map : withListener) {
            Object obj = map.get("count");
            int count = Integer.parseInt(obj.toString());

            switch (map.get("type")) {
                case "Activate":
                    for (int i = 0; i < count; i++) {
                        optimizelyE2EService.getOptimizelyManager().getOptimizely().getNotificationCenter().addNotificationHandler(ActivateNotification.class, activateNotification -> {
                            Map<String, Object> notificationMap = new HashMap<>();
                            notificationMap.put("experiment_key", activateNotification.getExperiment().getKey());
                            notificationMap.put("user_id", activateNotification.getUserId());
                            notificationMap.put("attributes", activateNotification.getAttributes());
                            notificationMap.put("variation_key", activateNotification.getVariation().getKey());

                            optimizelyE2EService.addNotification(notificationMap);
                        });
                    }
                    break;
                case "Track":
                    for (int i = 0; i < count; i++) {
                        optimizelyE2EService.getOptimizelyManager().getOptimizely().getNotificationCenter().addNotificationHandler(TrackNotification.class, trackNotification -> {
                            Map<String, Object> notificationMap = new HashMap<>();
                            notificationMap.put("event_key", trackNotification.getEventKey());
                            notificationMap.put("user_id", trackNotification.getUserId());
                            notificationMap.put("attributes", trackNotification.getAttributes());
                            notificationMap.put("event_tags", trackNotification.getEventTags());

                            optimizelyE2EService.addNotification(notificationMap);
                        });
                    }
                    break;
                case "Decision":
                    for (int i = 0; i < count; i++) {
                        optimizelyE2EService.getOptimizelyManager().getOptimizely().getNotificationCenter().addNotificationHandler(DecisionNotification.class, decisionNotification -> {
                            Map<String, Object> notificationMap = new HashMap<>();
                            notificationMap.put("type", decisionNotification.getType());
                            notificationMap.put("user_id", decisionNotification.getUserId());
                            notificationMap.put("attributes", decisionNotification.getAttributes());
                            notificationMap.put("decision_info", convertKeysCamelCaseToSnakeCase(decisionNotification.getDecisionInfo()));

                            optimizelyE2EService.addNotification(notificationMap);
                        });
                    }
                    break;
                default:
                    // do nothing
            }
        }
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

    public static List<Map<String, Object>> setListenerResponseCollection(OptimizelyE2EService optimizelyE2EService) {
        List<Map<String, Object>> responseCollection = optimizelyE2EService.getNotifications();

        // FCS expects empty arrays to be null :/
        if (responseCollection != null && responseCollection.size() == 0) {
            responseCollection = null;
        }

        return responseCollection;
    }

    public void setForcedVariations(ArrayList<HashMap> forcedVariationsList, OptimizelyE2EService optimizelyE2EService) {
        OptimizelyClient optimizely = optimizelyE2EService.getOptimizelyManager().getOptimizely();
        HashMap<String, HashMap<String, HashMap<String, String>>> forcedVariations = new HashMap<>();
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, HashMap<String, String>>>() {
        }.getType();
        if (forcedVariationsList != null) {
            for (HashMap forcedVariation : forcedVariationsList) {
                String userId = forcedVariation.get("user_id").toString();
                String bucket_map = forcedVariation.get("experiment_bucket_map").toString();
                HashMap<String, HashMap<String, String>> fVariations = gson.fromJson(bucket_map, type);
                Set fvarKeys = fVariations.keySet();
                for (int i = 0; i < fvarKeys.size(); i++) {
                    String vID = (fVariations.get(fvarKeys.toArray()[i])).get("variation_id");
                    optimizely.setForcedVariation(fvarKeys.toArray()[i].toString(), userId, vID);
                }
                forcedVariations.put(userId, forcedVariation);
            }
        }
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
