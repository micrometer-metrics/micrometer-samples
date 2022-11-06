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
package io.micrometer.boot3.samples.db;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.prometheus.client.exporter.common.TextFormat.CONTENT_TYPE_OPENMETRICS_100;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.openqa.selenium.OutputType.BYTES;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Testcontainers
@ActiveProfiles("test")
@AutoConfigureObservability
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class Boot3WithDatabaseSampleApplicationTests {

    private static final Pattern TRACE_PATTERN = Pattern
            .compile("^.+INFO \\[(.+),(\\p{XDigit}+),(\\p{XDigit}+)\\] .+ <ACCEPTANCE_TEST>.+$");

    @Container
    static GenericContainer<?> zipkin = new GenericContainer(DockerImageName.parse("openzipkin/zipkin:latest"))
            .withExposedPorts(9411);

    @Container
    static BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>().withCapabilities(new ChromeOptions())
            .withExtraHost("host.docker.internal", "host-gateway");

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
        verifyIfGreetingApiWorks();
        TraceInfo traceInfo = await()
                .atMost(Duration.ofMillis(200))
                .pollDelay(Duration.ofMillis(10))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> getTraceInfoFromLogs(output), Optional::isPresent)
                .orElseThrow();

        verifyIfPrometheusEndpointWorks(traceInfo);
        await()
                .atMost(Duration.ofSeconds(5))
                .pollDelay(Duration.ofMillis(100))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> verifyIfTraceIsInZipkin(traceInfo));
        // @formatter:on
        takeZipkinScreenShot(traceInfo.traceId);
    }

    private void takeZipkinScreenShot(String traceId) {
        RemoteWebDriver webDriver = chrome.getWebDriver();
        webDriver.manage().window().setSize(new Dimension(1920, 1080));
        String url = "http://host.docker.internal:%d/zipkin/traces/%s".formatted(zipkin.getFirstMappedPort(), traceId);
        webDriver.get(url);

        try {
            Files.write(Path.of("build/screenshot.png"), webDriver.getScreenshotAs(BYTES), CREATE, WRITE);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
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
                .filter(line -> line.contains("<ACCEPTANCE_TEST>"))
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
                .body("size()", equalTo(5))
                .body("findAll { it.name == 'greeting' }.size()", equalTo(1))
                .body("findAll { it.name == 'query' }.size()", equalTo(1))
                .body("findAll { it.name == 'http get /greet/{name}' }.size()", equalTo(1))
                .rootPath("find { it.name == 'greeting' }")
                    .body("traceId", equalTo(traceInfo.traceId))
                    .body("id", equalTo(traceInfo.spanId))
                    .body("parentId", not(nullValue()))
                    .body("localEndpoint.serviceName", equalTo("boot3-db-sample"))
                    .body("annotations[0].value", equalTo("greeted"))
                    .body("tags['greeting.name']", equalTo("suzy"))
                .detachRootPath("")
                .rootPath("find { it.name == 'query' }")
                    .body("traceId", equalTo(traceInfo.traceId))
                    .body("id", not(equalTo(traceInfo.spanId)))
                    .body("parentId", not(nullValue()))
                    .body("kind", equalTo("CLIENT"))
                // TODO: OTel does not add remoteEndpoint, it has a tag instead
//                    .body("remoteEndpoint.serviceName", equalTo("testdb"))
//                    .body("tags['peer.service']", equalTo("TESTDB"))
                    .body("tags['jdbc.query[0]']", equalTo("SELECT count(name) FROM emp where name=?"))
                .detachRootPath("")
                .rootPath("find { it.name == 'http get /greet/{name}' }")
                    .body("traceId", equalTo(traceInfo.traceId))
                    .body("id", not(nullValue()))
                    .body("parentId", is(nullValue()))
                    .body("kind", equalTo("SERVER"))
                    .body("localEndpoint.serviceName", equalTo("boot3-db-sample"))
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
