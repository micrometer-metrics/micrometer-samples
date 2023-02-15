package com.example.micrometer;

import jakarta.annotation.PostConstruct;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * In this class we'll add all the manual configuration required for Observability to
 * work.
 */
@Configuration(proxyBeanMethods = false)
public class ManualConfiguration {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @PostConstruct
    void setup() {
        this.rabbitTemplate.setObservationEnabled(true);
    }

}
