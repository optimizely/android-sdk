/****************************************************************************
 * Copyright 2016-2022, Optimizely, Inc. and contributors                   *
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

package com.optimizely.ab.android.sdk;

import android.content.Context;
import android.content.pm.PackageInfo;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 *  Class to encapsulate default attributes that will be added to attributes passed in
 *  by the OptimizelyClient.
 */
public class OptimizelyDefaultAttributes {

    static private final String DEVICE_MODEL_KEY = "optimizely_android_device_model";
    static private final String SDK_VERSION_KEY = "optimizely_android_sdk_version";
    static private final String OS_VERSION_KEY = "optimizely_android_os_version";
    static private final String APP_VERSION_KEY = "optimizely_android_app_version";

    /**
     * Builds the default attributes lists which includes the device model, sdk version, app version,
     * and the os version.
     *
     * @param context context used to get the app version information.
     * @param logger logger passed in for logging any warnings.
     * @return a map that has the default attributes.
     */
    static Map<String, String> buildDefaultAttributesMap(Context context, Logger logger) {
        String androidDeviceModel = android.os.Build.MODEL;
        String androidOSVersion = android.os.Build.VERSION.RELEASE;
        String androidSdkVersion = "";
        String androidSdkVersionName = "";
        String androidAppVersionName = "";
        int androidAppVersion = 0;

        // In case there is some problem with accessing the BuildConfig file....
        try {
            androidSdkVersion = BuildConfig.CLIENT_VERSION;
            androidSdkVersionName = BuildConfig.LIBRARY_PACKAGE_NAME;
        }
        catch (Exception e) {
            logger.warn("Error getting BuildConfig version code and version name");
        }

        try {
            PackageInfo pInfo = context.getApplicationContext().getPackageManager().getPackageInfo(
                    context.getApplicationContext().getPackageName(), 0);
            androidAppVersionName = pInfo.versionName;
            androidAppVersion = pInfo.versionCode;
        }
        catch (Exception e) {
            logger.warn("Error getting app version from context.", e);
        }


        Map<String, String> attrMap = new HashMap<>();

        attrMap.put(DEVICE_MODEL_KEY, androidDeviceModel);
        attrMap.put(SDK_VERSION_KEY, androidSdkVersion);
        attrMap.put(OS_VERSION_KEY, androidOSVersion);
        attrMap.put(APP_VERSION_KEY, androidAppVersionName);

        return attrMap;
    }

    static Map<String, Object> buildODPCommonData(Context context, Logger logger) {
        String androidDeviceModel = android.os.Build.MODEL;
        String androidOSVersion = android.os.Build.VERSION.RELEASE;

        // NOTE: other common fields for all ODP events are automatically added in java-core SDK (with values for android-sdk)
        //       android-sdk version number is passed down to java-sdk core when initialized.
        // - "idempotence_id" ("abcd1234-efgh-...")
        // - "data_source_type" ("sdk")
        // - "data_source" ("android-sdk")
        // - "data_source_version" ("3.14.0")

        Map<String, Object> commonData = new HashMap<>();
        commonData.put("os", "Android");
        commonData.put("os_version", androidOSVersion);
        commonData.put("device_type", "Phone");
        commonData.put("model", androidDeviceModel);

        return commonData;
    }

}
