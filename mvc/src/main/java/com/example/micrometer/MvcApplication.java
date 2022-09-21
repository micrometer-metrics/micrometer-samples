package com.example.micrometer;

import java.util.Map;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class MvcApplication {

    public static void main(String... args) {
        new SpringApplication(MvcApplication.class).run(args);
    }

}

@RestController
class MvcController {

    private static final Logger log = LoggerFactory.getLogger(MvcController.class);

    private final Tracer tracer;

    MvcController(Tracer tracer) {
        this.tracer = tracer;
    }

    // TODO: Uncomment this once Mvc gets instrumented in Framework
    // @GetMapping("/")
    // public String span() {
    // String traceId = this.tracer.currentSpan().context().traceId();
    // log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", traceId);
    // return traceId;
    // }

    @GetMapping("/")
    public String span(@RequestHeader Map<String, String> headers) {
        String traceId = headers.get("traceparent");
        Assert.notNull(traceId, "traceparent must not be null");
        log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", traceId.split("-")[1]);
        return traceId;
    }

}
