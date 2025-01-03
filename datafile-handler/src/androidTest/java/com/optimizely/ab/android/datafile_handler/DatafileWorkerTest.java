/****************************************************************************
 * Copyright 2021, Optimizely, Inc. and contributors                        *
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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.testing.TestWorkerBuilder;

import com.optimizely.ab.android.shared.Cache;
import com.optimizely.ab.android.shared.DatafileConfig;
import com.optimizely.ab.event.LogEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Tests {@link DatafileWorker}
 */
@RunWith(AndroidJUnit4.class)
public class DatafileWorkerTest {
    private Context context;
    private Executor executor;
    private String sdkKey = "sdkKey";
    private Logger logger = LoggerFactory.getLogger("test");

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        executor = Executors.newSingleThreadExecutor();
    }

    @Test
    public void testInputData() {
        DatafileConfig datafileConfig1 = new DatafileConfig(null, sdkKey);
        Data data = DatafileWorker.getData(datafileConfig1);

        DatafileConfig datafileConfig2 = DatafileWorker.getDataConfig(data);
        assertEquals(datafileConfig2.getKey(), sdkKey);
    }

    @Test
    public void testDatafileFetch() {
        DatafileWorker worker = mockDatafileWorker(sdkKey);
        worker.datafileLoader = mock(DatafileLoader.class);

        ListenableWorker.Result result = worker.doWork();

        DatafileConfig datafileConfig = new DatafileConfig(null, sdkKey);
        String datafileUrl = datafileConfig.getUrl();
        DatafileCache datafileCache = new DatafileCache(datafileConfig.getKey(), new Cache(context, logger), logger);

        verify(worker.datafileLoader).getDatafile(eq(datafileUrl), eq(datafileCache), eq(null));
        assertThat(result, is(ListenableWorker.Result.success()));  // success
    }

    // Helpers

    DatafileWorker mockDatafileWorker(String sdkKey) {
        DatafileConfig datafileConfig = new DatafileConfig(null, sdkKey);
        Data inputData = DatafileWorker.getData(datafileConfig);

        return (DatafileWorker) TestWorkerBuilder.from(context, DatafileWorker.class, executor)
                .setInputData(inputData)
                .build();
    }

}
