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
import android.app.Application;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OptimizelyManager.OptlyActivityLifecycleCallbacks}
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore
public class OptimizelyManagerOptlyActivityLifecycleCallbacksTest {

    @Mock OptimizelyManager optimizelyManager;
    @Mock Activity activity;
    private OptimizelyManager.OptlyActivityLifecycleCallbacks optlyActivityLifecycleCallbacks;

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Before
    public void setup() {
        Logger mockLogger = mock(Logger.class);
        try {
            FieldSetter fieldSetter = new FieldSetter(optimizelyManager,
                    OptimizelyManager.class.getDeclaredField("logger"));
            fieldSetter.set(mockLogger);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        optlyActivityLifecycleCallbacks = new OptimizelyManager.OptlyActivityLifecycleCallbacks(optimizelyManager);
        when(activity.getApplication()).thenReturn(new Application());
    }

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void onActivityStopped() {
        optlyActivityLifecycleCallbacks.onActivityStopped(activity);
        verify(optimizelyManager).stop(activity, optlyActivityLifecycleCallbacks);
    }
}
