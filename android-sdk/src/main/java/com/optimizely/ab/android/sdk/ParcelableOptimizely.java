package com.optimizely.ab.android.sdk;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.config.ProjectConfig;

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

    public ParcelableOptimizely(@NonNull Optimizely optimizely) {
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

    public Optimizely unparcel() {
        ProjectConfig projectConfig = optimizely.getProjectConfig();
        // return Optimizely.restore(projectConfig); // What I want!
        return optimizely;
    }
}
