package com.optimizely.ab.android.shared;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.Operation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.TestDriver;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tests for {@link WorkerScheduler}
 */
@RunWith(AndroidJUnit4.class)
public class WorkerSchedulerTest {
    private Context context;
    private WorkManager workManager;
    private TestDriver testDriver;
    private Data inputData;
    AbstractMap.SimpleEntry<WorkRequest, Operation> enqueResult;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();

        // synchronous executor for testing support
        Configuration config = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .build();

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config);

        workManager = WorkManager.getInstance(context);
        testDriver = WorkManagerTestInitHelper.getTestDriver(context);

        // static counter to trace periodic work execution
        TestWorker.count = 0;

        inputData = new Data.Builder().putString("key-1", "value-1").build();
    }

    @Test
    public void scheduleService() throws Exception {
        WorkerScheduler.requestOnlyWhenConnected = false;       // do not add Connected constraint

        enqueResult = WorkerScheduler.scheduleService(context, TestWorker.workerId, TestWorker.class, inputData, 10);
        WorkRequest workRequest = (WorkRequest) enqueResult.getKey();
        Operation operation = (Operation) enqueResult.getValue();
        UUID workId = workRequest.getId();

        // block until enqueue completed
        operation.getResult().get();

        // SynchronousExecutor starts doWork() immediately.
        // block until execution completed.
        WorkInfo workInfo = workManager.getWorkInfoById(workId).get();

        // Peridic Work state: ENQUEUED -> RUNNING -> ENQUEUED ->...
        // workInfo returns only for ENQUEUED since it's blocked till completed synchronously.
        assertThat(workInfo.getState(), is(WorkInfo.State.ENQUEUED));
        // work not started due to initial delay and period delay.
        assertEquals(TestWorker.count, 0);

        // set delay conditions met
        testDriver.setPeriodDelayMet(workId);
        testDriver.setInitialDelayMet(workId);

        // wait until work completed synchronously.
        workInfo = workManager.getWorkInfoById(workId).get();
        // work excution count incremented
        assertEquals(TestWorker.count, 1);
        assertThat(workInfo.getState(), is(WorkInfo.State.ENQUEUED));
    }

    @Test
    public void scheduleService_connectionDown() throws Exception {
        WorkerScheduler.requestOnlyWhenConnected = true;       // add Connected constraint

        enqueResult = WorkerScheduler.scheduleService(context, TestWorker.workerId, TestWorker.class, inputData, 10);
        WorkRequest workRequest = (WorkRequest) enqueResult.getKey();
        Operation operation = (Operation) enqueResult.getValue();
        UUID workId = workRequest.getId();

        // block until enqueue completed
        operation.getResult().get();

        // SynchronousExecutor starts doWork() immediately.
        // block until execution completed.
        WorkInfo workInfo = workManager.getWorkInfoById(workId).get();

        // Peridic Work state: ENQUEUED -> RUNNING -> ENQUEUED ->...
        // workInfo returns only for ENQUEUED since it's blocked till completed synchronously.
        assertThat(workInfo.getState(), is(WorkInfo.State.ENQUEUED));
        // work not started due to initial delay and period delay.
        assertEquals(TestWorker.count, 0);

        // set delay conditions met
        testDriver.setPeriodDelayMet(workId);
        testDriver.setInitialDelayMet(workId);

        // work is not executed because of Connected constraint
        workInfo = workManager.getWorkInfoById(workId).get();
        assertEquals(TestWorker.count, 0);

        // constraints met. SynchronousExecutor starts doWork() immediately.
        testDriver.setAllConstraintsMet(workId);

        // block until execution completed.
        workInfo = workManager.getWorkInfoById(workId).get();
        assertEquals(TestWorker.count, 1);
        assertThat(workInfo.getState(), is(WorkInfo.State.ENQUEUED));
    }

    @Test
    public void startService() throws Exception {
        WorkerScheduler.requestOnlyWhenConnected = false;   // do not add Connected constraint

        enqueResult = WorkerScheduler.startService(context, TestWorker.workerId, TestWorker.class, inputData, -1L);
        WorkRequest workRequest = (WorkRequest) enqueResult.getKey();
        Operation operation = (Operation) enqueResult.getValue();
        UUID workId = workRequest.getId();

        // block until enqueue completed
        operation.getResult().get();

        // SynchronousExecutor starts doWork() immediately.
        // block until execution completed.
        WorkInfo workInfo = workManager.getWorkInfoById(workId).get();

        // One-time Work state: ENQUEUED -> RUNNING -> SUCCESS/FAILURE/RETRY
        assertThat(workInfo.getState(), is(WorkInfo.State.SUCCEEDED));
        assertEquals(workInfo.getOutputData(), inputData);
    }

    @Test
    public void startService_connectionDown() throws Exception {
        WorkerScheduler.requestOnlyWhenConnected = true;   // add Connected constraint

        enqueResult = WorkerScheduler.startService(context, TestWorker.workerId, TestWorker.class, inputData, -1L);
        WorkRequest workRequest = (WorkRequest) enqueResult.getKey();
        Operation operation = (Operation) enqueResult.getValue();
        UUID workId = workRequest.getId();

        // block until enqueue completed
        operation.getResult().get();

        // SynchronousExecutor starts doWork() immediately.
        // block until execution completed, but work not started because of Connected constraint
        WorkInfo workInfo = workManager.getWorkInfoById(workId).get();
        assertThat(workInfo.getState(), is(WorkInfo.State.ENQUEUED));

        // constraints met. SynchronousExecutor starts doWork() immediately.
        testDriver.setAllConstraintsMet(workId);

        // block until execution completed.
        workInfo = workManager.getWorkInfoById(workId).get();
        assertThat(workInfo.getState(), is(WorkInfo.State.SUCCEEDED));
        assertEquals(workInfo.getOutputData(), inputData);
    }

}
