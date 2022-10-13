package com.example.micrometer;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NestedTransactionService {

    private static final Logger log = LoggerFactory.getLogger(NestedTransactionService.class);

    private final CustomerRepository repository;

    private final Tracer tracer;

    public NestedTransactionService(CustomerRepository repository, Tracer tracer) {
        this.repository = repository;
        this.tracer = tracer;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void newTransaction() {
        log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer requires new",
                tracer.currentSpan().context().traceId());
        repository.save(new Customer("Hello", "From Propagated Transaction"));
        repository.deleteById(10238L);
        log.info("");
    }

}
