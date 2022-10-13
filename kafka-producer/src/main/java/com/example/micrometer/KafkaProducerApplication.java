package com.example.micrometer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class KafkaProducerApplication implements CommandLineRunner {

    public static void main(String... args) {
        new SpringApplicationBuilder(KafkaProducerApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Autowired
    KafkaProducerService kafkaProducerService;

    @Override
    public void run(String... args) throws Exception {
        this.kafkaProducerService.call();
    }

    @Bean
    NewTopic myTopic() {
        return new NewTopic("mytopic", 1, (short) 1);
    }

}

@Service
class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final Tracer tracer;

    KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, Tracer tracer) {
        this.kafkaTemplate = kafkaTemplate;
        this.tracer = tracer;
    }

    void call() throws ExecutionException, InterruptedException {
        Span span = this.tracer.nextSpan().name("kafka-producer");
        try (Tracer.SpanInScope ws = this.tracer.withSpan(span.start())) {
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", this.tracer.currentSpan().context().traceId());
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send("mytopic", "hello");
            future.handle((result, throwable) -> {
                log.info("Result <{}>, throwable <{}>", result, throwable);
                span.end();
                return CompletableFuture.completedFuture(result);
            });
            // Blocking to ensure that we push all the spans
            future.get();
        }
    }

}
