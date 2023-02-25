package com.example.micrometer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Function;
import java.util.function.Supplier;

@SpringBootApplication
public class StreamReactiveProducerApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StreamReactiveProducerApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(StreamReactiveProducerApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Autowired
    StreamBridgeService streamBridgeService;

    @Override
    public void run(String... args) throws Exception {
        this.streamBridgeService.call();
    }

    // Function<Mono<?>, Mono<?>> and Supplier<Mono<?>> and Consumer<Mono<?>> are not
    // supported in Stream

    @Bean
    Function<Flux<Message<String>>, Flux<Message<String>>> tracingFunction(Tracer tracer) {
        return s -> s.doOnNext(i -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer function flux",
                tracer.currentSpan().context().traceId()));
    }

    // @PollableBean
    Supplier<Flux<String>> supplier(Tracer tracer) {
        return () -> Flux.just("HELLO")
            .doOnNext(s -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer supplier mono",
                    tracer.currentSpan().context().traceId()));
    }

    // @PollableBean
    Supplier<Flux<String>> stringSupplier(Tracer tracer) {
        return () -> Flux.just("a", "b")
            .doOnNext(s -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer supplier flux",
                    tracer.currentSpan().context().traceId()));
    }

}

@Service
class StreamBridgeService {

    private static final Logger log = LoggerFactory.getLogger(StreamBridgeService.class);

    private final StreamBridge streamBridge;

    private final Tracer tracer;

    private final ObservationRegistry observationRegistry;

    StreamBridgeService(StreamBridge streamBridge, Tracer tracer, ObservationRegistry observationRegistry) {
        this.streamBridge = streamBridge;
        this.tracer = tracer;
        this.observationRegistry = observationRegistry;
    }

    void call() {
        Observation.createNotStarted("stream.producer", this.observationRegistry).observe(() -> {
            log.info("Hello from stream bridge - trace <{}>", this.tracer.currentSpan().context().traceId());
            this.streamBridge.send("tracingFunction-in-0", "HELLO");
        });
    }

}
