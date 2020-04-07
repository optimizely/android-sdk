package com.optimizely.ab.android.optimizely_debugger;

import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.util.Log;

import com.optimizely.ab.android.optimizely_debugger.DebugActivity;
import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.sdk.OptimizelyStartListener;

import java.util.Optional;

public class OptimizelyDebugger {
    private static OptimizelyDebugger _instance;
    private OptimizelyManager optimizelyManager;

    public static OptimizelyDebugger getInstance() {
        if(_instance == null) {
            _instance = new OptimizelyDebugger();
        }
        return _instance;
    }

    public OptimizelyManager getOptimizelyManager() {
        return getInstance().optimizelyManager;
    }

    public static void open(Context context, OptimizelyManager optimizelyManager) {
       Log.d("OptimizelyDebugger", "optimizely debugger open");

       getInstance().optimizelyManager = optimizelyManager;

       Intent intent = new Intent(context, DebugActivity.class);
       context.startActivity(intent);
    }

}
