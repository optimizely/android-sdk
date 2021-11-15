package com.optimizely.ab.android.shared;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class TestWorker extends Worker {
    public static final String workerId = "TestWorker";

    // static counter to trace periodic work execution
    public static int count = 0;

    public TestWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    public Result doWork() {
        count++;

        Data input = getInputData();
        if (input.size() == 0) {
            return Result.failure();
        } else {
            return Result.success(input);
        }
    }
}
