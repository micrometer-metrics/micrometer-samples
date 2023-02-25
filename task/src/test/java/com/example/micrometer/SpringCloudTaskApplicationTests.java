package com.example.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.tck.MeterRegistryAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;

@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "debug=true")
class SpringCloudTaskApplicationTests {

    @Autowired
    MeterRegistry meterRegistry;

    @Test
    void should_record_metrics() {
        MeterRegistryAssert.then(meterRegistry)
            .hasTimerWithNameAndTagKeys("spring.cloud.task.runner", "error", "spring.cloud.task.runner.bean-name");
    }

}
