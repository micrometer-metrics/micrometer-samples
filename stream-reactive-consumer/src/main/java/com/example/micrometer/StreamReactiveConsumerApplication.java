package com.example.micrometer;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@SpringBootApplication
public class StreamReactiveConsumerApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StreamReactiveConsumerApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(StreamReactiveConsumerApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.warn("Remember about calling <.subscribe()> at the end of your Consumer<Flux> bean!");
        log.warn("Remember about finishing the span manually before calling subscribe!");
    }

    @Bean
    Consumer<Flux<Message<String>>> channel(Tracer tracer, ObservationRegistry observationRegistry) {
        return flux -> flux
            .doOnNext(msg -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer",
                    tracer.currentSpan().context().traceId()))
            .subscribe();
    }

}
