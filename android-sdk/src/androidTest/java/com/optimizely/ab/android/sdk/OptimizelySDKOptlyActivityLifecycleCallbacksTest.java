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
 * Tests for {@link OptimizelySDK.OptlyActivityLifecycleCallbacks}
 */
@RunWith(AndroidJUnit4.class)
public class OptimizelySDKOptlyActivityLifecycleCallbacksTest {

    OptimizelySDK.OptlyActivityLifecycleCallbacks optlyActivityLifecycleCallbacks;
    OptimizelySDK optimizelySDK;

    @Before
    public void setup() {
        optimizelySDK = mock(OptimizelySDK.class);
        optlyActivityLifecycleCallbacks = new OptimizelySDK.OptlyActivityLifecycleCallbacks(optimizelySDK);
    }

    @Test
    public void onActivityStopped() {
        Activity activity = mock(Activity.class);
        optlyActivityLifecycleCallbacks.onActivityStopped(activity);
        verify(optimizelySDK).stop(activity, optlyActivityLifecycleCallbacks);
    }
}
