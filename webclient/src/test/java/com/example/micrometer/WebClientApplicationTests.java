package com.example.micrometer;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.tck.MeterRegistryAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebClientApplicationTests {

    static WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(7110));

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry meterRegistry;

    @BeforeAll
    static void setup() {
        wireMockServer.start();
        wireMockServer.stubFor(WireMock.get("/").willReturn(WireMock.aResponse().withBody("foo").withStatus(200)));
    }

    @AfterAll
    static void close() {
        wireMockServer.stop();
    }

    @Test
    void should_record_metrics() {
        MeterRegistryAssert.then(meterRegistry).hasTimerWithNameAndTagKeys("http.client.requests", "error", "exception",
                "method", "outcome", "status", "uri");
    }

}
