package com.example.micrometer;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.SessionBuilderConfigurer;
import org.springframework.data.cassandra.observability.ObservableCqlSessionFactoryBean;
import org.springframework.data.cassandra.observability.ObservableReactiveSessionFactoryBean;
import org.springframework.data.cassandra.observability.ObservationRequestTracker;

@Configuration
public class ManualConfiguration {

    @Bean
    ObservableCqlSessionFactoryBean cqlSessionFactoryBean(CqlSessionBuilder cqlSessionBuilder,
            ObservationRegistry observationRegistry) {
        return new ObservableCqlSessionFactoryBean(cqlSessionBuilder, observationRegistry);
    }

    @Bean
    ObservableReactiveSessionFactoryBean reactiveSessionFactoryBean(CqlSession cqlSession,
            ObservationRegistry observationRegistry) {
        return new ObservableReactiveSessionFactoryBean(cqlSession, observationRegistry);
    }

    @Bean
    public SessionBuilderConfigurer getSessionBuilderConfigurer() {
        return sessionBuilder -> sessionBuilder.addRequestTracker(ObservationRequestTracker.INSTANCE);
    }

}
