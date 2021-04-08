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

import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner
import android.app.Activity
import com.optimizely.ab.android.sdk.OptimizelyManager.OptlyActivityLifecycleCallbacks
import androidx.annotation.RequiresApi
import android.os.Build
import com.optimizely.ab.event.BatchEventProcessor
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

/**
 * Tests for [OptimizelyManager.OptlyActivityLifecycleCallbacks]
 */
@RunWith(PowerMockRunner::class)
@PrepareForTest(OptimizelyManager::class)
class OptimizelyManagerOptlyActivityLifecycleCallbacksTest {
    @Mock
    var optimizelyManager: OptimizelyManager? = null

    @Mock
    var activity: Activity? = null
    private var optlyActivityLifecycleCallbacks: OptlyActivityLifecycleCallbacks? = null
    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Before
    fun setup() {
        optlyActivityLifecycleCallbacks = OptlyActivityLifecycleCallbacks(optimizelyManager!!)
    }

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    fun onActivityStopped() {
        optlyActivityLifecycleCallbacks!!.onActivityStopped(activity!!)
        Mockito.verify(optimizelyManager)?.stop(activity!!, optlyActivityLifecycleCallbacks!!)
    }
}