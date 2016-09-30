/**
 * Copyright 2016, Optimizely
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.android.sdk;

import android.app.Activity;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by jdeffibaugh on 8/16/16 for Optimizely.
 *
 * Tests for {@link OptimizelyManager.OptlyActivityLifecycleCallbacks}
 */
@RunWith(AndroidJUnit4.class)
public class OptimizelyManagerOptlyActivityLifecycleCallbacksTest {

    OptimizelyManager.OptlyActivityLifecycleCallbacks optlyActivityLifecycleCallbacks;
    OptimizelyManager optimizelyManager;

    @Before
    public void setup() {
        optimizelyManager = mock(OptimizelyManager.class);
        optlyActivityLifecycleCallbacks = new OptimizelyManager.OptlyActivityLifecycleCallbacks(optimizelyManager);
    }

    @Test
    public void onActivityStopped() {
        Activity activity = mock(Activity.class);
        optlyActivityLifecycleCallbacks.onActivityStopped(activity);
        verify(optimizelyManager).stop(activity, optlyActivityLifecycleCallbacks);
    }
}
