package com.optimizely.ab.android.sdk;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.bucketing.UserExperimentRecord;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.event.EventHandler;

/**
 * Created by jdeffibaugh on 8/18/16 for Optimizely.
 */
public class ParcelableOptimizely implements Parcelable {

    public static final Creator<ParcelableOptimizely> CREATOR = new Creator<ParcelableOptimizely>() {
        @Override
        public ParcelableOptimizely createFromParcel(Parcel in) {
            return new ParcelableOptimizely(in);
        }

        @Override
        public ParcelableOptimizely[] newArray(int size) {
            return new ParcelableOptimizely[size];
        }
    };
    private Optimizely optimizely;
    private OptimizelySDK optimizelySDK;

    public ParcelableOptimizely(@NonNull OptimizelySDK optimizelySDK, @NonNull Optimizely optimizely) {
        this.optimizelySDK = optimizelySDK;
        this.optimizely = optimizely;
    }

    protected ParcelableOptimizely(Parcel in) {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    @Nullable
    public Optimizely unParcel() {
        ProjectConfig projectConfig = optimizely.getProjectConfig();
        // return Optimizely.restore(projectConfig); // What I want!
        final EventHandler eventHandler = optimizelySDK.getEventHandler();
        final UserExperimentRecord userExperimentRecord = optimizelySDK.getUserExperimentRecord();
        if (eventHandler != null && userExperimentRecord != null) {
            return new Optimizely.Builder(projectConfig, eventHandler)
                    .withUserExperimentRecord(userExperimentRecord)
                    .build();
        } else {
            return null;
        }

    }
}
