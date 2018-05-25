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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * A class to encapsulate the project id and any environment that might be used.
 * It can create the cache key used if you want to clear the cache for that project or environment.
 * It can also create the url depending on whether there is a environment or not.
 * Generally, this class is under the hood.  However, it should be noted that if you are using background
 * datafile updates, you need to make sure and call stop if you no longer want those updates.  For instance,
 * if you change environments or projects and no longer want updates from the previous.
 */
public class DatafileConfig {
    public static String projectUrl = "https://cdn.optimizely.com/json/%s.json";
    public static String delimitor = "::::";

    @NonNull private final String projectId;
    private final String environmentKey;

    /**
     * Constructor used to construct a ProjectId to get cache key, url,
     * and environment.
     * @param projectId project id string.
     * @param environmentKey the environment url.
     */
    public DatafileConfig(@NonNull String projectId, String environmentKey) {
        this.projectId = projectId;
        this.environmentKey = environmentKey;
    }

    /**
     * Constructor with no environment
     * @param projectId the current project id string.
     */
    public DatafileConfig(@NonNull String projectId) {
        this(projectId, null);
    }

    /**
     * This returns the current project id string.
     * @return project id string.
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * The environment key object for this project id.
     * This can be null.
     * @return environment object for current project config if any.
     */
    public String getEnvironmentKey() {
        return environmentKey;
    }

    /**
     * Return the cache key for this project id. Or, return
     * the environment cache key if there is an environment.
     *
     * @return cache key used to cache datafile.
     */
    public String getCacheKey() {
        if (environmentKey != null) {
            return String.valueOf(environmentKey.hashCode());
        }

        return projectId;
    }

    /**
     * Get the url associated with this project.  If there is an environment,
     * that url is returned.
     * @return url of current project configuration.
     */
    public String getUrl() {
        if (environmentKey != null) {
            return environmentKey;
        }
        else {
            return String.format(projectUrl, projectId);
        }
    }

    public String toJSONString() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("projectId", projectId);
            jsonObject.put("environmentKey", environmentKey);
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static DatafileConfig fromJSONString(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            if (jsonObject.has("projectId")) {
                String projectId = jsonObject.getString("projectId");
                String environmentKey = null;
                if (jsonObject.has("environmentKey")) {
                    environmentKey = jsonObject.getString("environmentKey");
                }
                return new DatafileConfig(projectId, environmentKey);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
    /**
     * To string either returns the proejct id as string or a concatenated string of project id
     * delimiter and environment key.
     * @return
     */
    @Override
    public String toString() {
        return projectId + (environmentKey != null? delimitor + environmentKey.toString() : "");
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DatafileConfig)) {
            return false;
        }
        DatafileConfig p = (DatafileConfig) o;
        return p.projectId.equals(((DatafileConfig) o).projectId) &&
                (p.getEnvironmentKey() == null && ((DatafileConfig) o).getEnvironmentKey() == null) ||
                (p.getEnvironmentKey() != null && p.getEnvironmentKey().equals(((DatafileConfig) o).getEnvironmentKey()) ||
                        ((DatafileConfig) o).getEnvironmentKey() != null &&
                                ((DatafileConfig) o).getEnvironmentKey().equals(p.getEnvironmentKey()));
    }

    public int hashCode() {
        int result = 17;
        result = 31 * result + projectId.hashCode() + (getEnvironmentKey() == null ? 0 : getEnvironmentKey().hashCode());
        return result;
    }

}
