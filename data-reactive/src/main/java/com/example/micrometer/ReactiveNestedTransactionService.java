package com.example.micrometer;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class ReactiveNestedTransactionService {

    private static final Logger log = LoggerFactory.getLogger(ReactiveNestedTransactionService.class);

    private final Tracer tracer;

    private final ReactiveCustomerRepository repository;

    public ReactiveNestedTransactionService(Tracer tracer, ReactiveCustomerRepository repository) {
        this.tracer = tracer;
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<Void> requiresNew() {
        return Mono
                .fromRunnable(() -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer requires new",
                        tracer.currentSpan().context().traceId()))
                .then(repository.save(new ReactiveCustomer("Hello", "From Propagated Transaction")))
                .doOnNext(customerFlux -> repository.deleteById(10238L)).doOnNext(customerFlux -> log.info("")).then();
    }

}
