package com.example.micrometer;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import java.time.Duration;

@SpringBootApplication
public class ReactiveCassandraApplication {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveCassandraApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(ReactiveCassandraApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public CommandLineRunner demo(SampleRepository repository, Tracer tracer, ObservationRegistry observationRegistry) {
        return (args) -> {
            Observation observation = Observation.start("cassandra-reactive-app", observationRegistry);
            Mono.just(observation).flatMap(span -> {
                observation.scoped(() -> logger.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer",
                        tracer.currentSpan().context().traceId()));
                return repository.save(new SampleEntity("test"))
                    .flatMap(savedEntity -> repository.findById(savedEntity.getId()))
                    .doOnNext(foundEntity -> logger.info("Found entity: " + foundEntity));
            })
                .doFinally(signalType -> observation.stop())
                .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation))
                .block(Duration.ofMinutes(1));
        };
    }

    @Bean
    CqlSession cqlSession(CqlSessionBuilder cqlSessionBuilder) {
        try (CqlSession session = cqlSessionBuilder.build()) {
            session.execute(
                    "CREATE KEYSPACE IF NOT EXISTS example WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };");
        }
        return cqlSessionBuilder.withKeyspace("example").build();
    }

}
