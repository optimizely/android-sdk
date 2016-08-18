package com.optimizely.ab.android.sdk;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.bucketing.UserExperimentRecord;
import com.optimizely.ab.config.Attribute;
import com.optimizely.ab.config.EventType;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Group;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.event.EventHandler;

import java.util.ArrayList;
import java.util.List;

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

    @NonNull private final ProjectConfig projectConfig;

    public ParcelableOptimizely(@NonNull Optimizely optimizely) {
        this.projectConfig = optimizely.getProjectConfig();
    }

    protected ParcelableOptimizely(Parcel in) {
        String accountId = in.readString();
        String projectId = in.readString();
        String revision = in.readString();
        String version = in.readString();

        List<Group> groupList = new ArrayList<>();
        List<Experiment> experimentList = new ArrayList<>();
        List<Attribute> attributeList = new ArrayList<>();
        List<EventType> eventList = new ArrayList<>();
        List<Audience> audiences = new ArrayList<>();

        final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

        in.readList(groupList, systemClassLoader);
        in.readList(experimentList, systemClassLoader);
        in.readList(attributeList, systemClassLoader);
        in.readList(eventList, systemClassLoader);
        in.readList(audiences, systemClassLoader);

        this.projectConfig = new ProjectConfig(accountId, projectId, revision, version,
                groupList, experimentList, attributeList, eventList, audiences);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(projectConfig.getAccountId());
        dest.writeString(projectConfig.getProjectId());
        dest.writeString(projectConfig.getVersion());

        dest.writeList(projectConfig.getGroups());
        dest.writeList(projectConfig.getExperiments());
        dest.writeList(projectConfig.getAudiences());
    }

    @Nullable
    public Optimizely unParcel(@NonNull OptimizelySDK optimizelySDK) {
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
