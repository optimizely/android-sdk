/****************************************************************************
 * Copyright 2018,2021, Optimizely, Inc. and contributors                   *
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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class to encapsulate the project id and any environment that might be used.
 * It can create the cache key used if you want to clear the cache for that project or environment.
 * It can also create the url depending on whether there is a environment or not.
 * Generally, this class is under the hood.  However, it should be noted that if you are using background
 * datafile updates, you need to make sure and call stop if you no longer want those updates.  For instance,
 * if you change environments or projects and no longer want updates from the previous.
 */
public class DatafileConfig {
    public static String defaultHost = "https://cdn.optimizely.com";
    public static String projectUrlSuffix = "/json/%s.json";
    public static String environmentUrlSuffix = "/datafiles/%s.json";
    public static String delimiter = "::::";

    private final String projectId;
    private final String sdkKey;
    private final String host;
    private final String datafileUrlString;

    /**
     * Constructor used to construct a DatafileConfig to get cache key, url,
     * for the appropriate environment.  One or the other can be null.  But, not both.
     * @param projectId project id string.
     * @param sdkKey the environment url.
     * @param host used to override the DatafileConfig.defaultHost used for datafile synchronization.
     */
    public DatafileConfig(String projectId, String sdkKey, String host) {
        assert(projectId != null || sdkKey != null);
        this.projectId = projectId;
        this.sdkKey = sdkKey;
        this.host = host;

        if (sdkKey != null) {
            this.datafileUrlString = String.format((this.host + environmentUrlSuffix), sdkKey);
        }
        else {
            this.datafileUrlString = String.format((this.host + projectUrlSuffix), projectId);
        }
    }

    /**
     * Constructor used to construct a DatafileConfig to get cache key, url,
     * for the appropriate environment.  One or the other can be null.  But, not both.
     * @param projectId project id string.
     * @param sdkKey the environment url.
     */
    public DatafileConfig(String projectId, String sdkKey) {
        this(projectId, sdkKey, defaultHost);
    }

    /**
     * This returns the current datafile key string.
     * @return datafile key string.
     */
    public String getKey() {
        return sdkKey != null ? sdkKey : projectId;
    }

    /**
     * Get the url associated with this project.  If there is an environment,
     * that url is returned.
     * @return url of current project configuration.
     */
    public String getUrl() {
        return datafileUrlString;
    }

    public String toJSONString() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("projectId", projectId);
            jsonObject.put("sdkKey", sdkKey);
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static DatafileConfig fromJSONString(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            String projectId = null;
            if (jsonObject.has("projectId")) {
                projectId = jsonObject.getString("projectId");
            }
            String environmentKey = null;
            if (jsonObject.has("sdkKey")) {
                environmentKey = jsonObject.getString("sdkKey");
            }
            return new DatafileConfig(projectId, environmentKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
    /**
     * To string either returns the proejct id as string or a concatenated string of project id
     * delimiter and environment key.
     * @return the string identification for the DatafileConfig
     */
    @Override
    public String toString() {
        return projectId != null ? projectId : "null" + delimiter + (sdkKey != null? sdkKey : "null");
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DatafileConfig)) {
            return false;
        }
        DatafileConfig p = (DatafileConfig) o;
        return this.projectId != null ? (p.projectId != null ? this.projectId.equals(p.projectId) : this.projectId == p.projectId) : p.projectId == null
                &&
                this.sdkKey != null ? (p.sdkKey != null ? this.sdkKey.equals(p.sdkKey) : this.sdkKey == p.sdkKey) : p.sdkKey == null;

    }

    public int hashCode() {
        int result = 17;
        result = 31 * result + (projectId == null ? 0 : projectId.hashCode()) + (sdkKey == null ? 0 : sdkKey.hashCode());
        return result;
    }

}
