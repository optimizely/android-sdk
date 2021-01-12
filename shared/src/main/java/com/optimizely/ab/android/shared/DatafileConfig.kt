/****************************************************************************
 * Copyright 2018, Optimizely, Inc. and contributors                        *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                            *
 * *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.android.shared

import org.json.JSONException
import org.json.JSONObject

/**
 * A class to encapsulate the project id and any environment that might be used.
 * It can create the cache key used if you want to clear the cache for that project or environment.
 * It can also create the url depending on whether there is a environment or not.
 * Generally, this class is under the hood.  However, it should be noted that if you are using background
 * datafile updates, you need to make sure and call stop if you no longer want those updates.  For instance,
 * if you change environments or projects and no longer want updates from the previous.
 */
class DatafileConfig @JvmOverloads constructor(projectId: String?, sdkKey: String?, host: String = defaultHost) {
    private val projectId: String?
    private val sdkKey: String?
    private val host: String

    /**
     * Get the url associated with this project.  If there is an environment,
     * that url is returned.
     * @return url of current project configuration.
     */
    var url: String? = null

    /**
     * This returns the current datafile key string.
     * @return datafile key string.
     */
    val key: String
        get() = sdkKey ?: projectId!!

    fun toJSONString(): String? {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("projectId", projectId)
            jsonObject.put("sdkKey", sdkKey)
            return jsonObject.toString()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * To string either returns the proejct id as string or a concatenated string of project id
     * delimiter and environment key.
     * @return
     */
    override fun toString(): String {
        return projectId ?: "null" + delimiter + (sdkKey ?: "null")
    }

    override fun equals(o: Any?): Boolean {
        if (o === this) {
            return true
        }
        if (o !is DatafileConfig) {
            return false
        }
        val p = o
        return if (projectId != null) if (p.projectId != null) projectId == p.projectId else projectId === p.projectId else if (p.projectId == null
                &&
                sdkKey != null) if (p.sdkKey != null) sdkKey == p.sdkKey else sdkKey === p.sdkKey else p.sdkKey == null
    }

    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + (projectId?.hashCode() ?: 0) + (sdkKey?.hashCode() ?: 0)
        return result
    }

    companion object {
        @JvmField
        var defaultHost = "https://cdn.optimizely.com"
        var projectUrlSuffix = "/json/%s.json"
        @JvmField
        var environmentUrlSuffix = "/datafiles/%s.json"
        var delimiter = "::::"
        fun fromJSONString(jsonString: String?): DatafileConfig? {
            try {
                val jsonObject = JSONObject(jsonString)
                var projectId: String? = null
                if (jsonObject.has("projectId")) {
                    projectId = jsonObject.getString("projectId")
                }
                var environmentKey: String? = null
                if (jsonObject.has("sdkKey")) {
                    environmentKey = jsonObject.getString("sdkKey")
                }
                return DatafileConfig(projectId, environmentKey)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return null
        }
    }
    /**
     * Constructor used to construct a DatafileConfig to get cache key, url,
     * for the appropriate environment.  One or the other can be null.  But, not both.
     * @param projectId project id string.
     * @param sdkKey the environment url.
     * @param host used to override the DatafileConfig.defaultHost used for datafile synchronization.
     */
    /**
     * Constructor used to construct a DatafileConfig to get cache key, url,
     * for the appropriate environment.  One or the other can be null.  But, not both.
     * @param projectId project id string.
     * @param sdkKey the environment url.
     */
    init {
        assert(projectId != null || sdkKey != null)
        this.projectId = projectId
        this.sdkKey = sdkKey
        this.host = host
        if (sdkKey != null) {
            url = String.format(this.host + environmentUrlSuffix, sdkKey)
        } else {
            url = String.format(this.host + projectUrlSuffix, projectId)
        }
    }
}