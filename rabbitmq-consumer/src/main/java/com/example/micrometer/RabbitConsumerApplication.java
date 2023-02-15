package com.example.micrometer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class RabbitConsumerApplication implements CommandLineRunner {

    private static final String EXCHANGE_NAME = "fanout.ex";

    private static final String QUEUE_NAME = "queue.ex";

    public static void main(String... args) {
        new SpringApplicationBuilder(RabbitConsumerApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Override
    public void run(String... args) throws Exception {

    }

    @Bean
    Queue createQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    Exchange fanoutExchange() {
        return new FanoutExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    Binding queueBinding() {
        return new Binding(QUEUE_NAME, Binding.DestinationType.QUEUE, EXCHANGE_NAME, "", null);
    }

}

@Service
class MyRabbitListener {

    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);

    private final Tracer tracer;

    private final ObservationRegistry observationRegistry;

    MyRabbitListener(Tracer tracer, ObservationRegistry observationRegistry) {
        this.tracer = tracer;
        this.observationRegistry = observationRegistry;
    }

    @RabbitListener(queues = "queue.ex")
    public void receiveMessage(String message) {
        Observation.createNotStarted("on-message", this.observationRegistry).observe(() -> {
            log.info("Got message <{}>", message);
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", this.tracer.currentSpan().context().traceId());
        });
    }

}
