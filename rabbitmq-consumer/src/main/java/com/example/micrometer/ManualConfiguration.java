package com.example.micrometer;

import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In this class we'll add all the manual configuration required for Observability to
 * work.
 */
@Configuration(proxyBeanMethods = false)
public class ManualConfiguration {

    @Bean
    ContainerCustomizer<SimpleMessageListenerContainer> containerCustomizer() {
        return (container) -> container.setObservationEnabled(true);
    }

}
