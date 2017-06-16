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
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.app.Service;
import android.support.annotation.RequiresApi;

import com.optimizely.ab.android.shared.DataFileLoadedListener;
import com.optimizely.ab.android.shared.ReflectionUtils;
import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.ab.bucketing.UserProfileService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests DataFileServiceConnection
 */
@RunWith(MockitoJUnitRunner.class)
public class OptimizelyManagerDataFileServiceConnectionTest {

    private ServiceConnection dataFileServiceConnection;
    @Mock private OptimizelyManager optimizelyManager;

    @Before
    public void setup() {
        dataFileServiceConnection = optimizelyManager.getDataFileServiceConnection(optimizelyManager.getProjectId(), mock(Context.class), mock(DataFileLoadedListener.class));
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void onServiceConnected() {
        IBinder binder = (IBinder) mock(ReflectionUtils.getClass("com.optimizely.ab.android.datafile_handler.DataFileService.LocalBinder", this.getClass().getClassLoader()));
        Service service = (Service) mock(ReflectionUtils.getClass("com.optimizely.ab.android.datafile_handler.DataFileService", this.getClass().getClassLoader()));
        Context context = mock(Context.class);
        when(service.getApplicationContext()).thenReturn(context);
        when(ReflectionUtils.callMethod(binder, "getService", ReflectionUtils.emptyArgTypes, ReflectionUtils.emptyArgs)).thenReturn(service);
        when(optimizelyManager.getProjectId()).thenReturn("1");
        ArgumentCaptor<DataFileLoadedListener> captor = ArgumentCaptor.forClass(DataFileLoadedListener.class);
        dataFileServiceConnection.onServiceConnected(null, binder);
        Class[] argTypes = {String.class, ReflectionUtils.getClass("com.optmizely.ab.android.datafile_handler.DataFileLoader", this.getClass().getClassLoader()), DataFileLoadedListener.class};
        ReflectionUtils.callMethod(verify(service), "getDataFile", argTypes, same("1"), any(ReflectionUtils.getClass("com.optmizely.ab.android.datafile_handler.DataFileLoader", this.getClass().getClassLoader())), captor.capture());
        //verify(service).getDataFile(same("1"), any(ReflectionUtils.getClass("com.optmizely.ab.android.datafile_handler.DataFileLoader", this.getClass().getClassLoader()), captor.capture());
        DataFileLoadedListener listener = captor.getValue();
        listener.onDataFileLoaded("");
        verify(optimizelyManager).injectOptimizely(any(Context.class), any(UserProfileService.class),
                any(ServiceScheduler.class), eq(""));
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void onServiceConnectedNullServiceFromBinder() {
        IBinder binder = (IBinder) mock(ReflectionUtils.getClass("com.optimizely.ab.android.datafile_handler.DataFileService.LocalBinder", this.getClass().getClassLoader()));
        when(ReflectionUtils.callMethod(binder, "getService", ReflectionUtils.emptyArgTypes, ReflectionUtils.emptyArgs)).thenReturn(null);

        try {
            dataFileServiceConnection.onServiceConnected(null, binder);
        } catch (NullPointerException e) {
            fail();
        }
    }
}
