package com.example.micrometer;

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

    private static final Logger log = LoggerFactory.getLogger(ReactiveCassandraApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(ReactiveCassandraApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public CommandLineRunner demo(BasicUserRepository basicUserRepository, ObservationRegistry observationRegistry,
            Tracer tracer) {
        return (args) -> {
            Observation observation = Observation.start("cassandra-reactive-app", observationRegistry);
            Mono.just(observation).flatMap(span -> {
                observation.scoped(() -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer",
                        tracer.currentSpan().context().traceId()));
                return basicUserRepository.save(new User("foo", "bar", "baz", 1L))
                        .flatMap(user -> basicUserRepository.findUserByIdIn(user.getId()));
            }).doFinally(signalType -> observation.stop())
                    .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation))
                    .block(Duration.ofMinutes(1));
        };
    }

}
