/****************************************************************************
 * Copyright 2016, Optimizely, Inc. and contributors                        *
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

package com.optimizely.ab.android.user_profile;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.core.deps.guava.util.concurrent.ListeningExecutorService;
import android.support.test.espresso.core.deps.guava.util.concurrent.MoreExecutors;
import android.support.test.runner.AndroidJUnit4;

import com.optimizely.ab.android.shared.Cache;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AndroidUserProfile}
 */
@RunWith(AndroidJUnit4.class)
@RequiresApi(Build.VERSION_CODES.HONEYCOMB)
public class AndroidUserProfileTest {

    private AndroidUserProfile androidUserProfile;
    private UserProfileCache diskUserProfileCache;
    private Map<String, Map<String, String>> memoryUserProfileCache = new HashMap<>();
    private AndroidUserProfile.WriteThroughCacheTaskFactory writeThroughCacheFactory;
    private Logger logger;
    private ListeningExecutorService executor;
    private Cache cache;

    @Before
    public void setup() {
        logger = mock(Logger.class);
        cache = new Cache(InstrumentationRegistry.getTargetContext(), logger);
        diskUserProfileCache = new UserProfileCache("1", cache, logger);
        executor = MoreExecutors.newDirectExecutorService();
        writeThroughCacheFactory = new AndroidUserProfile.WriteThroughCacheTaskFactory(diskUserProfileCache, memoryUserProfileCache, executor, logger);
        androidUserProfile = new AndroidUserProfile(diskUserProfileCache,
                writeThroughCacheFactory, logger);
    }

    @After
    public void teardown() {
        cache.delete(diskUserProfileCache.getFileName());
    }

    @Test
    public void saveActivation() {
        String userId = "user1";
        String expKey = "exp1";
        String varKey = "var1";
        assertTrue(androidUserProfile.save(userId, expKey, varKey));
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Time out");
        }
        assertEquals(varKey, androidUserProfile.lookup(userId, expKey));
    }

    @Test
    public void saveActivationNullUserId() {
        assertFalse(androidUserProfile.save(null, "exp1", "var1"));
        verify(logger).error("Received null userId, unable to save activation");
    }

    @Test
    public void saveActivationNullExperimentKey() {
        assertFalse(androidUserProfile.save("foo", null, "var1"));
        verify(logger).error("Received null experiment key, unable to save activation");
    }

    @Test
    public void saveActivationNullVariationKey() {
        assertFalse(androidUserProfile.save("foo", "exp1", null));
        verify(logger).error("Received null variation key, unable to save activation");
    }

    @Test
    public void saveActivationEmptyUserId() {
        assertFalse(androidUserProfile.save("", "exp1", "var1"));
        verify(logger).error("Received empty user id, unable to save activation");
    }

    @Test
    public void saveActivationEmptyExperimentKey() {
        assertFalse(androidUserProfile.save("foo", "", "var1"));
        verify(logger).error("Received empty experiment key, unable to save activation");
    }

    @Test
    public void saveActivationEmptyVariationKey() {
        assertFalse(androidUserProfile.save("foo", "exp1", ""));
        verify(logger).error("Received empty variation key, unable to save activation");
    }

    @Test
    public void lookupActivationNullUserId() {
        assertNull(androidUserProfile.lookup(null, "exp1"));
        verify(logger).error("Received null user id, unable to lookup activation");
    }

    @Test
    public void lookupActivationNullExperimentKey() {
        assertNull(androidUserProfile.lookup("foo", null));
        verify(logger).error("Received null experiment key, unable to lookup activation");
    }

    @Test
    public void lookupActivationEmptyUserId() {
        assertNull(androidUserProfile.lookup("", "exp1"));
        verify(logger).error("Received empty user id, unable to lookup activation");
    }

    @Test
    public void lookupActivationEmptyExperimentKey() {
        assertNull(androidUserProfile.lookup("foo", ""));
        verify(logger).error("Received empty experiment key, unable to lookup activation");
    }

    @Test
    public void removeExistingActivation() {
        androidUserProfile.save("user1", "exp1", "var1");
        assertTrue(androidUserProfile.remove("user1", "exp1"));
        assertNull(androidUserProfile.lookup("user1", "exp1"));
    }

    @Test
    public void removeNonExistingActivation() {
        assertFalse(androidUserProfile.remove("user1", "exp1"));
    }

    @Test
    public void removeActivationNullUserId() {
        assertFalse(androidUserProfile.remove(null, "exp1"));
        verify(logger).error("Received null user id, unable to remove activation");
    }

    @Test
    public void removeActivationNullExperimentKey() {
        assertFalse(androidUserProfile.remove("foo", null));
        verify(logger).error("Received null experiment key, unable to remove activation");
    }

    @Test
    public void removeActivationEmptyUserId() {
        assertFalse(androidUserProfile.remove("", "exp1"));
        verify(logger).error("Received empty user id, unable to remove activation");
    }

    @Test
    public void removeActivationEmptyExperimentKey() {
        assertFalse(androidUserProfile.remove("foo", ""));
        verify(logger).error("Received empty experiment key, unable to remove activation");
    }

    @Test
    public void startHandlesJSONException() throws IOException {
        assertTrue(cache.save(diskUserProfileCache.getFileName(), "{"));
        androidUserProfile.start();
        verify(logger).error(eq("Unable to parse user profile cache"), any(JSONException.class));
    }

    @Test
    public void start() throws JSONException {
        androidUserProfile.start();
        androidUserProfile.save("user1", "exp1", "var1");
        androidUserProfile.save("user1", "exp2", "var2");

        Map<String, String> expKeyToVarKeyMap = new HashMap<>();
        expKeyToVarKeyMap.put("exp1", "var1");
        expKeyToVarKeyMap.put("exp2", "var2");
        Map<String, Map<String, String>> profileMap = new HashMap<>();
        profileMap.put("user1", expKeyToVarKeyMap);

        assertEquals(profileMap, memoryUserProfileCache);
    }
}
