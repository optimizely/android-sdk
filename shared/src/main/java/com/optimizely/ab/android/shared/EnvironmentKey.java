/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.android.shared;

import android.support.annotation.NonNull;

/**
 * EnvironmentKey represents the environment.  Using this class you can get the
 * environment datafile cache key along with the datafile url.
 */
public class EnvironmentKey implements ProjectKey {
    @NonNull private final String environmentKey;

    public EnvironmentKey(@NonNull String environmentKey) {
        this.environmentKey = environmentKey;
    }

    /**
     * Get the cache key for this environment.
     * @return the cache key for this environment.
     */
    @Override
    public String getCacheKey() {
        String[] keys = environmentKey.split("/");
        String suffix = keys[keys.length -1];
        if (suffix.endsWith(".json")) {
            suffix = suffix.substring(0, suffix.length() - ".json".length());
        }
        return suffix;
    }

    /**
     * Get the url for this environment.
     * @return the url for this environment.
     */
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
