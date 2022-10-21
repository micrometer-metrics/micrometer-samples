package com.example.micrometer;

import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.cloud.stream.config.ProducerMessageHandlerCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;

/**
 * In this class we'll add all the manual configuration required for Observability to
 * work.
 */
@Configuration(proxyBeanMethods = false)
public class ManualConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    ListenerContainerCustomizer<MessageListenerContainer> observedListenerContainerCustomizer(ApplicationContext applicationContext) {
        return (container, destinationName, group) -> {
            AbstractMessageListenerContainer abstractMessageListenerContainer = ((AbstractMessageListenerContainer) container);
            abstractMessageListenerContainer.setObservationEnabled(true);
            abstractMessageListenerContainer.setApplicationContext(applicationContext);
        };
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    ProducerMessageHandlerCustomizer<AmqpOutboundEndpoint> observedProducerMessageHandlerCustomizer(ApplicationContext applicationContext) {
        return (handler, destinationName) -> {
            handler.getRabbitTemplate().setObservationEnabled(true);
            handler.getRabbitTemplate().setApplicationContext(applicationContext);
        };
    }

}
