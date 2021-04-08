/****************************************************************************
 * Copyright 2021, Optimizely, Inc. and contributors                   *
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
package com.optimizely.ab.android.datafile_handler

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.impl.utils.futures.SettableFuture
import com.google.common.util.concurrent.ListenableFuture
import com.optimizely.ab.android.datafile_handler.DatafileCache
import com.optimizely.ab.android.datafile_handler.DatafileClient
import com.optimizely.ab.android.datafile_handler.DatafileLoader
import com.optimizely.ab.android.shared.Cache
import com.optimizely.ab.android.shared.Client
import com.optimizely.ab.android.shared.DatafileConfig
import com.optimizely.ab.android.shared.OptlyStorage
import org.slf4j.LoggerFactory

class DatafileWorker(context: Context, workerParams: WorkerParameters) : ListenableWorker(context, workerParams) {
    @SuppressLint("RestrictedApi")
    var mFuture: SettableFuture<Result> = SettableFuture.create()


    override fun startWork(): ListenableFuture<Result> {
        val datafileConfig = getDataConfig(inputData)
        val datafileClient = DatafileClient(
                Client(OptlyStorage(this.applicationContext), LoggerFactory.getLogger(OptlyStorage::class.java)),
                LoggerFactory.getLogger(DatafileClient::class.java))
        val datafileCache = DatafileCache(
                datafileConfig?.key!!,
                Cache(this.applicationContext, LoggerFactory.getLogger(Cache::class.java)),
                LoggerFactory.getLogger(DatafileCache::class.java))
        val datafileUrl = datafileConfig?.url
        val datafileLoader = DatafileLoader(this.applicationContext, datafileClient, datafileCache, LoggerFactory.getLogger(DatafileLoader::class.java))
        datafileLoader.getDatafile(datafileUrl!!, null)
        return mFuture
    }

    companion object {
        const val workerId = "DatafileWorker"
        @JvmStatic
        fun getData(datafileConfig: DatafileConfig): Data {
            return Data.Builder().putString("DatafileConfig", datafileConfig.toJSONString()).build()
        }

        fun getDataConfig(data: Data): DatafileConfig? {
            val extraDatafileConfig = data.getString("DatafileConfig")
            return DatafileConfig.fromJSONString(extraDatafileConfig)
        }
    }
}