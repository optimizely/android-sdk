package com.optimizely.ab.android.shared;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;


/**
 * Created by jdeffibaugh on 8/1/16 for Optimizely.
 *
 * Tests for {@link OptlyStorage}
 */
@RunWith(AndroidJUnit4.class)
public class OptlyStorageTest {

    OptlyStorage optlyStorage;

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getTargetContext();
        optlyStorage = new OptlyStorage(context);
    }

    @Test
    public void saveAndGetLong() {
        optlyStorage.saveLong("foo", 1);
        assertEquals(1L, optlyStorage.getLong("foo", 0L));
    }
}

