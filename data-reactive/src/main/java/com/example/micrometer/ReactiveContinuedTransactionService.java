package com.example.micrometer;

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

    private final ReactiveNestedTransactionService reactiveNestedTransactionService;

    public ReactiveContinuedTransactionService(ReactiveCustomerRepository repository, Tracer tracer,
            ReactiveNestedTransactionService reactiveNestedTransactionService) {
        this.repository = repository;
        this.tracer = tracer;
        this.reactiveNestedTransactionService = reactiveNestedTransactionService;
    }

    @Transactional
    public Mono<Void> continuedTransaction() {
        return Mono.fromRunnable(() -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer",
                tracer.currentSpan().context().traceId())).then(repository.findById(1L)).doOnNext(customer -> {
                    // fetch an individual customer by ID
                    log.info("Customer found with findById(1L):");
                    log.info("--------------------------------");
                    log.info(customer.toString());
                    log.info("");
                }).doOnNext(customer -> {
                    // fetch customers by last name
                    log.info("Customer found with findByLastName('Bauer'):");
                    log.info("--------------------------------------------");
                }).flatMapMany(customer -> repository.findByLastName("Bauer"))
                .doOnNext(cust -> log.info(cust.toString())).then(this.reactiveNestedTransactionService.requiresNew());
    }

}
