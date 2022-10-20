package com.example.micrometer;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class ReactiveNewTransactionService {

    private static final Logger log = LoggerFactory.getLogger(ReactiveNewTransactionService.class);

    private final ReactiveCustomerRepository repository;

    private final ReactiveContinuedTransactionService reactiveContinuedTransactionService;

    private final Tracer tracer;

    public ReactiveNewTransactionService(ReactiveCustomerRepository repository,
            ReactiveContinuedTransactionService reactiveContinuedTransactionService, Tracer tracer) {
        this.repository = repository;
        this.reactiveContinuedTransactionService = reactiveContinuedTransactionService;
        this.tracer = tracer;
    }

    @Transactional
    public Mono<Void> newTransaction() {
        return Mono
                .fromRunnable(() -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer",
                        tracer.currentSpan().context().traceId()))
                // save a few customers
                .then(repository.save(new ReactiveCustomer("Jack", "Bauer")))
                .then(repository.save(new ReactiveCustomer("Chloe", "O'Brian")))
                .then(repository.save(new ReactiveCustomer("Kim", "Bauer")))
                .then(repository.save(new ReactiveCustomer("David", "Palmer")))
                .then(repository.save(new ReactiveCustomer("Michelle", "Dessler"))).doOnNext(reactiveCustomer -> {
                    log.info("Customers found with findAll():");
                    log.info("-------------------------------");
                }).flatMapMany(reactiveCustomer -> repository.findAll()).doOnNext(cust -> log.info(cust.toString()))
                .doOnNext(o -> log.info("")).then(this.reactiveContinuedTransactionService.continuedTransaction());
    }

}
