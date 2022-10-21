package com.example.micrometer;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.actuate.metrics.web.client.ObservationRestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;

/**
 * In this class we'll add all the manual configuration required for Observability to
 * work.
 */
@Configuration(proxyBeanMethods = false)
public class ManualConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    org.springframework.vault.client.RestTemplateCustomizer traceVaultRestTemplateCustomizer(
            ObservationRegistry observationRegistry) {
        return restTemplate -> new ObservationRestTemplateCustomizer(observationRegistry,
                new DefaultClientRequestObservationConvention()).customize(restTemplate);
    }

}
