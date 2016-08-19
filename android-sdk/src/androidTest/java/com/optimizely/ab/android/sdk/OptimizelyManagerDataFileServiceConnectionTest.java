package com.optimizely.ab.android.sdk;

import android.content.Context;

import com.optimizely.ab.android.shared.ServiceScheduler;
import com.optimizely.user_experiment_record.AndroidUserExperimentRecord;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 8/16/16 for Optimizely.
 *
 * Tests {@link OptimizelyManager.DataFileServiceConnection}
 */
public class OptimizelyManagerDataFileServiceConnectionTest {

    OptimizelyManager.DataFileServiceConnection dataFileServiceConnection;
    OptimizelyManager optimizelyManager;

    @Before
    public void setup() {
        optimizelyManager = mock(OptimizelyManager.class);
        dataFileServiceConnection = new OptimizelyManager.DataFileServiceConnection(optimizelyManager);
    }

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
        verify(optimizelyManager).injectOptimizely(any(Context.class), any(AndroidUserExperimentRecord.class), any(ServiceScheduler.class), eq(""));
    }

    @Test
    public void onServiceConnectedNotBoundWhenDataFileLoaded() {
        DataFileService.LocalBinder binder = mock(DataFileService.LocalBinder.class);
        DataFileService service = mock(DataFileService.class);
        Context context = mock(Context.class);
        when(service.getApplicationContext()).thenReturn(context);
        when(binder.getService()).thenReturn(service);
        when(optimizelyManager.getProjectId()).thenReturn("1");
        ArgumentCaptor<DataFileLoadedListener> captor = ArgumentCaptor.forClass(DataFileLoadedListener.class);
        dataFileServiceConnection.onServiceConnected(null, binder);
        verify(service).getDataFile(same("1"), any(DataFileLoader.class), captor.capture());

        dataFileServiceConnection.onServiceDisconnected(null);

        DataFileLoadedListener listener = captor.getValue();
        listener.onDataFileLoaded("");

        verify(optimizelyManager, never()).injectOptimizely(any(Context.class), any(AndroidUserExperimentRecord.class), any(ServiceScheduler.class), eq(""));
    }

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
