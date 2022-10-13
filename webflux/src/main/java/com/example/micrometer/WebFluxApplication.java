package com.example.micrometer;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class WebFluxApplication {

    public static void main(String... args) {
        new SpringApplication(WebFluxApplication.class).run(args);
    }

}

@RestController
class WebFluxController {

    private static final Logger log = LoggerFactory.getLogger(WebFluxController.class);

    private final Tracer tracer;

    WebFluxController(Tracer tracer) {
        this.tracer = tracer;
    }

    @RequestMapping("/")
    public Mono<String> span() {
        String traceId = this.tracer.currentSpan().context().traceId();
        log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", traceId);
        return Mono.just(traceId);
    }

}
