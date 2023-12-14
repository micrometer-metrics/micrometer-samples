package com.example.micrometer;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReactiveNestedTransactionService {

    private static final Logger log = LoggerFactory.getLogger(ReactiveNestedTransactionService.class);

    private final Tracer tracer;

    private final ObservationRegistry observationRegistry;

    private final ReactiveCustomerRepository repository;

    private final ContextSnapshotFactory contextSnapshotFactory = ContextSnapshotFactory.builder().build();

    public ReactiveNestedTransactionService(Tracer tracer, ObservationRegistry observationRegistry,
            ReactiveCustomerRepository repository) {
        this.tracer = tracer;
        this.observationRegistry = observationRegistry;
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<Void> requiresNew() {
        return Mono.deferContextual(contextView -> {
            try (ContextSnapshot.Scope scope = this.contextSnapshotFactory.setThreadLocalsFrom(contextView,
                    ObservationThreadLocalAccessor.KEY)) {
                log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer requires new",
                        tracer.currentSpan().context().traceId());
            }
            // save a few customers
            return repository.save(new ReactiveCustomer("Hello", "From Propagated Transaction"));
        }).doOnNext(customerFlux -> repository.deleteById(10238L)).then();
    }

}
