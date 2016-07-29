package com.optimizely.android.testapp;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.android.OnDataFileLoadedListener;
import com.optimizely.android.OptlyEventHandler;
import com.optimizely.android.OptlyProjectWatcher;
import com.optimizely.android.ProjectWatcher;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements OnDataFileLoadedListener {

    @Nullable ProjectWatcher projectWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // start polling for the dataFile
        projectWatcher = OptlyProjectWatcher.getInstance("projectId");
        projectWatcher.startWatching(this, this);
        // sync the data file even when the app isn't open, this method doesn't hit the callback
        projectWatcher.startWatchingInBackground(this, TimeUnit.DAYS, 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (projectWatcher != null) {
            projectWatcher.stopWatching(this);
        }
    }

    @Override
    public void onDataFileLoaded(String dataFile) {
        // create Android event handler
        EventHandler eventHandler = OptlyEventHandler.getInstance(this);

        // instantiate Optimizely object
        Optimizely optimizely = Optimizely.builder(dataFile, eventHandler).build();

        // activate user in the experiment
        Variation variation = optimizely.activate("experiment1", "user1");

        if (variation != null) {
            if (variation.is("variation_a")) {
                Toast.makeText(this, "Variation A", Toast.LENGTH_LONG).show();
            } else if (variation.is("variation_b")) {
                Toast.makeText(this, "Variation B", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Default", Toast.LENGTH_LONG).show();
        }

        // track conversion event
        optimizely.track("event1", "user1");
    }
}
