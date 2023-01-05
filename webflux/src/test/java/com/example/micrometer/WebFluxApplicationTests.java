package com.example.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.tck.MeterRegistryAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.BDDAssertions.then;

@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebFluxApplicationTests {

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry meterRegistry;

    @Test
    void should_record_metrics() {
        String response = new RestTemplate().getForObject("http://localhost:" + port + "/", String.class);

        then(response).isNotBlank();
        MeterRegistryAssert.then(meterRegistry).hasTimerWithNameAndTagKeys("http.server.requests", "error", "exception",
                "method", "outcome", "status", "uri");
    }

}
