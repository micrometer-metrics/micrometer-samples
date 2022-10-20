package com.example.micrometer;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

/**
 * In this class we'll add all the manual configuration required for
 * Observability to work.
 */
@Configuration(proxyBeanMethods = false)
public class ManualConfiguration {

    @Autowired
    ConcurrentKafkaListenerContainerFactory concurrentKafkaListenerContainerFactory;

    @PostConstruct
    void setup() {
        this.concurrentKafkaListenerContainerFactory.getContainerProperties().setObservationEnabled(true);
    }

}
