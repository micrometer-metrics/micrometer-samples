package com.example.micrometer;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;

@SpringBootApplication
@EnableKafka
public class KafkaConsumerApplication implements CommandLineRunner {

    public static void main(String... args) {
        new SpringApplicationBuilder(KafkaConsumerApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Override
    public void run(String... args) throws Exception {

    }

    @Bean
    NewTopic myTopic() {
        return new NewTopic("mytopic", 1, (short) 1);
    }

    @Bean
    MyKafkaListener myKafkaListener(Tracer tracer) {
        return new MyKafkaListener(tracer);
    }

}

class MyKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(MyKafkaListener.class);

    private final Tracer tracer;

    MyKafkaListener(Tracer tracer) {
        this.tracer = tracer;
    }

    @KafkaListener(topics = "mytopic", groupId = "group")
    void onMessage(Message message) {
        Span span = this.tracer.nextSpan().name("on-message").start();
        try (Tracer.SpanInScope ws = this.tracer.withSpan(span)) {
            log.info("Got message <{}>", message.getPayload());
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", this.tracer.currentSpan().context().traceId());
        }
        finally {
            span.end();
        }
    }

}
