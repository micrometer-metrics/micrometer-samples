package com.example.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureObservability
class MvcApplicationTests {

    @Autowired
    MeterRegistry meterRegistry;

    @Test
    void should_work_for_metrics() {
        this.meterRegistry.getMeters()
    }

    static class

}
