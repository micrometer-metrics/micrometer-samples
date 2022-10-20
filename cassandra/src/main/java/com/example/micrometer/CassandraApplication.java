package com.example.micrometer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;

@SpringBootApplication
public class CassandraApplication {

    private static final Logger log = LoggerFactory.getLogger(CassandraApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(CassandraApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public CommandLineRunner demo(BasicUserRepository basicUserRepository, Tracer tracer, ObservationRegistry observationRegistry) {
        return (args) -> {
            Observation.createNotStarted("cassandra-app", observationRegistry)
                    .observe(() -> {
                        try {
                            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", tracer.currentSpan().context().traceId());
                            User save = basicUserRepository.save(new User("foo", "bar", "baz", 1L));
                            User userByIdIn = basicUserRepository.findUserByIdIn(save.getId());
                            basicUserRepository.findUserByIdIn(123123L);
                        }
                        catch (DataAccessException e) {
                            log.info("Expected to throw an exception so that we see if rollback works", e);
                        }
                    });
        };
    }

}
