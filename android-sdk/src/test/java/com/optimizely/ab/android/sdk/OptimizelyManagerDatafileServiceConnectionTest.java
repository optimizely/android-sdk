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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.optimizely.ab.android.datafile_handler.DatafileService;
import com.optimizely.ab.android.datafile_handler.DatafileLoadedListener;
import com.optimizely.ab.android.datafile_handler.DatafileLoader;
import com.optimizely.ab.android.datafile_handler.DatafileServiceConnection;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DatafileServiceConnection}
 */
@RunWith(MockitoJUnitRunner.class)
public class OptimizelyManagerDatafileServiceConnectionTest {

    private DatafileServiceConnection datafileServiceConnection;
    @Mock private OptimizelyManager optimizelyManager;

    @Before
    public void setup() {
        Context context = mock(Context.class);
        datafileServiceConnection = new DatafileServiceConnection(optimizelyManager.getProjectId(), context, optimizelyManager.getDatafileLoadedListener(context,null));
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void onServiceConnected() {
        DatafileService.LocalBinder binder = mock(DatafileService.LocalBinder.class);
        DatafileService service = mock(DatafileService.class);
        Context context = mock(Context.class);
        when(service.getApplicationContext()).thenReturn(context);
        when(binder.getService()).thenReturn(service);
        when(optimizelyManager.getProjectId()).thenReturn("1");
        when(optimizelyManager.getDatafileLoadedListener(context,null)).thenReturn(mock(DatafileLoadedListener.class));
        ArgumentCaptor<DatafileLoadedListener> captor = ArgumentCaptor.forClass(DatafileLoadedListener.class);
        datafileServiceConnection = new DatafileServiceConnection(optimizelyManager.getProjectId(), context, optimizelyManager.getDatafileLoadedListener(context,null) );
        datafileServiceConnection.onServiceConnected(null, binder);
        verify(service).getDatafile(same("1"), any(DatafileLoader.class), any(DatafileLoadedListener.class));
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void onServiceConnectedNullServiceFromBinder() {
        DatafileService.LocalBinder binder = mock(DatafileService.LocalBinder.class);
        when(binder.getService()).thenReturn(null);

        try {
            datafileServiceConnection.onServiceConnected(null, binder);
        } catch (NullPointerException e) {
            fail();
        }
    }
}
