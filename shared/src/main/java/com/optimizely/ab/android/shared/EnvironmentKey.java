package com.optimizely.ab.android.shared;

import android.support.annotation.NonNull;

public class EnvironmentKey implements ProjectKey {
    @NonNull private final String environmentKey;

    public EnvironmentKey(@NonNull String environmentKey) {
        this.environmentKey = environmentKey;
    }

    @Override
    public String getCacheKey() {
        String[] keys = environmentKey.split("/");
        String suffix = keys[keys.length -1];
        if (suffix.endsWith(".json")) {
            suffix = suffix.substring(0, suffix.length() - ".json".length());
        }
        return suffix;
    }

    @Override
    public String getUrl() {
        return environmentKey;
    }

    @Override
    public String toString() {
        return environmentKey;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EnvironmentKey)) {
            return false;
        }
        EnvironmentKey p = (EnvironmentKey) o;
        return p.environmentKey == ((EnvironmentKey) o).environmentKey && p.environmentKey == ((EnvironmentKey) o).environmentKey;
    }

    public int hashCode() {
        int result = 17;
        result = 31 * result + environmentKey.hashCode();
        return result;
    }

}
