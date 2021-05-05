/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
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

import android.app.Activity;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

/**
 * Tests for {@link OptimizelyManager.OptlyActivityLifecycleCallbacks}
 */
@RunWith(MockitoJUnitRunner.class)
public class OptimizelyManagerOptlyActivityLifecycleCallbacksTest {

    @Mock OptimizelyManager optimizelyManager;
    @Mock Activity activity;
    private OptimizelyManager.OptlyActivityLifecycleCallbacks optlyActivityLifecycleCallbacks;

    @Before
    public void setup() {
        optlyActivityLifecycleCallbacks = new OptimizelyManager.OptlyActivityLifecycleCallbacks(optimizelyManager);
    }

    @Test
    public void onActivityStopped() {
        optlyActivityLifecycleCallbacks.onActivityStopped(activity);
        verify(optimizelyManager).stop(activity, optlyActivityLifecycleCallbacks);
    }
}
