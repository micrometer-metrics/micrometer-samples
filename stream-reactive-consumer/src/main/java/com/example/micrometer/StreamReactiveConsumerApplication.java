package com.example.micrometer;

import java.util.function.Consumer;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;

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
        // For the reactive consumer remember to call "subscribe()" at the end, otherwise
        // you'll get the "Dispatcher has no subscribers" error
        Observation observation = Observation.start("stream-reactive-consumer-app", observationRegistry);

        Consumer<Flux<Message<String>>> result = messageFlux -> {
            messageFlux.flatMap(message -> Mono.deferContextual(contextView -> {
                Runnable runnable = () -> {
                    log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer",
                            tracer.currentSpan().context().traceId());
                };
                scoped(contextView, runnable);
                return Mono.just(message);

            })).doOnNext(s -> {
                // tracer.currentSpan().end();
                // tracer.withSpan(null);
            }).doFinally(signalType -> observation.stop())
                    .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation)).subscribe();
        };
        return result;

// @formatter:off
//		return i -> i
//					.doOnNext(s ->
//						log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", tracer.currentSpan().context().traceId()))
//					// You must finish the span yourself and clear the tracing context like presented below.
//					// Otherwise you will be missing out the span that wraps the function execution.
//					.doOnNext(s -> {
//						tracer.currentSpan().end();
//						tracer.withSpan(null);
//					})
//					.subscribe();
// @formatter:on
    }

    private void scoped(ContextView contextView, Runnable runnable) {
        try (ContextSnapshot.Scope scope = ContextSnapshot.setThreadLocalsFrom(contextView,
                ObservationThreadLocalAccessor.KEY)) {
            runnable.run();
        }
    }

}
