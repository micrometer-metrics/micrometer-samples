package com.example.micrometer;

import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.BDDAssertions.then;

@Component
class TracingAssertions {

    private static final Logger log = LoggerFactory.getLogger(TracingAssertions.class);

    private static final Pattern tracePattern = Pattern.compile("^.*<ACCEPTANCE_TEST> <TRACE:(.*)> .*$");

    private static final String expectedConsumerText = "Hello from consumer";

    private static final String expectedProducerText = "Hello from producer";

    private final ProjectDeployer projectDeployer;

    TracingAssertions(ProjectDeployer projectDeployer) {
        this.projectDeployer = projectDeployer;
    }

    void assertThatTraceIdGotPropagated(String... appIds) {
        try {
            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
                AtomicBoolean consumerPresent = new AtomicBoolean();
                AtomicBoolean producerPresent = new AtomicBoolean();
                List<String> traceIds = Arrays.stream(appIds)
                    .map(this.projectDeployer::getLog)
                    .flatMap(s -> Arrays.stream(s.split(System.lineSeparator())))
                    .filter(s -> s.contains("ACCEPTANCE_TEST"))
                    .map(s -> {
                        Matcher matcher = tracePattern.matcher(s);
                        if (matcher.matches()) {
                            if (s.contains(expectedConsumerText)) {
                                consumerPresent.set(true);
                            }
                            else if (s.contains(expectedProducerText)) {
                                producerPresent.set(true);
                            }
                            return matcher.group(1);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
                log.info("Found the following trace id {}", traceIds);
                then(traceIds).as("TraceId should have only one value").hasSize(1);
                log.info("Checking if producer code was called");
                then(producerPresent).as("Producer code must be called").isTrue();
                log.info("Producer code got called! Checking if consumer code was called");
                then(consumerPresent).as("Consumer code must be called").isTrue();
                log.info("Consumer code got called!");
            });
        }
        catch (Throwable er) {
            log.error("Something went wrong! Will print out the application logs\n\n");
            Arrays.stream(appIds)
                .forEach(id -> log.error("App with id [" + id + "]\n\n" + this.projectDeployer.getLog(id)));
            throw er;
        }
    }

    void assertThatLogsContainPropagatedIdAtLeastXNumberOfTimes(String appId, String springApplicationName,
            int minNumberOfOccurrences) {
        Pattern pattern = Pattern
            .compile("^.+ --- \\[" + springApplicationName + "] \\[.+] \\[(\\p{XDigit}+)-(\\p{XDigit}+)] .+$");
        try {
            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
                AtomicInteger counter = new AtomicInteger();
                List<String> traceIds = Arrays.stream(this.projectDeployer.getLog(appId).split(System.lineSeparator()))
                    .map(s -> {
                        Matcher matcher = pattern.matcher(s);
                        if (matcher.matches()) {
                            counter.incrementAndGet();
                            return matcher.group(1);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
                log.info("Found the trace id {} [{}] times. Min required number [{}] ", traceIds, counter.get(),
                        minNumberOfOccurrences);
                then(traceIds).as("TraceId should have only one value").hasSize(1);
                then(counter).as("There should be at least X number of times")
                    .hasValueGreaterThanOrEqualTo(minNumberOfOccurrences);
            });
        }
        catch (Throwable er) {
            log.error("One of the assertions has failed! Will print out the application logs");
            log.error(this.projectDeployer.getLog(appId));
            throw er;
        }
    }

}
