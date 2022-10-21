package com.example.micrometer;

import com.mongodb.ConnectionString;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.observability.ContextProviderFactory;
import org.springframework.data.mongodb.observability.MongoObservationCommandListener;

/**
 * In this class we'll add all the manual configuration required for Observability to
 * work.
 */
@Configuration(proxyBeanMethods = false)
public class ManualConfiguration {

    // You must set this manually until this is registered in Boot
    @Bean
    MongoClientSettingsBuilderCustomizer mongoObservabilityCustomizer(ObservationRegistry observationRegistry, MongoProperties mongoProperties) {
        return clientSettingsBuilder -> clientSettingsBuilder.contextProvider(ContextProviderFactory.create(observationRegistry))
                .addCommandListener(new MongoObservationCommandListener(observationRegistry, new ConnectionString(mongoProperties.determineUri())));

    }
}
