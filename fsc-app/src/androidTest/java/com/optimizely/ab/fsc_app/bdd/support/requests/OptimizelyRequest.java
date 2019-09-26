package com.optimizely.ab.fsc_app.bdd.support.requests;

import android.content.Context;
import android.support.annotation.RawRes;

import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.NoopEventHandler;
import com.optimizely.ab.event.internal.payload.EventBatch;
import com.optimizely.ab.fsc_app.bdd.support.customeventdispatcher.ProxyEventDispatcher;
import com.optimizely.ab.fsc_app.bdd.support.userprofileservices.NoOpService;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.android.sdk.OptimizelyManager.loadRawResource;


public class OptimizelyRequest {
    private Context context;
    private EventHandler eventHandler;
    private UserProfileService userProfileService;
    private String datafile;
    private ArrayList<Map<String,Object>> dispatchedEvents = new ArrayList<>();
    private ArrayList<Map<String, String>> withListener = new ArrayList<>();
    private ArrayList<HashMap> forceVariations = new ArrayList<>();

    private String api;
    private String arguments;

    public OptimizelyRequest(Context context) {
        this.context = context;
        this.eventHandler = new ProxyEventDispatcher(dispatchedEvents);
        this.userProfileService = new NoOpService();
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

    public void setDispatchedEvents(ArrayList<Map<String,Object>>  dispatchedEvents) {
        this.dispatchedEvents = dispatchedEvents;
    }

    public ArrayList<Map<String,Object>>  getDispatchedEvents() {
        return dispatchedEvents;
    }

    public void setForceVariations(ArrayList<HashMap> forceVariations) {
        this.forceVariations = forceVariations;
    }

    public ArrayList<HashMap> getForceVariations() {
        return forceVariations;
    }

    public List<Map<String, String>> getWithListener() {
        return withListener;
    }

    public Context getContext() {
        return context;
    }

    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public String getDatafile() {
        return datafile;
    }

    public UserProfileService getUserProfileService() {
        return userProfileService;
    }

    // TODO: use this method for setting userProfiles
    public void setUserProfileService(String userProfileServiceName) {
        if (userProfileServiceName != null) {
            try {
                Class<?> userProfileServiceClass = Class.forName("com.optimizely.ab.android.test_app.bdd.cucumber.support.user_profile_services." + userProfileServiceName);
                Constructor<?> serviceConstructor = userProfileServiceClass.getConstructor(ArrayList.class);
                userProfileService = UserProfileService.class.cast(serviceConstructor.newInstance(userProfileServiceName));
            } catch (Exception e) {
            }
        }
    }

    public void setEventHandler(String customEventDispatcher) {
        if (customEventDispatcher != null && customEventDispatcher.equals("ProxyEventDispatcher")) {
            eventHandler = new ProxyEventDispatcher(dispatchedEvents);
        } else {
            eventHandler = new NoopEventHandler();
        }
    }

    public void setDatafile(String datafileName) {
        @RawRes Integer intRawResID = context.getResources().
                getIdentifier(datafileName.split("\\.")[0],
                        "raw",
                        context.getPackageName());
        try {
            datafile = loadRawResource(context, intRawResID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addWithListener(HashMap<String, String> withListener) {
        this.withListener.add(withListener);
    }


}
