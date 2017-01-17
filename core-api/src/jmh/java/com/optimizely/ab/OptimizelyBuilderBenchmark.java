/**
 *
 *    Copyright 2016, Optimizely and contributors
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

import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.event.EventHandler;
import com.optimizely.ab.event.NoopEventHandler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.Properties;

/**
 * JMH benchmark for {@link Optimizely.Builder}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class OptimizelyBuilderBenchmark {

    public EventHandler eventHandler = new NoopEventHandler();

    @Param({"10", "25", "50"})
    private int numExperiments;

    private String datafile;

    @Setup
    @SuppressFBWarnings(value="OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE", justification="stream is safely closed")
    public void setup() throws IOException {
        Properties properties = new Properties();
        InputStream propertiesStream = getClass().getResourceAsStream("/benchmark.properties");
        properties.load(propertiesStream);
        propertiesStream.close();

        String datafilePathTemplate = properties.getProperty("datafilePathTemplate");
        String datafilePath = String.format(datafilePathTemplate, numExperiments);
        datafile = BenchmarkUtils.getProfilingDatafile(datafilePath);
    }

    @Benchmark
    public Optimizely measureOptimizelyCreation() throws IOException, ConfigParseException {
        return Optimizely.builder(datafile, eventHandler).build();
    }
}
