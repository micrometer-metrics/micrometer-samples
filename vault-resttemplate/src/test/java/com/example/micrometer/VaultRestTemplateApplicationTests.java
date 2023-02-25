package com.example.micrometer;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "debug=true")
class VaultRestTemplateApplicationTests {

    static WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry meterRegistry;

    @BeforeAll
    static void setup() {
        wireMockServer.start();
        wireMockServer
            .stubFor(WireMock.get("/secret/foo").willReturn(WireMock.aResponse().withBody("{ }").withStatus(200)));
    }

    @AfterAll
    static void close() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void vaultProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.vault.port", () -> wireMockServer.port());
    }

    @Test
    void should_record_metrics() {
        MeterRegistryAssert.then(meterRegistry)
            .hasTimerWithNameAndTagKeys("http.client.requests", "error", "exception", "method", "outcome", "status",
                    "uri");
    }

    @TestConfiguration(proxyBeanMethods = false) // otherwise the Vault autoconfig doesn't
                                                 // hook in
    static class Config {

        @Bean
        ObservationRegistry testObservationRegistry() {
            return ObservationRegistry.create();
        }

    }

}
