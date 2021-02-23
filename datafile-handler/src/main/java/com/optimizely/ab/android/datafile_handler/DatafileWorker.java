/****************************************************************************
 * Copyright 2021, Optimizely, Inc. and contributors                   *
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
package com.optimizely.ab.android.datafile_handler;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.Client;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.android.shared.OptlyStorage;

import org.slf4j.LoggerFactory;

public class DatafileWorker extends Worker {
    public static final String workerId = "DatafileWorker";
    public DatafileWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static Data getData(DatafileConfig datafileConfig) {
        return new Data.Builder().putString("DatafileConfig", datafileConfig.toJSONString()).build();
    }

    public static DatafileConfig getDataConfig(Data data) {
        String extraDatafileConfig = data.getString("DatafileConfig");
        return DatafileConfig.fromJSONString(extraDatafileConfig);
    }

    @NonNull
    @Override
    public Result doWork() {
        DatafileConfig datafileConfig = getDataConfig(getInputData());
        DatafileClient datafileClient = new DatafileClient(
                new Client(new OptlyStorage(this.getApplicationContext()), LoggerFactory.getLogger(OptlyStorage.class)),
                LoggerFactory.getLogger(DatafileClient.class));
        DatafileCache datafileCache = new DatafileCache(
                datafileConfig.getKey(),
                new Cache(this.getApplicationContext(), LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(DatafileCache.class));

        String datafileUrl = datafileConfig.getUrl();

        DatafileLoader datafileLoader = new DatafileLoader(this.getApplicationContext(), datafileClient, datafileCache, LoggerFactory.getLogger(DatafileLoader.class));
        datafileLoader.getDatafile(datafileUrl, null);

        return Result.success();
    }
}
