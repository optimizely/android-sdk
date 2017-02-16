/****************************************************************************
 * Copyright 2016-2017, Optimizely, Inc. and contributors                        *
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
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.optimizely.ab.event.internal.payload.Event;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OptimizelyClientEngineTest {
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB_MR2)
    @Test
    public void testGetClientEngineFromContextAndroidTV() {
        Context context = mock(Context.class);
        UiModeManager uiModeManager = mock(UiModeManager.class);
        when(context.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(uiModeManager);
        when(uiModeManager.getCurrentModeType()).thenReturn(Configuration.UI_MODE_TYPE_TELEVISION);
        assertEquals(Event.ClientEngine.ANDROID_TV_SDK, OptimizelyClientEngine.getClientEngineFromContext(context));
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB_MR2)
    @Test
    public void testGetClientEngineFromContextAndroid() {
        Context context = mock(Context.class);
        UiModeManager uiModeManager = mock(UiModeManager.class);
        when(context.getSystemService(Context.UI_MODE_SERVICE)).thenReturn(uiModeManager);
        when(uiModeManager.getCurrentModeType()).thenReturn(Configuration.UI_MODE_TYPE_NORMAL);
        assertEquals(Event.ClientEngine.ANDROID_SDK, OptimizelyClientEngine.getClientEngineFromContext(context));
    }
}
