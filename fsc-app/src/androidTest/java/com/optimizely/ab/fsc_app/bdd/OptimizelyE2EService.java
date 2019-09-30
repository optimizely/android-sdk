package com.optimizely.ab.fsc_app.bdd;

import android.support.test.espresso.core.deps.guava.base.CaseFormat;
import android.support.test.espresso.core.deps.guava.reflect.TypeToken;

import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.fsc_app.bdd.support.Utils;
import com.optimizely.ab.fsc_app.bdd.support.customeventdispatcher.ProxyEventDispatcher;
import com.optimizely.ab.fsc_app.bdd.support.requests.OptimizelyRequest;
import com.optimizely.ab.fsc_app.bdd.support.resources.ActivateResource;
import com.optimizely.ab.fsc_app.bdd.support.resources.ForcedVariationResource;
import com.optimizely.ab.fsc_app.bdd.support.resources.GetFeatureVariableBooleanResource;
import com.optimizely.ab.fsc_app.bdd.support.resources.GetFeatureVariableDoubleResource;
import com.optimizely.ab.fsc_app.bdd.support.resources.GetFeatureVariableIntegerResource;
import com.optimizely.ab.fsc_app.bdd.support.resources.GetFeatureVariableStringResource;
import com.optimizely.ab.fsc_app.bdd.support.resources.GetVariationResource;
import com.optimizely.ab.fsc_app.bdd.support.resources.IsFeatureEnabledResource;
import com.optimizely.ab.fsc_app.bdd.support.resources.TrackResource;
import com.optimizely.ab.fsc_app.bdd.support.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.support.responses.ListenerMethodResponse;
import com.optimizely.ab.fsc_app.bdd.support.userprofileservices.NoOpService;
import com.optimizely.ab.notification.ActivateNotification;
import com.optimizely.ab.notification.DecisionNotification;
import com.optimizely.ab.notification.TrackNotification;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gherkin.deps.com.google.gson.Gson;

import static com.optimizely.ab.fsc_app.bdd.support.Utils.parseYAML;
import static com.optimizely.ab.notification.DecisionNotification.FeatureVariableDecisionNotificationBuilder.SOURCE_INFO;

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
                Class<?> userProfileServiceClass = Class.forName("com.optimizely.ab.fsc_app.bdd.support.userprofileservices." + optimizelyRequest.getUserProfileService());
                Constructor<?> serviceConstructor = userProfileServiceClass.getConstructor(ArrayList.class);
                userProfileService = UserProfileService.class.cast(serviceConstructor.newInstance(optimizelyRequest.getUserProfiles()));
            } catch (Exception e) { }
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
        setupListeners(optimizelyRequest.getWithListener());
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
                    result = GetVariationResource.getInstance().convertToResourceCall(this, argumentsObj);
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
            case "listener_called":
                return compareListenerCalled(count, parsedArguments);

            case "dispatched_event":
                try {
                    HashMap actualParams = (HashMap) ProxyEventDispatcher.getDispatchedEvents().get(0).get("params");
                    HashMap expectedParams = (HashMap) ((ArrayList) parsedArguments).get(0);
                    return Utils.containsSubset(expectedParams, actualParams);
                } catch (Exception e) {
                    return false;
                }
            default:
                return false;
        }
    }

    public Boolean compareListenerCalled(int count, Object parsedArguments) {
        ListenerMethodResponse listenerMethodResponse;
        if (result instanceof ListenerMethodResponse)
            listenerMethodResponse = (ListenerMethodResponse) result;
        else
            return false;
        try {
            Object expectedListenersCalled = copyResponse(count, parsedArguments);
            return expectedListenersCalled.equals(listenerMethodResponse.getListenerCalled());
        } catch (Exception e) {
        }
        return parsedArguments == listenerMethodResponse.getListenerCalled();
    }

    public Object copyResponse(int count, Object args) {
        if (args instanceof List) {
            List argsObject = (List) args;
            List cloneArgs = new ArrayList<>();
            if (argsObject.size() > 0) {
                for (int i = 0; i < count; i++) {
                    cloneArgs.add((argsObject).get(0));
                }
                return cloneArgs;
            }
        }
        return args;
    }

    public void setForcedVariations(ArrayList<HashMap> forcedVariationsList) {
        OptimizelyClient optimizely = getOptimizelyManager().getOptimizely();
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


    private void setupListeners(List<Map<String, String>> withListener) {
        if (withListener == null) return;
        if (!getOptimizelyManager().getOptimizely().isValid()) return;

        for (Map<String, String> map : withListener) {
            Object obj = map.get("count");
            int count = Integer.parseInt(obj.toString());

            switch (map.get("type")) {
                case "Activate":
                    for (int i = 0; i < count; i++) {
                        getOptimizelyManager().getOptimizely().getNotificationCenter().addNotificationHandler(ActivateNotification.class, activateNotification -> {
                            Map<String, Object> notificationMap = new HashMap<>();
                            notificationMap.put("experiment_key", activateNotification.getExperiment().getKey());
                            notificationMap.put("user_id", activateNotification.getUserId());
                            notificationMap.put("attributes", activateNotification.getAttributes());
                            notificationMap.put("variation_key", activateNotification.getVariation().getKey());

                            addNotification(notificationMap);
                        });
                    }
                    break;
                case "Track":
                    for (int i = 0; i < count; i++) {
                        getOptimizelyManager().getOptimizely().getNotificationCenter().addNotificationHandler(TrackNotification.class, trackNotification -> {
                            Map<String, Object> notificationMap = new HashMap<>();
                            notificationMap.put("event_key", trackNotification.getEventKey());
                            notificationMap.put("user_id", trackNotification.getUserId());
                            notificationMap.put("attributes", trackNotification.getAttributes());
                            notificationMap.put("event_tags", trackNotification.getEventTags());

                            addNotification(notificationMap);
                        });
                    }
                    break;
                case "Decision":
                    for (int i = 0; i < count; i++) {
                        getOptimizelyManager().getOptimizely().getNotificationCenter().addNotificationHandler(DecisionNotification.class, decisionNotification -> {
                            Map<String, Object> notificationMap = new HashMap<>();
                            notificationMap.put("type", decisionNotification.getType());
                            notificationMap.put("user_id", decisionNotification.getUserId());
                            notificationMap.put("attributes", decisionNotification.getAttributes());
                            notificationMap.put("decision_info", convertKeysCamelCaseToSnakeCase(decisionNotification.getDecisionInfo()));

                            addNotification(notificationMap);
                        });
                    }
                    break;
                default:
                    // do nothing
            }
        }
    }

    private Map<String, ?> convertKeysCamelCaseToSnakeCase(Map<String, ?> decisionInfo) {
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
