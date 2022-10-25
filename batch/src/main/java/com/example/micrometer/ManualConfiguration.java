package com.example.micrometer;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.batch.core.observability.BatchMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * In this class we'll add all the manual configuration required for Observability to
 * work.
 */
@Configuration(proxyBeanMethods = false)
public class ManualConfiguration {

    @Autowired
    Tracer tracer;

    @PostConstruct
    void setupBatchMetrics() {
        BatchMetrics.observationRegistry.observationConfig()
                .observationHandler(new DefaultTracingObservationHandler(tracer));
    }

}
