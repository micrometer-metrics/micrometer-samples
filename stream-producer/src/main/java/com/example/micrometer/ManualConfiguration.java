package com.example.micrometer;

import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * In this class we'll add all the manual configuration required for Observability to
 * work.
 */
@Configuration(proxyBeanMethods = false)
public class ManualConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    ListenerContainerCustomizer<AbstractMessageListenerContainer> observedListenerContainerCustomizer() {
        return (container, destinationName, group) -> container.setObservationEnabled(true);
    }

}
