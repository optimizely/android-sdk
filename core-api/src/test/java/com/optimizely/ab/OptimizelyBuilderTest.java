/**
 *
 *    Copyright 2016, Optimizely
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab;

import ch.qos.logback.classic.Level;

import com.optimizely.ab.bucketing.UserExperimentRecord;
import com.optimizely.ab.config.ProjectConfigTestUtils;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.error.NoOpErrorHandler;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.internal.LogbackVerifier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV1;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV1;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Optimizely#builder(String, EventHandler)}.
 */
@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class OptimizelyBuilderTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    @Mock private EventHandler mockEventHandler;

    @Mock private ErrorHandler mockErrorHandler;

    @Test
    public void withEventHandler() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .build();

        assertThat(optimizelyClient.eventHandler, is(mockEventHandler));
    }

    @Test
    public void projectConfigV1() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV1(), mockEventHandler)
                .build();

        ProjectConfigTestUtils.verifyProjectConfig(optimizelyClient.getProjectConfig(), validProjectConfigV1());
    }

    @Test
    public void projectConfigV2() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .build();

        ProjectConfigTestUtils.verifyProjectConfig(optimizelyClient.getProjectConfig(), validProjectConfigV2());
    }

    @Test
    public void withErrorHandler() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .withErrorHandler(mockErrorHandler)
            .build();

        assertThat(optimizelyClient.errorHandler, is(mockErrorHandler));
    }

    @Test
    public void withDefaultErrorHandler() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .build();

        assertThat(optimizelyClient.errorHandler, instanceOf(NoOpErrorHandler.class));
    }

    @Test
    public void withUserExperimentRecord() throws Exception {
        UserExperimentRecord userExperimentRecord = mock(UserExperimentRecord.class);
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .withUserExperimentRecord(userExperimentRecord)
            .build();

        assertThat(optimizelyClient.bucketer.getUserExperimentRecord(), is(userExperimentRecord));
    }

    @Test
    public void builderThrowsConfigParseExceptionForInvalidDatafile() throws Exception {
        thrown.expect(ConfigParseException.class);
        Optimizely.builder("{invalidDatafile}", mockEventHandler).build();
    }
}
