package com.example.micrometer;

import io.micrometer.tracing.Tracer;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ContinuedTransactionService {

    private static final Logger log = LoggerFactory.getLogger(ContinuedTransactionService.class);

    private final CustomerRepository repository;

    private final NestedTransactionService nestedTransactionService;

    private final Tracer tracer;

    public ContinuedTransactionService(CustomerRepository repository, NestedTransactionService nestedTransactionService,
            Tracer tracer) {
        this.repository = repository;
        this.nestedTransactionService = nestedTransactionService;
        this.tracer = tracer;
    }

    @Transactional
    public void continuedTransaction() {
        log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", tracer.currentSpan().context().traceId());

        // fetch an individual customer by ID
        Customer customer = repository.findById(1L);
        log.info("Customer found with findById(1L):");
        log.info("--------------------------------");
        log.info(customer.toString());
        log.info("");

        // fetch customers by last name
        log.info("Customer found with findByLastName('Bauer'):");
        log.info("--------------------------------------------");
        repository.findByLastName("Bauer").forEach(bauer -> {
            log.info(bauer.toString());
        });
        nestedTransactionService.newTransaction();
    }

}
