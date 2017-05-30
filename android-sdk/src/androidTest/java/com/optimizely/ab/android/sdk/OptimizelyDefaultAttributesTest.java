package com.optimizely.ab.android.sdk;

import java.util.HashMap;
import java.util.Map;
import android.content.Context;
import android.content.pm.PackageInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.Assert.*;

/**
 * Created by tzurkan on 5/30/17.
 */
public class OptimizelyDefaultAttributesTest {
    private Logger logger;

    @Before
    public void setUp() throws Exception {
        logger = mock(Logger.class);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void buildDefaultAttributesMap() throws Exception {
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);
        when(appContext.getPackageName()).thenReturn("com.optly");

        Map<String, String> defaultAttributes = OptimizelyDefaultAttributes.buildDefaultAttributesMap(context, logger);

        assertEquals(defaultAttributes.size(), 4);
    }

}