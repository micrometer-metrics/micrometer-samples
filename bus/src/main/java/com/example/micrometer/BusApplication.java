package com.example.micrometer;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.bus.event.MyEvent;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class BusApplication implements CommandLineRunner {

    public static void main(String... args) {
        new SpringApplicationBuilder(BusApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Autowired
    MyEventService myEventService;

    @Override
    public void run(String... args) throws Exception {
        this.myEventService.publish();
    }

}

@Configuration
@RemoteApplicationEventScan(basePackageClasses = MyEvent.class)
class EventConfig {

}

@RestController
class MyEventService {

    private static final Logger log = LoggerFactory.getLogger(MyEventService.class);

    private final ApplicationEventPublisher publisher;

    private final BusProperties bus;

    private final Tracer tracer;

    MyEventService(ApplicationEventPublisher publisher, BusProperties bus, Tracer tracer) {
        this.publisher = publisher;
        this.bus = bus;
        this.tracer = tracer;
    }

    void publish() {
        Span span = this.tracer.nextSpan();
        try (Tracer.SpanInScope ws = this.tracer.withSpan(span.start())) {
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", this.tracer.currentSpan().context().traceId());
            publisher.publishEvent(new MyEvent(this, this.bus.getId()));
        }
        finally {
            span.end();
        }
    }

    @EventListener(MyEvent.class)
    public void gotLoanIssued() {
        log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", this.tracer.currentSpan().context().traceId());
    }

}
