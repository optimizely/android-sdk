Optimizely Java SDK
===================
[![Build Status](https://travis-ci.org/optimizely/java-sdk.svg?branch=master)](https://travis-ci.org/optimizely/java-sdk)
[![Apache 2.0](https://img.shields.io/badge/license-APACHE%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

This repository houses the Java SDK for Optimizely's Full Stack product.

## Getting Started

### Installing the SDK

#### Gradle

The SDK is available through Bintray. The core-api and httpclient Bintray packages are [optimizely-sdk-core-api](https://bintray.com/optimizely/optimizely/optimizely-sdk-core-api)
and [optimizely-sdk-httpclient](https://bintray.com/optimizely/optimizely/optimizely-sdk-httpclient) respectively. To install, place the
following in your `build.gradle` and substitute `VERSION` for the latest SDK version available via Bintray:

```
repositories {
  jcenter()
}

dependencies {
  compile 'com.optimizely.ab:core-api:{VERSION}'
  compile 'com.optimizely.ab:core-httpclient-impl:{VERSION}'
  // The SDK integrates with multiple JSON parsers, here we use
  // Jackson.
  compile 'com.fasterxml.jackson.core:jackson-core:2.7.1'
  compile 'com.fasterxml.jackson.core:jackson-annotations:2.7.1'
  compile 'com.fasterxml.jackson.core:jackson-databind:2.7.1'
}
```  

#### Dependencies

`core-api` requires [org.slf4j:slf4j-api:1.7.16](https://mvnrepository.com/artifact/org.slf4j/slf4j-api/1.7.16) and a supported JSON parser. 
We currently integrate with [Jackson](https://github.com/FasterXML/jackson), [GSON](https://github.com/google/gson), [json.org](http://www.json.org),
and [json-simple](https://code.google.com/archive/p/json-simple); if any of those packages are available at runtime, they will be used by `core-api`.
If none of those packages are already provided in your project's classpath, one will need to be added. `core-httpclient-impl` is an optional 
dependency that implements the event dispatcher and requires [org.apache.httpcomponents:httpclient:4.5.2](https://mvnrepository.com/artifact/org.apache.httpcomponents/httpclient/4.5.2).
The supplied `pom` files on Bintray define module dependencies.

### Using the SDK

See the Optimizely Full Stack [developer documentation](http://developers.optimizely.com/server/reference/index.html) to learn how to set
up your first Java project and use the SDK.

## Development

### Building the SDK

To build local jars which are outputted into the respective modules' `build/lib` directories:

```
./gradlew build
```

### Unit tests

#### Running all tests

You can run all unit tests with:

```
./gradlew test
```

### Checking for bugs

We utilize [FindBugs](http://findbugs.sourceforge.net/) to identify possible bugs in the SDK. To run the check:

```
./gradlew check
```

### Benchmarking

[JMH](http://openjdk.java.net/projects/code-tools/jmh/) benchmarks can be run through gradle:

```
./gradlew core-api:jmh
```

Results are generated in `$buildDir/reports/jmh`.

### Contributing

Please see [CONTRIBUTING](CONTRIBUTING.md).

## License
```
   Copyright 2016, Optimizely

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

[JAR hell]: https://en.wikipedia.org/wiki/Java_Classloader#JAR_hell
[developer_docs]: http://developers.optimizely.com/server/index.html
[project_json]: http://developers.optimizely.com/server/reference/index.html#json
[Optimizely]: core-api/src/main/java/com/optimizely/ab/Optimizely.java
[Project Watcher]: core-api/src/main/java/com/optimizely/ab/config/ProjectWatcher.java
[Event Handler]: core-api/src/main/java/com/optimizely/ab/event/EventHandler.java
