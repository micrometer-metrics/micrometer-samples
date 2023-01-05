package com.example.micrometer;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.time.Duration;

@SpringBootApplication
public class ReactiveCircuitBreakerApplication implements CommandLineRunner {

    public static void main(String... args) {
        new SpringApplicationBuilder(ReactiveCircuitBreakerApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Autowired
    CircuitService circuitService;

    @Override
    public void run(String... args) throws Exception {
        this.circuitService.call().block(Duration.ofSeconds(1));
    }

}

@Service
class CircuitService {

    private static final Logger log = LoggerFactory.getLogger(CircuitService.class);

    private final ReactiveCircuitBreakerFactory factory;

    private final Tracer tracer;

    private final ObservationRegistry observationRegistry;

    CircuitService(ReactiveCircuitBreakerFactory factory, Tracer tracer, ObservationRegistry observationRegistry) {
        this.factory = factory;
        this.tracer = tracer;
        this.observationRegistry = observationRegistry;
    }

    Mono<String> call() {
        Observation observation = Observation.start("reactive-circuit-breaker", observationRegistry);
        // You don't need this in your code unless you want to create new spans manually
        return Mono.deferContextual(contextView -> {
            // You don't need this in your code unless you want to create new spans
            // manually
            return this.factory.create("circuit").run(Mono.defer(() -> {
                scoped(contextView, () -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer",
                        this.tracer.currentSpan().context().traceId()));
                return Mono.error(new IllegalStateException("BOOM"));
            }), throwable -> {
                scoped(contextView, () -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer",
                        this.tracer.currentSpan().context().traceId()));
                return Mono.just("fallback");
            });
        }).contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation))
                .doFinally(signalType -> observation.stop());
    }

    private void scoped(ContextView contextView, Runnable runnable) {
        try (ContextSnapshot.Scope scope = ContextSnapshot.setThreadLocalsFrom(contextView,
                ObservationThreadLocalAccessor.KEY)) {
            runnable.run();
        }
    }

}
