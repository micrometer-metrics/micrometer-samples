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
import org.springframework.stereotype.Service;

@SpringBootApplication
public class StreamProducerApplication implements CommandLineRunner {

    public static void main(String... args) {
        new SpringApplicationBuilder(StreamProducerApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Autowired
    StreamBridgeService streamBridgeService;

    @Override
    public void run(String... args) throws Exception {
        this.streamBridgeService.call();
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
        Observation.createNotStarted("stream.producer", observationRegistry)
                .observe(() -> {
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", this.tracer.currentSpan().context().traceId());
            this.streamBridge.send("channel-out-0", "HELLO");
        });
    }

}
