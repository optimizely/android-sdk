/**
 *
 *    Copyright 2016-2017, Optimizely and contributors
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

import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.ProjectConfigTestUtils;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.error.NoOpErrorHandler;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.internal.BuildVersionInfo;
import com.optimizely.ab.event.internal.EventBuilderV2;
import com.optimizely.ab.event.internal.payload.Event.ClientEngine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigJsonV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.noAudienceProjectConfigV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validConfigJsonV3;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV2;
import static com.optimizely.ab.config.ProjectConfigTestUtils.validProjectConfigV3;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Optimizely#builder(String, EventHandler)}.
 */
@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class OptimizelyBuilderTest {


    private static final String userId = "userId";
    private static String noAudienceDatafile;
    private static ProjectConfig noAudienceProjectConfig;

    @BeforeClass
    public static void setUp() throws Exception {
        noAudienceDatafile = noAudienceProjectConfigJsonV2();
        noAudienceProjectConfig = noAudienceProjectConfigV2();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock private EventHandler mockEventHandler;

    @Mock private ErrorHandler mockErrorHandler;

    @Test
    public void withEventHandler() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .build();

        assertThat(optimizelyClient.eventHandler, is(mockEventHandler));
    }

    @Test
    public void projectConfigV2() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .build();

        ProjectConfigTestUtils.verifyProjectConfig(optimizelyClient.getProjectConfig(), validProjectConfigV2());
    }

    @Test
    public void projectConfigV3() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV3(), mockEventHandler)
            .build();

        ProjectConfigTestUtils.verifyProjectConfig(optimizelyClient.getProjectConfig(), validProjectConfigV3());
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
    public void withUserProfileService() throws Exception {
        UserProfileService userProfileService = mock(UserProfileService.class);
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .withUserProfileService(userProfileService)
            .build();

        assertThat(optimizelyClient.getUserProfileService(), is(userProfileService));
    }

    @Test
    public void withDefaultClientEngine() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .build();

        assertThat(((EventBuilderV2)optimizelyClient.eventBuilder).clientEngine, is(ClientEngine.JAVA_SDK));
    }

    @Test
    public void withAndroidSDKClientEngine() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .withClientEngine(ClientEngine.ANDROID_SDK)
            .build();

        assertThat(((EventBuilderV2)optimizelyClient.eventBuilder).clientEngine, is(ClientEngine.ANDROID_SDK));
    }

    @Test
    public void withAndroidTVSDKClientEngine() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .withClientEngine(ClientEngine.ANDROID_TV_SDK)
            .build();

        assertThat(((EventBuilderV2)optimizelyClient.eventBuilder).clientEngine, is(ClientEngine.ANDROID_TV_SDK));
    }

    @Test
    public void withDefaultClientVersion() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .build();

        assertThat(((EventBuilderV2)optimizelyClient.eventBuilder).clientVersion, is(BuildVersionInfo.VERSION));
    }

    @Test
    public void withCustomClientVersion() throws Exception {
        Optimizely optimizelyClient = Optimizely.builder(validConfigJsonV2(), mockEventHandler)
            .withClientVersion("0.0.0")
            .build();

        assertThat(((EventBuilderV2)optimizelyClient.eventBuilder).clientVersion, is("0.0.0"));
    }

    @SuppressFBWarnings(value="NP_NONNULL_PARAM_VIOLATION", justification="Testing nullness contract violation")
    @Test
    public void builderThrowsConfigParseExceptionForNullDatafile() throws Exception {
        thrown.expect(ConfigParseException.class);
        Optimizely.builder(null, mockEventHandler).build();
    }

    @Test
    public void builderThrowsConfigParseExceptionForEmptyDatafile() throws Exception {
        thrown.expect(ConfigParseException.class);
        Optimizely.builder("", mockEventHandler).build();
    }

    @Test
    public void builderThrowsConfigParseExceptionForInvalidDatafile() throws Exception {
        thrown.expect(ConfigParseException.class);
        Optimizely.builder("{invalidDatafile}", mockEventHandler).build();
    }
}
