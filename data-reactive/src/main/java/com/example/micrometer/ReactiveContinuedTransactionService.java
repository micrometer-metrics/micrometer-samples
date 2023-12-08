package com.example.micrometer;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class ReactiveContinuedTransactionService {

    private static final Logger log = LoggerFactory.getLogger(ReactiveContinuedTransactionService.class);

    private final ReactiveCustomerRepository repository;

    private final Tracer tracer;

    private final ObservationRegistry observationRegistry;

    private final ReactiveNestedTransactionService reactiveNestedTransactionService;

    private final ContextSnapshotFactory contextSnapshotFactory = ContextSnapshotFactory.builder().build();

    public ReactiveContinuedTransactionService(ReactiveCustomerRepository repository, Tracer tracer,
            ObservationRegistry observationRegistry,
            ReactiveNestedTransactionService reactiveNestedTransactionService) {
        this.repository = repository;
        this.tracer = tracer;
        this.observationRegistry = observationRegistry;
        this.reactiveNestedTransactionService = reactiveNestedTransactionService;
    }

    @Transactional
    public Mono<Void> continuedTransaction() {
        return Mono.deferContextual(contextView -> {
            try (ContextSnapshot.Scope scope = this.contextSnapshotFactory.setThreadLocalsFrom(contextView,
                    ObservationThreadLocalAccessor.KEY)) {
                log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", tracer.currentSpan().context().traceId());
            }
            return repository.findById(1L);
        })
            .transformDeferredContextual(
                    (reactiveCustomerMono, contextView) -> reactiveCustomerMono.doOnNext(customer -> {
                        try (ContextSnapshot.Scope scope = this.contextSnapshotFactory.setThreadLocalsFrom(contextView,
                                ObservationThreadLocalAccessor.KEY)) {
                            // fetch an individual customer by ID
                            log.info("Customer found with findById(1L):");
                            log.info("--------------------------------");
                            log.info(customer.toString());
                            log.info("");
                            // fetch customers by last name
                            log.info("Customer found with findByLastName('Bauer'):");
                            log.info("--------------------------------------------");
                        }
                    }).flatMapMany(customer -> repository.findByLastName("Bauer")).doOnNext(reactiveCustomer -> {
                        try (ContextSnapshot.Scope scope = this.contextSnapshotFactory.setThreadLocalsFrom(contextView,
                                ObservationThreadLocalAccessor.KEY)) {
                            log.info(reactiveCustomer.toString());
                        }
                    }).then(this.reactiveNestedTransactionService.requiresNew()));
    }

}
