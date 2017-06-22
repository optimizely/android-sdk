/****************************************************************************
 * Copyright 2017, Optimizely, Inc. and contributors                        *
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class OptimizelyManagerBuilderTest {

    /**
     * Verify that building the {@link OptimizelyManager} with a polling interval less than 60
     * seconds defaults to 60 seconds.
     */
    @Test
    public void testBuildWithInvalidPollingInterval() {
        OptimizelyManager manager = OptimizelyManager.builder("1")
                .withDatafileDownloadInterval(5, TimeUnit.SECONDS)
                .build();

        assertEquals(60L, manager.getDatafileDownloadInterval().longValue());
        assertEquals(TimeUnit.SECONDS, manager.getDatafileDownloadIntervalTimeUnit());
    }

    /**
     * Verify that building the {@link OptimizelyManager} with a polling interval greater than 60
     * seconds is properly registered.
     */
    @Test
    public void testBuildWithValidPollingInterval() {
        OptimizelyManager manager = OptimizelyManager.builder("1")
                .withDatafileDownloadInterval(61, TimeUnit.SECONDS)
                .build();

        assertEquals(61L, manager.getDatafileDownloadInterval().longValue());
        assertEquals(TimeUnit.SECONDS, manager.getDatafileDownloadIntervalTimeUnit());
    }
}
