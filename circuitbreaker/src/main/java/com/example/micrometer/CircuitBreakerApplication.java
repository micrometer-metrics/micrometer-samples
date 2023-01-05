package com.example.micrometer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class CircuitBreakerApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(CircuitBreakerApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Autowired
    CircuitService circuitService;

    @Override
    public void run(String... args) throws Exception {
        this.circuitService.call();
    }

}

@Service
class CircuitService {

    private static final Logger log = LoggerFactory.getLogger(CircuitService.class);

    private final CircuitBreakerFactory factory;

    private final Tracer tracer;

    private final ObservationRegistry observationRegistry;

    CircuitService(CircuitBreakerFactory factory, Tracer tracer, ObservationRegistry observationRegistry) {
        this.factory = factory;
        this.tracer = tracer;
        this.observationRegistry = observationRegistry;
    }

    String call() {
        return Observation.createNotStarted("circuitbreaker", observationRegistry)
                .observe(() -> this.factory.create("circuit").run(() -> {
                    log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer",
                            this.tracer.currentSpan().context().traceId());
                    throw new IllegalStateException("BOOM");
                }, throwable -> {
                    log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer",
                            this.tracer.currentSpan().context().traceId());
                    return "fallback";
                }));
    }

}
