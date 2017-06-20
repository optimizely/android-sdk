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

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.ab.android.user_profile.AndroidUserProfileServiceDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.optimizely.ab.android.datafile_handler.DataFileService;
import com.optimizely.ab.android.datafile_handler.DataFileLoadedListener;
import com.optimizely.ab.android.datafile_handler.DataFileLoader;
import com.optimizely.ab.android.datafile_handler.DataFileServiceConnection;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link OptimizelyManager.DataFileServiceConnection}
 */
@RunWith(MockitoJUnitRunner.class)
public class OptimizelyManagerDataFileServiceConnectionTest {

    private DataFileServiceConnection dataFileServiceConnection;
    @Mock private OptimizelyManager optimizelyManager;

    @Before
    public void setup() {
        Context context = mock(Context.class);
        dataFileServiceConnection = new DataFileServiceConnection(optimizelyManager.getProjectId(), context, optimizelyManager.getDataFileLoadedListener(context));
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void onServiceConnected() {
        DataFileService.LocalBinder binder = mock(DataFileService.LocalBinder.class);
        DataFileService service = mock(DataFileService.class);
        Context context = mock(Context.class);
        when(service.getApplicationContext()).thenReturn(context);
        when(binder.getService()).thenReturn(service);
        when(optimizelyManager.getProjectId()).thenReturn("1");
        ArgumentCaptor<DataFileLoadedListener> captor = ArgumentCaptor.forClass(DataFileLoadedListener.class);
        dataFileServiceConnection.onServiceConnected(null, binder);
        verify(service).getDataFile(same("1"), any(DataFileLoader.class), captor.capture());
        DataFileLoadedListener listener = captor.getValue();
        listener.onDataFileLoaded("");
        verify(optimizelyManager).injectOptimizely(any(Context.class), any(AndroidUserProfileServiceDefault.class), eq(""));
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void onServiceConnectedNullServiceFromBinder() {
        DataFileService.LocalBinder binder = mock(DataFileService.LocalBinder.class);
        when(binder.getService()).thenReturn(null);

        try {
            dataFileServiceConnection.onServiceConnected(null, binder);
        } catch (NullPointerException e) {
            fail();
        }
    }
}
