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

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.optimizely.ab.android.datafile_handler.DatafileLoadedListener
import com.optimizely.ab.android.datafile_handler.DatafileLoader
import com.optimizely.ab.android.datafile_handler.DatafileService
import com.optimizely.ab.android.datafile_handler.DatafileService.LocalBinder
import com.optimizely.ab.android.datafile_handler.DatafileServiceConnection
import com.optimizely.ab.android.shared.DatafileConfig
import junit.framework.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

/**
 * Tests [DatafileServiceConnection]
 */
@RunWith(PowerMockRunner::class)
@PrepareForTest(OptimizelyManager::class, LocalBinder::class, DatafileService::class)
class OptimizelyManagerDatafileServiceConnectionTest {
    @Mock
    var optimizelyManager: OptimizelyManager? = null

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    fun onServiceConnected() {
        val binder : LocalBinder = PowerMockito.mock(LocalBinder::class.java)
        val service = PowerMockito.mock(DatafileService::class.java)
        val context = PowerMockito.mock(Context::class.java)
        PowerMockito.`when`(context.applicationContext).thenReturn(context)
        PowerMockito.`when`(binder.service).thenReturn(service)
        PowerMockito.`when`(optimizelyManager?.datafileConfig).thenReturn(DatafileConfig("1", null as String?))
        PowerMockito.`when`(optimizelyManager?.getDatafileLoadedListener(context, null)).thenReturn(Mockito.mock(DatafileLoadedListener::class.java))
        val captor = ArgumentCaptor.forClass(DatafileLoadedListener::class.java)
        val datafileServiceConnection = DatafileServiceConnection(optimizelyManager?.datafileConfig!!, context, optimizelyManager!!.getDatafileLoadedListener(context, null))
        datafileServiceConnection.onServiceConnected(null, binder)
        val sameString = optimizelyManager?.datafileConfig!!.url
        Mockito.verify(service).getDatafile(Matchers.eq(sameString), Matchers.any(DatafileLoader::class.java), Matchers.any(DatafileLoadedListener::class.java))
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Test
    fun onServiceConnectedNullServiceFromBinder() {
        val context = PowerMockito.mock(Context::class.java)
        val binder: LocalBinder = PowerMockito.mock(LocalBinder::class.java)
        PowerMockito.`when`(binder.service).thenReturn(null)
        PowerMockito.`when`(optimizelyManager?.datafileConfig).thenReturn(DatafileConfig("1", null as String?))
        PowerMockito.`when`(optimizelyManager?.getDatafileLoadedListener(context, null)).thenReturn(Mockito.mock(DatafileLoadedListener::class.java))

        val datafileServiceConnection = DatafileServiceConnection(optimizelyManager?.datafileConfig!!, context, optimizelyManager!!.getDatafileLoadedListener(context, null))

        try {
            datafileServiceConnection.onServiceConnected(null, binder)
        } catch (e: NullPointerException) {
            Assert.fail()
        }
    }
}