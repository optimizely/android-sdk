package com.optimizely.ab.android.datafile_handler;

import android.content.Context;

import androidx.annotation.NonNull;
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

    @NonNull
    @Override
    public Result doWork() {
        String extraDatafileConfig = getInputData().getString("DatafileConfig");
        DatafileConfig datafileConfig = DatafileConfig.fromJSONString(extraDatafileConfig);
        DatafileClient datafileClient = new DatafileClient(
                new Client(new OptlyStorage(this.getApplicationContext()), LoggerFactory.getLogger(OptlyStorage.class)),
                LoggerFactory.getLogger(DatafileClient.class));
        DatafileCache datafileCache = new DatafileCache(
                datafileConfig.getKey(),
                new Cache(this.getApplicationContext(), LoggerFactory.getLogger(Cache.class)),
                LoggerFactory.getLogger(DatafileCache.class));

        String datafileUrl = datafileConfig.getUrl();
        final String[] df = {null};

        final Worker worker = this;

        DatafileLoader datafileLoader = new DatafileLoader(this.getApplicationContext(), datafileClient, datafileCache, LoggerFactory.getLogger(DatafileLoader.class));
        datafileLoader.getDatafile(datafileUrl, null);

        return Result.success();
    }
}
