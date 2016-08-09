package com.optimizely.persistent_bucketer;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.Variation;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jdeffibaugh on 8/8/16 for Optimizely.
 *
 * Tests for {@link AndroidPersistentBucketer}
 */
public class AndroidPersistentBucketerTest {

    AndroidPersistentBucketer androidPersistentBucketer;
    PersistentBucketerCache persistentBucketerCache;
    Logger logger;

    @Before
    public void setup() {
        persistentBucketerCache = mock(PersistentBucketerCache.class);
        logger = mock(Logger.class);
        androidPersistentBucketer = new AndroidPersistentBucketer(persistentBucketerCache, logger);
    }

    @Test
    public void saveActivation() {
        ProjectConfig projectConfig = mock(ProjectConfig.class);
        String userId = "foo";
        Experiment experiment = mock(Experiment.class);
        String expId = "exp1";
        when(experiment.getId()).thenReturn(expId);
        Variation variation = mock(Variation.class);
        String varId = "var1";
        when(variation.getId()).thenReturn(varId);
        androidPersistentBucketer.saveActivation(projectConfig, userId, experiment, variation);
        verify(persistentBucketerCache).save(userId, expId, varId);
    }

    @Test
    public void saveActivationNullProjectConfig() {
        androidPersistentBucketer.saveActivation(null, "foo", mock(Experiment.class), mock(Variation.class));
        verify(logger).error("Received null projectConfig, unable to save activation");
    }

    @Test
    public void saveActivationNullUserId() {
        androidPersistentBucketer.saveActivation(mock(ProjectConfig.class), null, mock(Experiment.class), mock(Variation.class));
        verify(logger).error("Received null userId, unable to save activation");
    }

    @Test
    public void saveActivationNullExperiment() {
        androidPersistentBucketer.saveActivation(mock(ProjectConfig.class), "foo", null, mock(Variation.class));
        verify(logger).error("Received null experiment, unable to save activation");
    }

    @Test
    public void saveActivationNullVariation() {
        androidPersistentBucketer.saveActivation(mock(ProjectConfig.class), "foo", mock(Experiment.class), null);
        verify(logger).error("Received null variation, unable to save activation");
    }

    @Test
    public void restoreActivationNullProjectConfig() {
        androidPersistentBucketer.restoreActivation(null, "foo", mock(Experiment.class));
        verify(logger).error("Received null projectConfig, unable to restore activation");
    }

    @Test
    public void restoreActivationNullUserId() {
        androidPersistentBucketer.restoreActivation(mock(ProjectConfig.class), null, mock(Experiment.class));
        verify(logger).error("Received null userId, unable to restore activation");
    }

    @Test
    public void restoreActivationNullExperiment() {
        androidPersistentBucketer.restoreActivation(mock(ProjectConfig.class), "foo", null);
        verify(logger).error("Received null experiment, unable to restore activation");
    }

    @Test
    public void restoreActivation() throws JSONException {
        ProjectConfig projectConfig = mock(ProjectConfig.class);
        Experiment experiment = mock(Experiment.class);
        String expId = "exp1";
        when(experiment.getId()).thenReturn(expId);
        Variation variation = mock(Variation.class);
        String varId = "var1";
        when(variation.getId()).thenReturn(varId);
        JSONObject activation = new JSONObject();
        JSONObject expIdToVarIdDict = new JSONObject();
        expIdToVarIdDict.put("exp1", "var1");
        activation.put("foo", expIdToVarIdDict);
        Map<String,Experiment> experimentIdMapping = new HashMap<>();
        experimentIdMapping.put("exp1", experiment);
        when(projectConfig.getExperimentIdMapping()).thenReturn(experimentIdMapping);
        Map<String,Variation> variationIdToVariationMap = new HashMap<>();
        variationIdToVariationMap.put("var1", variation);
        when(experiment.getVariationIdToVariationMap()).thenReturn(variationIdToVariationMap);
        when(persistentBucketerCache.load()).thenReturn(activation);

        assertEquals(androidPersistentBucketer.restoreActivation(projectConfig, "foo", experiment), variation);
    }

    @Test
    public void restoreActivationNoExp() throws JSONException {
        ProjectConfig projectConfig = mock(ProjectConfig.class);
        Experiment experiment = mock(Experiment.class);
        String expId = "exp1";
        when(experiment.getId()).thenReturn(expId);
        Variation variation = mock(Variation.class);
        String varId = "var1";
        when(variation.getId()).thenReturn(varId);
        JSONObject activation = new JSONObject();
        JSONObject expIdToVarIdDict = new JSONObject();
        expIdToVarIdDict.put("exp1", "var1");
        activation.put("foo", expIdToVarIdDict);
        Map<String,Experiment> experimentIdMapping = new HashMap<>();
        when(projectConfig.getExperimentIdMapping()).thenReturn(experimentIdMapping);
        when(persistentBucketerCache.load()).thenReturn(activation);

        assertNull(androidPersistentBucketer.restoreActivation(projectConfig, "foo", experiment));
        verify(logger).error("Project config did not contain matching experiment and variation ids");
    }

    @Test
    public void restoreActivationNoVar() throws JSONException {
        ProjectConfig projectConfig = mock(ProjectConfig.class);
        Experiment experiment = mock(Experiment.class);
        String expId = "exp1";
        when(experiment.getId()).thenReturn(expId);
        Variation variation = mock(Variation.class);
        String varId = "var1";
        when(variation.getId()).thenReturn(varId);
        JSONObject activation = new JSONObject();
        JSONObject expIdToVarIdDict = new JSONObject();
        expIdToVarIdDict.put("exp1", "var1");
        activation.put("foo", expIdToVarIdDict);
        Map<String,Experiment> experimentIdMapping = new HashMap<>();
        experimentIdMapping.put("exp1", experiment);
        when(projectConfig.getExperimentIdMapping()).thenReturn(experimentIdMapping);
        Map<String,Variation> variationIdToVariationMap = new HashMap<>();
        when(experiment.getVariationIdToVariationMap()).thenReturn(variationIdToVariationMap);
        when(persistentBucketerCache.load()).thenReturn(activation);

        assertNull(androidPersistentBucketer.restoreActivation(projectConfig, "foo", experiment));
        verify(logger).error("Project config did not contain matching experiment and variation ids");
    }

    @Test
    public void restoreActivationJSONException() throws JSONException {
        ProjectConfig projectConfig = mock(ProjectConfig.class);
        Experiment experiment = mock(Experiment.class);
        String expId = "exp1";
        when(experiment.getId()).thenReturn(expId);
        Variation variation = mock(Variation.class);
        String varId = "var1";
        when(variation.getId()).thenReturn(varId);
        JSONObject activation = new JSONObject();
        when(persistentBucketerCache.load()).thenReturn(activation);

        assertNull(androidPersistentBucketer.restoreActivation(projectConfig, "foo", experiment));
        verify(logger).error(contains("Unable to parse persistent data file cache"), any(JSONException.class));
    }

}
