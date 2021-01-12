/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
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
package com.optimizely.ab.android.sdk

import android.content.Context
import android.os.Build
import org.slf4j.Logger
import java.util.*

/**
 * Class to encapsulate default attributes that will be added to attributes passed in
 * by the OptimizelyClient.
 */
object OptimizelyDefaultAttributes {
    private const val DEVICE_MODEL_KEY = "optimizely_android_device_model"
    private const val SDK_VERSION_KEY = "optimizely_android_sdk_version"
    private const val OS_VERSION_KEY = "optimizely_android_os_version"
    private const val APP_VERSION_KEY = "optimizely_android_app_version"

    /**
     * Builds the default attributes lists which includes the device model, sdk version, app version,
     * and the os version.
     *
     * @param context context used to get the app version information.
     * @param logger logger passed in for logging any warnings.
     * @return a map that has the default attributes.
     */
    @JvmStatic
    fun buildDefaultAttributesMap(context: Context, logger: Logger): Map<String, String> {
        val androidDeviceModel = Build.MODEL
        val androidOSVersion = Build.VERSION.RELEASE
        var androidSdkVersion = 0
        var androidSdkVersionName = ""
        var androidAppVersionName = ""
        var androidAppVersion = 0

        // In case there is some problem with accessing the BuildConfig file....
        try {
            androidSdkVersion = BuildConfig.VERSION_CODE
            androidSdkVersionName = BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            logger.warn("Error getting BuildConfig version code and version name")
        }
        try {
            val pInfo = context.applicationContext.packageManager.getPackageInfo(
                    context.applicationContext.packageName, 0)
            androidAppVersionName = pInfo.versionName
            androidAppVersion = pInfo.versionCode
        } catch (e: Exception) {
            logger.warn("Error getting app version from context.", e)
        }
        val attrMap: MutableMap<String, String> = HashMap()
        attrMap[DEVICE_MODEL_KEY] = androidDeviceModel
        attrMap[SDK_VERSION_KEY] = androidSdkVersionName
        attrMap[OS_VERSION_KEY] = androidOSVersion
        val appVersion = androidAppVersionName + Integer.toString(androidAppVersion)
        attrMap[APP_VERSION_KEY] = appVersion
        return attrMap
    }
}