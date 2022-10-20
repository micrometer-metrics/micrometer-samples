package com.example.micrometer;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.reactive.ServerHttpObservationFilter;
import org.springframework.web.server.WebFilter;

/**
 * In this class we'll add all the manual configuration required for
 * Observability to work.
 */
@Configuration(proxyBeanMethods = false)
public class ManualConfiguration {
    // You must set this manually until this is registered in Boot
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    WebFilter observationWebFilter(ObservationRegistry observationRegistry) {
        return new ServerHttpObservationFilter(observationRegistry);
    }

}
