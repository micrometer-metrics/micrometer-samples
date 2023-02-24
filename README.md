# Micrometer Samples

[![Build Status](https://circleci.com/gh/micrometer-metrics/micrometer-samples.svg?style=shield)](https://circleci.com/gh/micrometer-metrics/micrometer-samples)
[![Apache 2.0](https://img.shields.io/github/license/micrometer-metrics/micrometer.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.micrometer/micrometer-core.svg)](https://search.maven.org/artifact/io.micrometer/micrometer-core)
[![Javadocs](https://www.javadoc.io/badge/io.micrometer/micrometer-core.svg)](https://www.javadoc.io/doc/io.micrometer/micrometer-core)
[![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.micrometer.io/)

Sample apps to demo [Micrometer](https://github.com/micrometer-metrics/micrometer) features.

## Building

To build the project with Brave tracer just run 

```bash
$ ./gradlew build
```

To build the project with OpenTelemetry tracer just run 

```bash
$ ./gradlew build -Potel
```

## Running

You can run each of the samples individually.

## Join the discussion

Join the [Micrometer Slack](https://slack.micrometer.io) to share your questions, concerns, and feature requests.

## Documentation

The reference documentation is managed in [a separate GitHub repository](https://github.com/micrometer-metrics/micrometer-docs) and published to https://micrometer.io/.

## Contributing

See our [Contributing Guide](CONTRIBUTING.md) for information about contributing to Micrometer.

-------------------------------------
_Licensed under [Apache Software License 2.0](https://www.apache.org/licenses/LICENSE-2.0)_

_Sponsored by [VMware](https://tanzu.vmware.com)_
