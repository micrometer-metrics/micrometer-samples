package com.example.micrometer;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
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
class RetrofitApplicationTests {
    private static final Pattern TRACE_PATTERN = Pattern
        .compile("^.+INFO \\[(.+),(\\p{XDigit}+),(\\p{XDigit}+)\\] .+ <TEST_MARKER>.+$");

    private static final WireMockServer WIREMOCK = new WireMockServer(wireMockConfig().port(7100));

    @Container
    static GenericContainer<?> zipkin = new GenericContainer<>(DockerImageName.parse("openzipkin/zipkin:latest"))
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

    @BeforeAll
    static void setup() {
        WIREMOCK.start();
        WIREMOCK.stubFor(
            WireMock.get("/greeting/Suzy").willReturn(
                WireMock.aResponse()
                    .withBody("Hi Suzy!")
                    .withStatus(200)
            )
        );
    }

    @AfterAll
    static void close() {
        WIREMOCK.stop();
    }

    @Test
    void verifyLogsMetricsTraces(CapturedOutput output) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(this::verifyIfZipkinIsUp);
        verifyIfGreetApiWorks();
        TraceInfo traceInfo = await().atMost(Duration.ofSeconds(5))
            .until(() -> getTraceInfoFromLogs(output), Optional::isPresent)
            .orElseThrow();
        verifyIfPrometheusEndpointWorks(traceInfo);
        await().atMost(Duration.ofSeconds(10))
            .pollDelay(Duration.ofMillis(100))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> verifyIfTraceIsInZipkin(traceInfo));
    }

    private void verifyIfZipkinIsUp() {
        given().port(zipkin.getFirstMappedPort()).when().get("/zipkin").then().statusCode(200);
    }

    private void verifyIfGreetApiWorks() {
        given().port(port)
            .accept(JSON)
        .when()
            .get("/greet/Suzy")
        .then()
            .statusCode(200)
            .body("greeting", equalTo("Hi Suzy!"));
    }

    private Optional<TraceInfo> getTraceInfoFromLogs(CharSequence output) {
        return output.toString()
            .lines()
            .filter(line -> line.contains("<TEST_MARKER>"))
            .map(TRACE_PATTERN::matcher)
            .flatMap(Matcher::results)
            .map(matchResult -> new TraceInfo(matchResult.group(2), matchResult.group(3)))
            .findFirst();
    }

    private record TraceInfo(String traceId, String spanId) {
    }

    private void verifyIfPrometheusEndpointWorks(TraceInfo traceInfo) {
        given().port(port)
            .accept(CONTENT_TYPE_OPENMETRICS_100)
        .when()
            .get("/actuator/prometheus")
        .then()
            .statusCode(200)
            .body(
                // HTTP Server Observation
                // Timer
                containsString("http_server_requests_seconds_count"),
                containsString("http_server_requests_seconds_sum"),
                containsString("http_server_requests_seconds_max"),
                containsString("http_server_requests_seconds_bucket"),
                // LongTaskTimer
                containsString("http_server_requests_active_seconds_active_count"),
                containsString("http_server_requests_active_seconds_duration_sum"),
                containsString("http_server_requests_active_seconds_max"),
                containsString("http_server_requests_active_seconds_bucket"),
                // Exemplar
                matchesRegex(
                    "[\\s\\S]*http_server_requests_seconds_bucket\\{.*}.* 1.0 # \\{span_id=\".*\",trace_id=\"%s\"} [\\s\\S]*"
                        .formatted(traceInfo.traceId)),

                // HTTP Client Observation
                // Timer
                containsString("http_client_requests_seconds_count"),
                containsString("http_client_requests_seconds_sum"),
                containsString("http_client_requests_seconds_max"),
                containsString("http_client_requests_seconds_bucket"),
                // LongTaskTimer
                containsString("http_client_requests_active_seconds_duration_sum"),
                containsString("http_client_requests_active_seconds_active_count"),
                containsString("http_client_requests_active_seconds_max"),
                containsString("http_client_requests_active_seconds_bucket"),
                // Exemplar
                matchesRegex(
                    "[\\s\\S]*http_client_requests_seconds_bucket\\{.*}.* 1.0 # \\{span_id=\".*\",trace_id=\"%s\"} [\\s\\S]*"
                        .formatted(traceInfo.traceId))
            );
    }

    private void verifyIfTraceIsInZipkin(TraceInfo traceInfo) {
        given().port(zipkin.getFirstMappedPort())
            .accept(JSON)
        .when()
            .get("/zipkin/api/v2/trace/" + traceInfo.traceId)
        .then()
            .statusCode(200)
            .contentType(JSON)
            .body("size()", equalTo(2))
            .body("findAll { it.name == 'http get /greet/{name}' }.size()", equalTo(1))
            .body("findAll { it.name == 'get' }.size()", equalTo(1))
            .rootPath("find { it.name == 'http get /greet/{name}' }")
                .body("traceId", equalTo(traceInfo.traceId))
                .body("id", equalTo(traceInfo.spanId))
                .body("parentId", is(nullValue()))
                .body("kind", equalTo("SERVER"))
                .body("localEndpoint.serviceName", equalTo("boot3-retrofit-sample"))
                .body("tags['method']", equalTo("GET"))
                .body("tags['http.url']", equalTo("/greet/Suzy"))
                .body("tags['outcome']", equalTo("SUCCESS"))
                .body("tags['status']", equalTo("200"))
                .body("tags['uri']", equalTo("/greet/{name}"))
            .detachRootPath("")
            .rootPath("find { it.name == 'get' }")
                .body("traceId", equalTo(traceInfo.traceId))
                .body("id", not(nullValue()))
                .body("parentId", not(nullValue()))
                .body("kind", equalTo("CLIENT"))
                .body("localEndpoint.serviceName", equalTo("boot3-retrofit-sample"))
                .body("tags['host']", equalTo("localhost"))
                .body("tags['method']", equalTo("GET"))
                .body("tags['outcome']", equalTo("SUCCESS"))
                .body("tags['status']", equalTo("200"))
                .body("tags['target.host']", equalTo("localhost"))
                .body("tags['target.port']", equalTo("7100"))
                .body("tags['target.scheme']", equalTo("http"));
    }

}
