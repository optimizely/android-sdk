package com.optimizely.user_experiment_record;

import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 8/8/16 for Optimizely.
 *
 * Tests for {@link AndroidUserExperimentRecord}
 */
@RunWith(AndroidJUnit4.class)
public class AndroidUserExperimentRecordTest {

    AndroidUserExperimentRecord androidUserExperimentRecord;
    UserExperimentRecordCache diskUserExperimentRecordCache;
    Map<String, Map<String, String>> memoryUserExperimentRecordCache = new HashMap<>();
    AndroidUserExperimentRecord.WriteThroughCacheTaskFactory writeThroughCacheFactory;
    Logger logger;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        diskUserExperimentRecordCache = mock(UserExperimentRecordCache.class);
        logger = mock(Logger.class);
        writeThroughCacheFactory = mock(AndroidUserExperimentRecord.WriteThroughCacheTaskFactory.class);
        androidUserExperimentRecord = new AndroidUserExperimentRecord(diskUserExperimentRecordCache,
                writeThroughCacheFactory, logger);
        when(writeThroughCacheFactory.getMemoryUserExperimentRecordCache()).thenReturn(memoryUserExperimentRecordCache);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void saveActivation() {
        String userId = "foo";
        String expKey = "exp1";
        String varKey = "var1";
        assertTrue(androidUserExperimentRecord.save(userId, expKey, varKey));
        verify(writeThroughCacheFactory).startWriteCacheTask(userId, expKey, varKey);
    }

    @Test
    public void saveActivationNullUserId() {
        assertFalse(androidUserExperimentRecord.save(null, "exp1", "var1"));
        verify(logger).error("Received null userId, unable to save activation");
    }

    @Test
    public void saveActivationNullExperimentKey() {
        assertFalse(androidUserExperimentRecord.save("foo", null, "var1"));
        verify(logger).error("Received null experiment key, unable to save activation");
    }

    @Test
    public void saveActivationNullVariationKey() {
        assertFalse(androidUserExperimentRecord.save("foo", "exp1", null));
        verify(logger).error("Received null variation key, unable to save activation");
    }

    @Test
    public void saveActivationEmptyUserId() {
        assertFalse(androidUserExperimentRecord.save("", "exp1", "var1"));
        verify(logger).error("Received empty user id, unable to save activation");
    }

    @Test
    public void saveActivationEmptyExperimentKey() {
        assertFalse(androidUserExperimentRecord.save("foo", "", "var1"));
        verify(logger).error("Received empty experiment key, unable to save activation");
    }

    @Test
    public void saveActivationEmptyVariationKey() {
        assertFalse(androidUserExperimentRecord.save("foo", "exp1", ""));
        verify(logger).error("Received empty variation key, unable to save activation");
    }

    @Test
    public void lookupActivationNullUserId() {
        assertNull(androidUserExperimentRecord.lookup(null, "exp1"));
        verify(logger).error("Received null user id, unable to lookup activation");
    }

    @Test
    public void lookupActivationNullExperimentKey() {
        assertNull(androidUserExperimentRecord.lookup("foo", null));
        verify(logger).error("Received null experiment key, unable to lookup activation");
    }

    @Test
    public void lookupActivationEmptyUserId() {
        assertNull(androidUserExperimentRecord.lookup("", "exp1"));
        verify(logger).error("Received empty user id, unable to lookup activation");
    }

    @Test
    public void lookupActivationEmptyExperimentKey() {
        assertNull(androidUserExperimentRecord.lookup("foo", ""));
        verify(logger).error("Received empty experiment key, unable to lookup activation");
    }

    @Test
    public void lookupActivation() throws JSONException {
        Map<String,String> activation = new HashMap<>();
        activation.put("exp1", "var1");
        memoryUserExperimentRecordCache.put("foo", activation);

        assertEquals("var1", androidUserExperimentRecord.lookup("foo", "exp1"));
    }

    @Test
    public void lookupActivationNoExp() throws JSONException {
        String expKey = "exp1";
        JSONObject activation = new JSONObject();
        JSONObject expIdToVarIdDict = new JSONObject();
        activation.put("foo", expIdToVarIdDict);
        when(diskUserExperimentRecordCache.load()).thenReturn(activation);

        assertNull(androidUserExperimentRecord.lookup("foo", expKey));
        verify(logger).error("Project config did not contain matching experiment and variation ids");
    }

    @Test
    public void lookupActivationNoVar() throws JSONException {
        String expKey = "exp1";
        JSONObject activation = new JSONObject();
        JSONObject expIdToVarIdDict = new JSONObject();
        expIdToVarIdDict.put("exp1", null);
        activation.put("foo", expIdToVarIdDict);
        when(diskUserExperimentRecordCache.load()).thenReturn(activation);

        assertNull(androidUserExperimentRecord.lookup("foo", expKey));
        verify(logger).error("Project config did not contain matching experiment and variation ids");
    }

    @Test
    public void removeExistingActivation() {
        Map<String,String> activation = new HashMap<>();
        activation.put("exp1", "var1");
        memoryUserExperimentRecordCache.put("foo", activation);

        assertTrue(androidUserExperimentRecord.remove("foo", "exp1"));
        verify(writeThroughCacheFactory).startRemoveCacheTask("foo", "exp1", "var1");
    }

    @Test
    public void removeNonExistingActivation() {
        Map<String,String> activation = new HashMap<>();
        activation.put("exp2", "var1");
        memoryUserExperimentRecordCache.put("foo", activation);

        assertTrue(androidUserExperimentRecord.remove("foo", "exp1"));
        verify(writeThroughCacheFactory, never()).startRemoveCacheTask("foo", "exp1", "var1");
    }

    @Test
    public void removeActivationNullUserId() {
        assertFalse(androidUserExperimentRecord.remove(null, "exp1"));
        verify(logger).error("Received null user id, unable to remove activation");
    }

    @Test
    public void removeActivationNullExperimentKey() {
        assertFalse(androidUserExperimentRecord.remove("foo", null));
        verify(logger).error("Received null experiment key, unable to remove activation");
    }

    @Test
    public void removeActivationEmptyUserId() {
        assertFalse(androidUserExperimentRecord.remove("", "exp1"));
        verify(logger).error("Received empty user id, unable to remove activation");
    }

    @Test
    public void removeActivationEmptyExperimentKey() {
        assertFalse(androidUserExperimentRecord.remove("foo", ""));
        verify(logger).error("Received empty experiment key, unable to remove activation");
    }

    @Test
    public void startHandlesJSONException() throws JSONException {
        final JSONException jsonException = new JSONException("");
        when(diskUserExperimentRecordCache.load()).thenThrow(jsonException);
        try {
            androidUserExperimentRecord.start();
            verify(logger).error("Unable to parse user experiment record cache", jsonException);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void start() throws JSONException {
        JSONObject expKeyToVarKeyDict = new JSONObject();
        expKeyToVarKeyDict.put("exp1", "var1");
        JSONObject recordDict = new JSONObject();
        recordDict.put("user1", expKeyToVarKeyDict);

        when(diskUserExperimentRecordCache.load()).thenReturn(recordDict);

        Map<String, String> expKeyToVarKeyMap = new HashMap<>();
        expKeyToVarKeyMap.put("exp1", "var1");
        Map<String, Map<String, String>> recordMap = new HashMap<>();
        recordMap.put("user1", expKeyToVarKeyMap);
        androidUserExperimentRecord.start();

        assertEquals(recordMap, memoryUserExperimentRecordCache);
    }
}
