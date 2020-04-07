package com.optimizely.ab.android.optimizely_debugger;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.optimizelyconfig.OptimizelyConfig;

import java.util.ArrayList;

public class ObjectWrapperForBinder extends Binder {
    private final Object data;

    public ObjectWrapperForBinder(Object data) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }
}

