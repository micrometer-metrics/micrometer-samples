package com.example.micrometer;

import java.util.function.Consumer;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class StreamConsumerApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StreamConsumerApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(StreamConsumerApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Override
    public void run(String... args) throws Exception {

    }

    @Bean
    Consumer<String> channel(Tracer tracer) {
        return string -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer",
                tracer.currentSpan().context().traceId());
    }

}
