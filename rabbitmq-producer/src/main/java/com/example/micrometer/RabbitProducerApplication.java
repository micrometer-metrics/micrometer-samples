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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class RabbitProducerApplication implements CommandLineRunner {

    static final String EXCHANGE_NAME = "test.exchange";

    private static final String QUEUE_NAME = "test.queue";

    public static void main(String... args) {
        new SpringApplicationBuilder(RabbitProducerApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Autowired
    MyRabbitProducer myRabbitProducer;

    @Override
    public void run(String... args) throws Exception {
        myRabbitProducer.call();
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
class MyRabbitProducer {

    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);

    private final Tracer tracer;

    private final ObservationRegistry observationRegistry;

    private final RabbitTemplate rabbitTemplate;

    MyRabbitProducer(RabbitTemplate rabbitTemplate, Tracer tracer, ObservationRegistry observationRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.tracer = tracer;
        this.observationRegistry = observationRegistry;
    }

    public void call() {
        Observation.createNotStarted("rabbit-producer", this.observationRegistry).observe(() -> {
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", this.tracer.currentSpan().context().traceId());
            rabbitTemplate.convertAndSend(RabbitProducerApplication.EXCHANGE_NAME, "",
                    "Sample message using amqp template");
        });
    }

}
