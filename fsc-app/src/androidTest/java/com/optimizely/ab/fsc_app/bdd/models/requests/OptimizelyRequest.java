package com.optimizely.ab.fsc_app.bdd.models.requests;

import android.content.Context;
import android.support.annotation.RawRes;

import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.NoopEventHandler;
import com.optimizely.ab.fsc_app.bdd.optlyplugins.ProxyEventDispatcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.optimizely.ab.android.sdk.OptimizelyManager.loadRawResource;


public class OptimizelyRequest {
    private Context context;
    private EventHandler eventHandler;
    private String userProfileService;
    private String datafile;
    private ArrayList<Map<String,Object>> dispatchedEvents = new ArrayList<>();
    private ArrayList<Map<String, String>> withListener = new ArrayList<>();
    private ArrayList<HashMap> forceVariations = new ArrayList<>();
    private ArrayList<Map> userProfiles = new ArrayList<>();
    private String api;
    private String arguments;

    public OptimizelyRequest(Context context) {
        this.context = context;
        this.eventHandler = new ProxyEventDispatcher(dispatchedEvents);
    }

    public ArrayList<Map> getUserProfiles() {
        return userProfiles;
    }

    public void addUserProfile(Map userProfile) {
        userProfiles.add(userProfile);
    }

    public void setUserProfiles(ArrayList<Map> userProfiles) {
        this.userProfiles = userProfiles;
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

    public String getUserProfileService() {
        return userProfileService;
    }

    public void setUserProfileService(String userProfileService) {
        this.userProfileService = userProfileService;
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
