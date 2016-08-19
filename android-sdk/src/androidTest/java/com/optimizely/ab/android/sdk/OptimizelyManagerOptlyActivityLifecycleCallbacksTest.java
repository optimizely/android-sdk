package com.optimizely.ab.android.sdk;

import android.app.Activity;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by jdeffibaugh on 8/16/16 for Optimizely.
 *
 * Tests for {@link OptimizelyManager.OptlyActivityLifecycleCallbacks}
 */
@RunWith(AndroidJUnit4.class)
public class OptimizelyManagerOptlyActivityLifecycleCallbacksTest {

    OptimizelyManager.OptlyActivityLifecycleCallbacks optlyActivityLifecycleCallbacks;
    OptimizelyManager optimizelyManager;

    @Before
    public void setup() {
        optimizelyManager = mock(OptimizelyManager.class);
        optlyActivityLifecycleCallbacks = new OptimizelyManager.OptlyActivityLifecycleCallbacks(optimizelyManager);
    }

    @Test
    public void onActivityStopped() {
        Activity activity = mock(Activity.class);
        optlyActivityLifecycleCallbacks.onActivityStopped(activity);
        verify(optimizelyManager).stop(activity, optlyActivityLifecycleCallbacks);
    }
}
