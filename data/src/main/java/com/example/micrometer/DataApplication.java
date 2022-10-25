package com.example.micrometer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;

@SpringBootApplication
public class DataApplication {

    private static final Logger log = LoggerFactory.getLogger(DataApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(DataApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public CommandLineRunner demo(NewTransactionService newTransactionService,
            ObservationRegistry observationRegistry) {
        return (args) -> {
            try {
                Observation.start("data", observationRegistry).observe(newTransactionService::newTransaction);
            }
            catch (DataAccessException e) {
                log.info("Expected to throw an exception so that we see if rollback works", e);
            }
        };
    }

}
