package com.example.micrometer;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class RsocketServerApplication {

    public static void main(String... args) {
        new SpringApplication(RsocketServerApplication.class).run(args);
    }

}

@Controller
class RSocketController {

    private static final Logger log = LoggerFactory.getLogger(RSocketController.class);

    private final Tracer tracer;

    RSocketController(Tracer tracer) {
        this.tracer = tracer;
    }

    @MessageMapping("foo")
    public Mono<String> span() {
        String traceId = this.tracer.currentSpan().context().traceId();
        log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", traceId);
        return Mono.just(traceId);
    }

}
