package com.example.micrometer;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.observability.MongoObservationCommandListener;
import org.springframework.data.mongodb.observability.MongoTracingObservationHandler;

/**
 * In this class we'll add all the manual configuration required for Observability to
 * work.
 */
@Configuration(proxyBeanMethods = false)
public class ManualConfiguration {

    // You must set this manually until this is registered in Boot
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    MongoClientSettingsBuilderCustomizer observationMongoClientSettingsBuilderCustomizer(
            ObservationRegistry observationRegistry) {
        return clientSettingsBuilder -> clientSettingsBuilder
                .addCommandListener(new MongoObservationCommandListener(observationRegistry));
    }

    @Bean
    MongoTracingObservationHandler mongoTracingObservationHandler(Tracer tracer) {
        return new MongoTracingObservationHandler(tracer);
    }

}
