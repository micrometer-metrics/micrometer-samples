package com.example.micrometer;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.RSocketServer;
import io.rsocket.micrometer.observation.*;
import io.rsocket.plugins.RSocketInterceptor;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.rsocket.RSocketConnectorConfigurer;

/**
 * In this class we'll add all the manual configuration required for Observability to
 * work.
 */
@Configuration(proxyBeanMethods = false)
public class ManualConfiguration {

    // You must set this manually until this is registered in Boot
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    RSocketResponderTracingObservationHandler rSocketResponderTracingObservationHandler(Tracer tracer,
            Propagator propagator) {
        return new RSocketResponderTracingObservationHandler(tracer, propagator, new ByteBufGetter(), false);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    RSocketRequesterTracingObservationHandler rSocketRequesterTracingObservationHandler(Tracer tracer,
            Propagator propagator) {
        return new RSocketRequesterTracingObservationHandler(tracer, propagator, new ByteBufSetter(), false);
    }

    @Bean
    ObservationRSocketConnectorConfigurer observationRSocketConnectorConfigurer(
            ObservationRegistry observationRegistry) {
        return new ObservationRSocketConnectorConfigurer(observationRegistry);
    }

    @Bean
    ObservationRSocketServerCustomizer observationRSocketServerCustomizer(ObservationRegistry observationRegistry) {
        return new ObservationRSocketServerCustomizer(observationRegistry);
    }

}

class ObservationRSocketConnectorConfigurer implements RSocketConnectorConfigurer {

    private final ObservationRegistry observationRegistry;

    ObservationRSocketConnectorConfigurer(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @Override
    public void configure(RSocketConnector rSocketConnector) {
        rSocketConnector.interceptors(ir -> ir.forResponder(
                (RSocketInterceptor) rSocket -> new ObservationResponderRSocketProxy(rSocket, this.observationRegistry))
                .forRequester((RSocketInterceptor) rSocket -> new ObservationRequesterRSocketProxy(rSocket,
                        this.observationRegistry)));
    }

}

class ObservationRSocketServerCustomizer implements RSocketServerCustomizer {

    private final ObservationRegistry observationRegistry;

    ObservationRSocketServerCustomizer(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @Override
    public void customize(RSocketServer rSocketServer) {
        rSocketServer.interceptors(ir -> ir.forResponder(
                (RSocketInterceptor) rSocket -> new ObservationResponderRSocketProxy(rSocket, this.observationRegistry))
                .forRequester((RSocketInterceptor) rSocket -> new ObservationRequesterRSocketProxy(rSocket,
                        this.observationRegistry)));
    }

}
