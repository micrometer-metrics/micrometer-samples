package com.example.micrometer;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class BaggageConsumerApplication {

    public static void main(String... args) {
        new SpringApplication(BaggageConsumerApplication.class).run(args);
    }

}

@RestController
class BaggageController {

    private static final Logger log = LoggerFactory.getLogger(BaggageController.class);

    private final Tracer tracer;

    // Used for tests - ignore me
    private final RemoteFieldsTestChecker remoteFieldsTestChecker;

    BaggageController(Tracer tracer, RemoteFieldsTestChecker remoteFieldsTestChecker) {
        this.tracer = tracer;
        this.remoteFieldsTestChecker = remoteFieldsTestChecker;
    }

    @GetMapping("/")
    public String span() {
        log.info("Contains the following baggage {}", this.tracer.getAllBaggage());
        String traceId = this.tracer.currentSpan().context().traceId();
        log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", traceId);
        // Used for tests - ignore me
        this.remoteFieldsTestChecker.assertThatFieldGotPropagated();
        return traceId;
    }

}

// Used for tests - ignore me
@Configuration(proxyBeanMethods = false)
class TestConfiguration {

    @Bean
    RemoteFieldsTestChecker remoteFieldsChecker(RestTemplateBuilder restTemplateBuilder) {
        return new RemoteFieldsTestChecker(restTemplateBuilder.build());
    }

}

// Used for tests - ignore me
class RemoteFieldsTestChecker {

    private static final Logger log = LoggerFactory.getLogger(RemoteFieldsTestChecker.class);

    private final WireMockServer wireMockServer;

    private final RestTemplate restTemplate;

    RemoteFieldsTestChecker(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        this.wireMockServer.start();
        this.wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/foo"))
                .withHeader("myremotefield", WireMock.equalTo("my-remote-field-value"))
                .willReturn(WireMock.aResponse().withStatus(200).withBody("OK")));
    }

    void assertThatFieldGotPropagated() {
        String object = this.restTemplate.getForObject("http://localhost:" + this.wireMockServer.port() + "/foo",
                String.class);
        if (!"OK".equals(object)) {
            throw new IllegalStateException("WireMock failed to respond correctly");
        }
        // [myremotefield] was set in the baggage-producer
        this.wireMockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/foo")).withHeader("myremotefield",
                WireMock.equalTo("my-remote-field-value")));
        log.info("Successfully propagated the headers!");
    }

    @PreDestroy
    void destroy() {
        this.wireMockServer.shutdown();
    }

}
