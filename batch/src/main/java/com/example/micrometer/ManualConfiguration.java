package com.example.micrometer;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import org.springframework.batch.core.configuration.annotation.BatchObservabilityBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In this class we'll add all the manual configuration required for Observability to
 * work.
 */
@Configuration(proxyBeanMethods = false)
public class ManualConfiguration {

    @Bean
    public ObservationRegistry observationRegistry(Tracer tracer) {
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        observationRegistry.observationConfig()
                .observationHandler(new DefaultMeterObservationHandler(Metrics.globalRegistry))
                .observationHandler(new DefaultTracingObservationHandler(tracer));
        return observationRegistry;
    }

    @Bean
    public BatchObservabilityBeanPostProcessor batchObservabilityBeanPostProcessor() {
        return new BatchObservabilityBeanPostProcessor();
    }

}
