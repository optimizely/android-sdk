package com.optimizely.ab.android.test_app;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.android.project_watcher.OnDataFileLoadedListener;
import com.optimizely.ab.android.sdk.OptimizelyAndroid;
import com.optimizely.ab.android.project_watcher.OptlyProjectWatcher;
import com.optimizely.ab.android.project_watcher.ProjectWatcher;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements OnDataFileLoadedListener {

    @Nullable ProjectWatcher projectWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // start polling for the dataFile
        projectWatcher = OptlyProjectWatcher.getInstance("6820012714", getApplicationContext());
        // sync the data file even when the app isn't open, this method doesn't hit the callback
        projectWatcher.startWatching(TimeUnit.DAYS, 1);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (projectWatcher != null) {
            projectWatcher.loadDataFile(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (projectWatcher != null) {
            projectWatcher.cancelDataFileLoad();
        }
    }

    @Override
    public void onDataFileLoaded(String dataFile) {
        Optimizely optimizely = OptimizelyAndroid.newInstance(this, dataFile);

        // activate user in the experiment
        Variation variation = optimizely.activate("exp1", "user1");

        if (variation != null) {
            if (variation.is("var1")) {
                Toast.makeText(this, "Variation 1", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Default", Toast.LENGTH_LONG).show();
        }

        // track conversion event
        optimizely.track("goal1", "user1");
    }
}
