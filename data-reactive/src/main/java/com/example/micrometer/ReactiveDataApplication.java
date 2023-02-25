package com.example.micrometer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;

import java.time.Duration;

@SpringBootApplication
public class ReactiveDataApplication {

    private static final Logger log = LoggerFactory.getLogger(ReactiveDataApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(ReactiveDataApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public CommandLineRunner demo(ReactiveNewTransactionService reactiveNewTransactionService,
            ObservationRegistry observationRegistry) {
        return (args) -> {
            try {
                Observation observation = Observation.start("reactive-data", observationRegistry);
                reactiveNewTransactionService.newTransaction()
                    .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation))
                    .doFinally(signalType -> observation.stop())
                    .block(Duration.ofSeconds(50));
            }
            catch (DataAccessException e) {
                log.info("Expected to throw an exception so that we see if rollback works", e);
            }
        };
    }

}
