/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                   *
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

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import androidx.annotation.NonNull;

import com.optimizely.ab.event.internal.payload.EventBatch;

/**
 * This class manages client engine value of the Event depending on current mode of UI.
 */
public class OptimizelyClientEngine {

    /**
     * Get client engine value for current UI mode type
     *
     * @param context any valid Android {@link Context}
     * @return String value of client engine
     */
    public static EventBatch.ClientEngine getClientEngineFromContext(@NonNull Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);

        if (uiModeManager != null && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            return EventBatch.ClientEngine.ANDROID_TV_SDK;
        }

        return EventBatch.ClientEngine.ANDROID_SDK;
    }

}
