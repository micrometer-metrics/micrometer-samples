package com.example.micrometer;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NewTransactionService {

    private static final Logger log = LoggerFactory.getLogger(NewTransactionService.class);

    private final CustomerRepository repository;

    private final ContinuedTransactionService continuedTransactionService;

    public NewTransactionService(CustomerRepository repository,
            ContinuedTransactionService continuedTransactionService) {
        this.repository = repository;
        this.continuedTransactionService = continuedTransactionService;
    }

    @Transactional
    public void newTransaction() {
        // log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer",
        // tracer.currentSpan().context().traceId());
        // TX is not working

        // save a few customers
        repository.save(new Customer("Jack", "Bauer"));
        repository.save(new Customer("Chloe", "O'Brian"));
        repository.save(new Customer("Kim", "Bauer"));
        repository.save(new Customer("David", "Palmer"));
        repository.save(new Customer("Michelle", "Dessler"));

        // fetch all customers
        log.info("Customers found with findAll():");
        log.info("-------------------------------");
        for (Customer customer : repository.findAll()) {
            log.info(customer.toString());
        }
        log.info("");

        this.continuedTransactionService.continuedTransaction();
    }

}
