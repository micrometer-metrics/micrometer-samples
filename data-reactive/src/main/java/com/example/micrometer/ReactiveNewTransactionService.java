package com.example.micrometer;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReactiveNewTransactionService {

    private static final Logger log = LoggerFactory.getLogger(ReactiveNewTransactionService.class);

    private final ReactiveCustomerRepository repository;

    private final ReactiveContinuedTransactionService reactiveContinuedTransactionService;

    private final Tracer tracer;

    private final ObservationRegistry observationRegistry;

    public ReactiveNewTransactionService(ReactiveCustomerRepository repository,
            ReactiveContinuedTransactionService reactiveContinuedTransactionService, Tracer tracer,
            ObservationRegistry observationRegistry) {
        this.repository = repository;
        this.reactiveContinuedTransactionService = reactiveContinuedTransactionService;
        this.tracer = tracer;
        this.observationRegistry = observationRegistry;
    }

    @Transactional
    public Mono<Void> newTransaction() {
        return Mono.deferContextual(contextView -> {
            try (ContextSnapshot.Scope scope = ContextSnapshot.setThreadLocalsFrom(contextView,
                    ObservationThreadLocalAccessor.KEY)) {
                log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", tracer.currentSpan().context().traceId());
                log.info("Customers found with findAll():");
                log.info("-------------------------------");
            }
            // save a few customers
            return repository.save(new ReactiveCustomer("Jack", "Bauer"));
        })
            .then(repository.save(new ReactiveCustomer("Chloe", "O'Brian")))
            .then(repository.save(new ReactiveCustomer("Kim", "Bauer")))
            .then(repository.save(new ReactiveCustomer("David", "Palmer")))
            .then(repository.save(new ReactiveCustomer("Michelle", "Dessler")))
            .flatMapMany(reactiveCustomer -> repository.findAll())
            .transformDeferredContextual(
                    (reactiveCustomerFlux, contextView) -> reactiveCustomerFlux.doOnNext(reactiveCustomer -> {
                        try (ContextSnapshot.Scope scope = ContextSnapshot.setThreadLocalsFrom(contextView,
                                ObservationThreadLocalAccessor.KEY)) {
                            log.info(reactiveCustomer.toString());
                        }
                    }))
            .then(this.reactiveContinuedTransactionService.continuedTransaction());

    }

}
