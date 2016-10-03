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

import com.optimizely.ab.config.Variation;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.event.NoopEventHandler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.io.InputStream;
import java.lang.String;
import java.util.Collections;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for public {@link Optimizely} functions
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class OptimizelyBenchmark {

    private Optimizely optimizely;
    private Random random = new Random();

    private String activateGroupExperimentUserId;
    private String activateGroupExperimentAttributesUserId;
    private String trackGroupExperimentUserId;
    private String trackGroupExperimentAttributesUserId;

    @Param({"10", "25", "50"})
    private int numExperiments;

    @Setup
    @SuppressFBWarnings(value="OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE", justification="stream is safely closed")
    public void setup() throws IOException, ConfigParseException {
        Properties properties = new Properties();
        InputStream propertiesStream = getClass().getResourceAsStream("/benchmark.properties");
        properties.load(propertiesStream);
        propertiesStream.close();

        String datafilePathTemplate = properties.getProperty("datafilePathTemplate");
        String datafilePath = String.format(datafilePathTemplate, numExperiments);

        String activateGroupExperimentPropTemplate = properties.getProperty("activateGroupExperimentUserIdPropTemplate");
        activateGroupExperimentUserId = properties.getProperty(
                String.format(activateGroupExperimentPropTemplate, numExperiments));

        String activateGroupExperimentAttributesPropTemplate =
                properties.getProperty("activateGroupExperimentAttributesUserIdPropTemplate");
        activateGroupExperimentAttributesUserId = properties.getProperty(
                String.format(activateGroupExperimentAttributesPropTemplate, numExperiments));

        String trackGroupExperimentPropTemplate = properties.getProperty("trackGroupExperimentUserIdPropTemplate");
        trackGroupExperimentUserId = properties.getProperty(
                String.format(trackGroupExperimentPropTemplate, numExperiments));

        String trackGroupExperimentAttributesPropTemplate =
                properties.getProperty("trackGroupExperimentAttributesUserIdPropTemplate");
        trackGroupExperimentAttributesUserId = properties.getProperty(
                String.format(trackGroupExperimentAttributesPropTemplate, numExperiments));

        optimizely = Optimizely.builder(BenchmarkUtils.getProfilingDatafile(datafilePath),
                                        new NoopEventHandler()).build();
    }

    @Benchmark
    public Variation measureGetVariationWithNoAttributes() {
        return optimizely.getVariation("testExperiment2", "optimizely_user" + random.nextInt());
    }

    @Benchmark
    public Variation measureGetVariationWithAttributes() {
        return optimizely.getVariation("testExperimentWithFirefoxAudience", "optimizely_user" + random.nextInt(),
                                       Collections.singletonMap("browser_type", "firefox"));
    }

    @Benchmark
    public Variation measureGetVariationWithForcedVariation() {
        return optimizely.getVariation("testExperiment2", "variation_user");
    }

    @Benchmark
    public Variation measureGetVariationForGroupExperimentWithNoAttributes() {
        return optimizely.getVariation("mutex_exp2", activateGroupExperimentUserId);
    }

    @Benchmark
    public Variation measureGetVariationForGroupExperimentWithAttributes() {
        return optimizely.getVariation("mutex_exp1", activateGroupExperimentAttributesUserId,
                                       Collections.singletonMap("browser_type", "chrome"));
    }

    @Benchmark
    public Variation measureActivateWithNoAttributes() {
        return optimizely.activate("testExperiment2", "optimizely_user" + random.nextInt());
    }

    @Benchmark
    public Variation measureActivateWithAttributes() {
        return optimizely.activate("testExperimentWithFirefoxAudience", "optimizely_user" + random.nextInt(),
                                   Collections.singletonMap("browser_type", "firefox"));
    }

    @Benchmark
    public Variation measureActivateWithForcedVariation() {
        return optimizely.activate("testExperiment2", "variation_user");
    }

    @Benchmark
    public Variation measureActivateForGroupExperimentWithNoAttributes() {
        return optimizely.activate("mutex_exp2", activateGroupExperimentUserId);
    }

    @Benchmark
    public Variation measureActivateForGroupExperimentWithAttributes() {
        return optimizely.activate("mutex_exp1", activateGroupExperimentAttributesUserId,
                                   Collections.singletonMap("browser_type", "chrome"));
    }

    @Benchmark
    public Variation measureActivateForGroupExperimentWithForcedVariation() {
        return optimizely.activate("mutex_exp2", "user_a");
    }

    @Benchmark
    public void measureTrackWithNoAttributesAndNoRevenue() {
        optimizely.track("testEventWithMultipleExperiments", "optimizely_user" + random.nextInt());
    }

    @Benchmark
    public void measureTrackWithNoAttributesAndRevenue() {
        optimizely.track("testEventWithMultipleExperiments", "optimizely_user" + random.nextInt(), 50000);
    }

    @Benchmark
    public void measureTrackWithAttributesAndNoRevenue() {
        optimizely.track("testEventWithMultipleExperiments", "optimizely_user" + random.nextInt(),
                         Collections.singletonMap("browser_type", "firefox"));
    }

    @Benchmark
    public void measureTrackWithAttributesAndRevenue() {
        optimizely.track("testEventWithMultipleExperiments", "optimizely_user" + random.nextInt(),
                         Collections.singletonMap("browser_type", "firefox"), 50000);
    }

    @Benchmark
    public void measureTrackWithGroupExperimentsNoAttributesNoRevenue() {
        optimizely.track("testEventWithMultipleExperiments", trackGroupExperimentUserId);
    }

    @Benchmark
    public void measureTrackWithGroupExperimentsNoAttributesAndRevenue() {
        optimizely.track("testEventWithMultipleExperiments", trackGroupExperimentUserId, 50000);
    }

    @Benchmark
    public void measureTrackWithGroupExperimentsNoRevenueAndAttributes() {
        optimizely.track("testEventWithMultipleExperiments", trackGroupExperimentAttributesUserId,
                         Collections.singletonMap("browser_type", "chrome"));
    }

    @Benchmark
    public void measureTrackWithGroupExperimentsAndAttributesAndRevenue() {
        optimizely.track("testEventWithMultipleExperiments", trackGroupExperimentAttributesUserId,
                         Collections.singletonMap("browser_type", "chrome"), 50000);
    }

    @Benchmark
    public void measureTrackWithGroupExperimentsAndForcedVariation() {
        optimizely.track("testEventWithMultipleExperiments", "user_a");
    }
}
