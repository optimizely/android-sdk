package com.optimizely.ab.android.shared;

import android.support.annotation.NonNull;
import java.util.Arrays;

public class ProjectId implements ProjectKey {
    public static String projectUrl = "https://cdn.optimizely.com/json/%s.json";
    public static String delimitor = "::::";

    @NonNull private final String projectId;
    private EnvironmentKey environmentKey;

    public ProjectId(@NonNull String projectId, String environmentKey) {
        this.projectId = projectId;
        if (environmentKey != null) {
            this.environmentKey = new EnvironmentKey(environmentKey);
        }
    }

    public ProjectId(@NonNull String projectId) {
        this(projectId, null);
    }

    public String getId() {
        return projectId;
    }

    public EnvironmentKey getEnvironmentKey() {
        return environmentKey;
    }

    @Override
    public String getCacheKey() {
        if (environmentKey != null) {
            return environmentKey.getCacheKey();
        }

        return projectId;
    }

    @Override
    public String getUrl() {
        if (environmentKey != null) {
            return environmentKey.getUrl();
        }
        else {
            return String.format(projectUrl, projectId);
        }
    }

    @Override
    public String toString() {
        return projectId + (environmentKey != null? delimitor + environmentKey.toString() : "");
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ProjectId)) {
            return false;
        }
        ProjectId p = (ProjectId) o;
        return p.projectId == ((ProjectId) o).projectId && p.getEnvironmentKey() == ((ProjectId) o).getEnvironmentKey();
    }

    public int hashCode() {
        int result = 17;
        result = 31 * result + projectId.hashCode() + (getEnvironmentKey() == null ? 0 : getEnvironmentKey().hashCode());
        return result;
    }

}
