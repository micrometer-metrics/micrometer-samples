package com.example.micrometer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@AutoConfigureObservability
@ExtendWith(OutputCaptureExtension.class)
class ReactiveCassandraApplicationTests {

    private static final Pattern TRACE_PATTERN = Pattern
        .compile("^.+ --- \\[(.+)] \\[.+] \\[(\\p{XDigit}+)-(\\p{XDigit}+)] .+ <ACCEPTANCE_TEST>.+$");

    @Container
    @ServiceConnection
    static CassandraContainer<?> cassandra = new CassandraContainer<>("cassandra:3.11.10");

    @Test
    void logsShouldContainTraceId(CapturedOutput output) {
        String traceId = await().atMost(Duration.ofSeconds(5))
            .until(() -> getTraceIdFromLogs(output), Optional::isPresent)
            .orElseThrow();
        assertThat(traceId).hasSize(32);
    }

    private Optional<String> getTraceIdFromLogs(CharSequence output) {
        return output.toString()
            .lines()
            .filter(line -> line.contains("<ACCEPTANCE_TEST>"))
            .map(TRACE_PATTERN::matcher)
            .flatMap(Matcher::results)
            .map(matchResult -> matchResult.group(2))
            .findFirst();
    }

}
