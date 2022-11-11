/*
 * Copyright 2022 VMware, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.boot3.samples.web;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.prometheus.client.exporter.common.TextFormat.CONTENT_TYPE_OPENMETRICS_100;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Testcontainers
@ActiveProfiles("test")
@AutoConfigureObservability
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class Boot3WithWebSampleApplicationTests {

    private static final Pattern TRACE_PATTERN = Pattern
            .compile("^.+INFO \\[(.+),(\\p{XDigit}+),(\\p{XDigit}+)\\] .+ <TEST_MARKER>.+$");

    @Container
    static GenericContainer<?> zipkin = new GenericContainer(DockerImageName.parse("openzipkin/zipkin:latest"))
            .withExposedPorts(9411);

    @Autowired
    ObservationRegistry observationRegistry;

    @LocalServerPort
    int port;

    @DynamicPropertySource
    static void zipkinProperties(DynamicPropertyRegistry registry) {
        registry.add("management.zipkin.tracing.endpoint",
                () -> String.format("http://localhost:%d/api/v2/spans", zipkin.getFirstMappedPort()));
    }

    @Test
    void verifyLogsMetricsTraces(CapturedOutput output) {
        // @formatter:off
        await().atMost(Duration.ofSeconds(5)).untilAsserted(this::verifyIfZipkinIsUp);
        verifyIfGreetingApiWorks();
        TraceInfo traceInfo = await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> getTraceInfoFromLogs(output), Optional::isPresent)
                .orElseThrow();
        verifyIfPrometheusEndpointWorks(traceInfo);
        await()
                .atMost(Duration.ofSeconds(10))
                .pollDelay(Duration.ofMillis(100))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> verifyIfTraceIsInZipkin(traceInfo));
        // @formatter:on
    }

    private void verifyIfGreetingApiWorks() {
        // @formatter:off
        given()
                .port(port)
                .accept(JSON)
        .when()
                .get("/greet/suzy")
        .then()
                .statusCode(200)
                .body("greeted", equalTo("suzy"));
        // @formatter:on
    }

    private Optional<TraceInfo> getTraceInfoFromLogs(CharSequence output) {
        // @formatter:off
        return output.toString().lines()
                .filter(line -> line.contains("<TEST_MARKER>"))
                .map(TRACE_PATTERN::matcher)
                .flatMap(Matcher::results)
                .map(matchResult -> new TraceInfo(matchResult.group(2), matchResult.group(3)))
                .findFirst();
        // @formatter:on
    }

    private void verifyIfPrometheusEndpointWorks(TraceInfo traceInfo) {
        // @formatter:off
        given()
                .port(port)
                .accept(CONTENT_TYPE_OPENMETRICS_100)
        .when()
                .get("/actuator/prometheus")
        .then()
                .statusCode(200)
                .body(
                        containsString("greeting_greeted_total"), // counter
                        containsString("greeting_seconds_count"), // summary
                        containsString("greeting_seconds_bucket"), // histogram
                        containsString("greeting_active_seconds_active_count"), // active summary
                        containsString("greeting_active_seconds_bucket"), // active histogram
                        containsString(String.format("{span_id=\"%s\",trace_id=\"%s\"}", traceInfo.spanId, traceInfo.traceId)) // exemplar
                );
        // @formatter:on
    }

    private void verifyIfZipkinIsUp() {
        // @formatter:off
        given()
                .port(zipkin.getFirstMappedPort())
        .when()
                .get("/zipkin")
        .then()
                .statusCode(200);
        // @formatter:on
    }

    private void verifyIfTraceIsInZipkin(TraceInfo traceInfo) {
        // @formatter:off
        given()
                .port(zipkin.getFirstMappedPort())
                .accept(JSON)
        .when()
                .get("/zipkin/api/v2/trace/" + traceInfo.traceId)
        .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", equalTo(2))
                .body("findAll { it.name == 'greeting' }.size()", equalTo(1))
                .body("findAll { it.name == 'http get /greet/{name}' }.size()", equalTo(1))
                .rootPath("find { it.name == 'greeting' }")
                    .body("traceId", equalTo(traceInfo.traceId))
                    .body("id", equalTo(traceInfo.spanId))
                    .body("parentId", not(nullValue()))
                    .body("localEndpoint.serviceName", equalTo("boot3-web-sample"))
                    .body("annotations[0].value", equalTo("greeted"))
                    .body("tags['greeting.name']", equalTo("suzy"))
                .detachRootPath("")
                .rootPath("find { it.name == 'http get /greet/{name}' }")
                    .body("traceId", equalTo(traceInfo.traceId))
                    .body("id", not(nullValue()))
                    .body("parentId", is(nullValue()))
                    .body("kind", equalTo("SERVER"))
                    .body("localEndpoint.serviceName", equalTo("boot3-web-sample"))
                    .body("tags['method']", equalTo("GET"))
                    .body("tags['http.url']", equalTo("/greet/suzy"))
                    .body("tags['outcome']", equalTo("SUCCESS"))
                    .body("tags['status']", equalTo("200"))
                    .body("tags['uri']", equalTo("/greet/{name}"));
        // @formatter:on
    }

    private record TraceInfo(String traceId, String spanId) {
    }

}
