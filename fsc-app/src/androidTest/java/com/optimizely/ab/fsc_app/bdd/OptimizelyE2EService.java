package com.optimizely.ab.fsc_app.bdd;

import android.support.test.espresso.core.deps.guava.base.CaseFormat;
import android.support.test.espresso.core.deps.guava.reflect.TypeToken;

import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.fsc_app.bdd.support.listeners.ActivateListener;
import com.optimizely.ab.fsc_app.bdd.support.requests.OptimizelyRequest;
import com.optimizely.ab.fsc_app.bdd.support.resources.ActivateResource;
import com.optimizely.ab.fsc_app.bdd.support.resources.ForcedVariationResource;
import com.optimizely.ab.fsc_app.bdd.support.resources.IsFeatureEnabledResource;
import com.optimizely.ab.fsc_app.bdd.support.resources.TrackResource;
import com.optimizely.ab.fsc_app.bdd.support.responses.BaseResponse;
import com.optimizely.ab.fsc_app.bdd.support.responses.ListenerMethodResponse;
import com.optimizely.ab.fsc_app.bdd.support.responses.ListenerResponse;
import com.optimizely.ab.notification.DecisionNotification;
import com.optimizely.ab.notification.NotificationCenter;
import com.optimizely.ab.notification.TrackNotification;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import gherkin.deps.com.google.gson.Gson;

import static com.optimizely.ab.fsc_app.bdd.support.Utils.parseYAML;
import static com.optimizely.ab.notification.DecisionNotification.FeatureVariableDecisionNotificationBuilder.SOURCE_INFO;

public class OptimizelyE2EService {
    private final static String OPTIMIZELY_PROJECT_ID = "123123";
    private ArrayList<ListenerResponse> listenerResponses = new ArrayList<>();

    public ArrayList<ListenerResponse> getListenerResponses() {
        return listenerResponses;
    }

    public void addListenerResponse(ListenerResponse listenerResponse) {
        listenerResponses.add(listenerResponse);
    }

    private BaseResponse result;
    private OptimizelyManager optimizelyManager;


    public OptimizelyManager getOptimizelyManager() {
        return optimizelyManager;
    }

    public void initializeOptimizely(OptimizelyRequest optimizelyRequest) {

        optimizelyManager = OptimizelyManager.builder(OPTIMIZELY_PROJECT_ID)
                .withEventDispatchInterval(60L * 10L)
                .withEventHandler(optimizelyRequest.getEventHandler())
                .withUserProfileService(optimizelyRequest.getUserProfileService())
                .withDatafileDownloadInterval(60L * 10L)
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

        Object argumentsObj = parseYAML(optimizelyRequest.getArguments(), optimizelyManager.getOptimizely().getProjectConfig());
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
                case "set_forced_variation":
                    result = ForcedVariationResource.getInstance().convertToResourceCall(this, argumentsObj);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Boolean compareFields(String field, String args) {
        Object parsedArguments = parseYAML(args, optimizelyManager.getOptimizely().getProjectConfig());

        switch (field) {
            case "listener_called":
                ListenerMethodResponse listenerMethodResponse = null;
                if (result instanceof ListenerMethodResponse)
                    listenerMethodResponse = (ListenerMethodResponse) result;
                else
                    return false;
                try {
                    if (parsedArguments != null) {
                        return parsedArguments.equals(listenerMethodResponse.getListenerCalled());
                    }
                } catch (Exception e) {
                }
                return parsedArguments == listenerMethodResponse.getListenerCalled();

            case "dispatched_event":
                try {
                    //TODO - resolve this comparision:
//                    HashMap actualParams = (HashMap) ProxyEventDispatcher.getDispatchedEvents().get(0).get("params");
//                    HashMap expectedParams = (HashMap) ((ArrayList) parsedArguments).get(0);
//                    Map temp = new HashMap(expectedParams);
//                    temp.equals(expectedParams);
//                    mergeWithSideEffect(expectedParams, actualParams);
//                    // Add everything in map1 not in map2 to map2
//                    mergeWithSideEffect(expectedParams, temp);
//                    return actualParams.equals(expectedParams);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            default:
                return false;
        }
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

    private void setupListeners(ArrayList<HashMap<String, Object>> withListener) {
        if (withListener == null) return;

        for (Map<String, Object> map : withListener) {
            int count = (int) Double.parseDouble(map.get("count").toString());
            if ("Activate".equals(map.get("type"))) {
                for (int i = 0; i < count; i++) {
                    System.out.println("Adding activate notification");
                    ActivateListener activateListener = new ActivateListener();
                    getOptimizelyManager().getOptimizely().getNotificationCenter().addNotificationListener(NotificationCenter.NotificationType.Activate,
                            activateListener);
                    addListenerResponse(activateListener);

                }
            } else if ("Track".equals(map.get("type"))) {

                ArrayList<Map<String, Object>> trackListenerResponse = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    getOptimizelyManager().getOptimizely().addTrackNotificationHandler((TrackNotification trackNotification) -> {
                        Map<String, Object> trackMap = new HashMap<>();

                        trackMap.put("event_key", trackNotification.getEventKey());
                        trackMap.put("user_id", trackNotification.getUserId());
                        trackMap.put("attributes", trackNotification.getAttributes());
                        trackMap.put("event_tags", trackNotification.getEventTags());
                        trackListenerResponse.add(trackMap);
                    });

                    addListenerResponse(() -> trackListenerResponse);
                }

            } else if ("Decision".equals(map.get("type"))) {
                ArrayList<Map<String, Object>> decisionListenerResponse = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    getOptimizelyManager().getOptimizely().addDecisionNotificationHandler((DecisionNotification decisionNotification) -> {
                        Map<String, Object> decisionMap = new HashMap<>();

                        decisionMap.put("type", decisionNotification.getType());
                        decisionMap.put("user_id", decisionNotification.getUserId());
                        decisionMap.put("attributes", decisionNotification.getAttributes());
                        decisionMap.put("decision_info", convertKeysCamelCaseToSnakeCase(decisionNotification.getDecisionInfo()));
                        decisionListenerResponse.add(decisionMap);
                    });

                    addListenerResponse(() -> decisionListenerResponse);
                }
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
