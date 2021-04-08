/****************************************************************************
 * Copyright 2019, Optimizely, Inc. and contributors                        *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                            *
 * *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.android.sdk

import androidx.test.InstrumentationRegistry
import com.optimizely.ab.config.*
import com.optimizely.ab.optimizelyconfig.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*

class OptimizelyConfigServiceTest {
    private var optimizelyConfig: OptimizelyConfig? = null
    private var projectConfig: ProjectConfig? = null
    @Before
    @Throws(Exception::class)
    fun initialize() {
        projectConfig = DatafileProjectConfig.Builder().withDatafile(OptimizelyManager.loadRawResource(InstrumentationRegistry.getTargetContext(), R.raw.validprojectconfigv4))
                .build()
        optimizelyConfig = OptimizelyConfigService(projectConfig).getConfig()
    }

    @Test
    @Throws(Exception::class)
    fun shouldReturnAllExperimentsExceptRollouts() {
        val optimizelyExperimentMap: Map<String, OptimizelyExperiment> = optimizelyConfig?.getExperimentsMap() as Map<String, OptimizelyExperiment>
        Assert.assertEquals(optimizelyExperimentMap.size.toLong(), 11)
        val experiments: List<Experiment> = allExperimentsFromDatafile as List<Experiment>
        for (experiment in experiments) {
            val optimizelyExperiment: OptimizelyExperiment? = optimizelyExperimentMap[experiment.getKey()]
            Assert.assertEquals(optimizelyExperiment?.getId(), experiment.getId())
            Assert.assertEquals(optimizelyExperiment?.getKey(), experiment.getKey())
            val optimizelyVariationMap: MutableMap<String, OptimizelyVariation>? = optimizelyExperimentMap[experiment.getKey()]?.getVariationsMap()
            for (variation in experiment.getVariations()) {
                val optimizelyVariation: OptimizelyVariation? = optimizelyVariationMap?.get(variation.getKey())
                Assert.assertEquals(optimizelyVariation?.getId(), variation.getId())
                Assert.assertEquals(optimizelyVariation?.getKey(), variation.getKey())
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldReturnAllFeatureFlag() {
        val optimizelyFeatureMap: MutableMap<String, OptimizelyFeature>? = optimizelyConfig?.getFeaturesMap()
        Assert.assertEquals(optimizelyFeatureMap?.size, 7)
        for (featureFlag in projectConfig?.getFeatureFlags()!!) {
            val optimizelyFeature: OptimizelyFeature? = optimizelyFeatureMap?.get(featureFlag.getKey())
            Assert.assertEquals(optimizelyFeature?.getId(), featureFlag.getId())
            Assert.assertEquals(optimizelyFeature?.getKey(), featureFlag.getKey())
            for (experimentId in featureFlag.getExperimentIds()) {
                val experimentKey: String? = projectConfig!!.getExperimentIdMapping().get(experimentId)?.getKey()
                Assert.assertNotNull(optimizelyFeatureMap?.get(featureFlag.getKey())?.getExperimentsMap()?.get(experimentKey))
            }
            val optimizelyVariableMap: MutableMap<String, OptimizelyVariable>? = optimizelyFeatureMap?.get(featureFlag.getKey())?.getVariablesMap()
            for (variable in featureFlag.getVariables()) {
                val optimizelyVariable: OptimizelyVariable? = optimizelyVariableMap!![variable.getKey()]
                Assert.assertEquals(optimizelyVariable?.getId(), variable.getId())
                Assert.assertEquals(optimizelyVariable?.getKey(), variable.getKey())
                Assert.assertEquals(optimizelyVariable?.getType(), variable.getType())
                Assert.assertEquals(optimizelyVariable?.getValue(), variable.getDefaultValue())
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldCorrectlyMergeAllFeatureVariables() {
        val featureFlags: List<FeatureFlag> = projectConfig!!.getFeatureFlags()
        val datafileExperimentsMap: MutableMap<String, Experiment> = HashMap<String, Experiment>()
        for (experiment in allExperimentsFromDatafile) {
            datafileExperimentsMap[experiment.getKey()] = experiment
        }
        for (featureFlag in featureFlags) {
            val experimentIds: List<String> = featureFlag.getExperimentIds()
            for (experimentId in experimentIds) {
                val experimentKey: String = projectConfig!!.getExperimentIdMapping().get(experimentId)!!.getKey()
                val experiment: OptimizelyExperiment? = optimizelyConfig?.getExperimentsMap()?.get(experimentKey)
                val variations: List<Variation> = datafileExperimentsMap[experimentKey]!!.getVariations()
                val variationsMap: Map<String, OptimizelyVariation> = experiment!!.getVariationsMap()
                for (variation in variations) {
                    for (variable in featureFlag.getVariables()) {
                        val optimizelyVariable: OptimizelyVariable? = variationsMap[variation.getKey()]!!.getVariablesMap().get(variable.getKey())
                        if (optimizelyVariable != null) {
                            Assert.assertEquals(variable.getId(), optimizelyVariable.getId())
                            Assert.assertEquals(variable.getKey(), optimizelyVariable.getKey())
                            Assert.assertEquals(variable.getType(), optimizelyVariable.getType())
                            if (!variation.getFeatureEnabled()) {
                                Assert.assertEquals(variable.getDefaultValue(), optimizelyVariable.getValue())
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldReturnCorrectRevision() {
        val revision: String = optimizelyConfig!!.getRevision()
        Assert.assertEquals(revision, projectConfig!!.getRevision())
    }

    private val allExperimentsFromDatafile: List<Experiment>
        private get() {
            val experiments: MutableList<Experiment> = ArrayList<Experiment>()
            for (group in projectConfig?.getGroups()!!) {
                experiments.addAll(group.experiments)
            }
            experiments.addAll(projectConfig!!.getExperiments())
            return experiments
        }
}