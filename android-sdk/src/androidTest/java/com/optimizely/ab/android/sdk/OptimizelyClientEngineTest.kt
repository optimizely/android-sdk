/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
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

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.optimizely.ab.android.sdk.OptimizelyClientEngine.getClientEngineFromContext
import com.optimizely.ab.event.internal.payload.EventBatch
import junit.framework.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class OptimizelyClientEngineTest {
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB_MR2)
    @Test
    fun testGetClientEngineFromContextAndroidTV() {
        val context = Mockito.mock(Context::class.java)
        val uiModeManager = Mockito.mock(UiModeManager::class.java)
        Mockito.`when`(context.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(uiModeManager)
        Mockito.`when`(uiModeManager.currentModeType).thenReturn(Configuration.UI_MODE_TYPE_TELEVISION)
        Assert.assertEquals(EventBatch.ClientEngine.ANDROID_TV_SDK, getClientEngineFromContext(context))
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB_MR2)
    @Test
    fun testGetClientEngineFromContextAndroid() {
        val context = Mockito.mock(Context::class.java)
        val uiModeManager = Mockito.mock(UiModeManager::class.java)
        Mockito.`when`(context.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(uiModeManager)
        Mockito.`when`(uiModeManager.currentModeType).thenReturn(Configuration.UI_MODE_TYPE_NORMAL)
        Assert.assertEquals(EventBatch.ClientEngine.ANDROID_SDK, getClientEngineFromContext(context))
    }
}