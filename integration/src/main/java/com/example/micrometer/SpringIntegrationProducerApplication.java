package com.example.micrometer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.dsl.Files;
import org.springframework.stereotype.Component;

import java.io.File;

@SpringBootApplication
public class SpringIntegrationProducerApplication {

    public static void main(String... args) {
        new SpringApplicationBuilder(SpringIntegrationProducerApplication.class).web(WebApplicationType.NONE).run(args);
    }

}

@Component
class Runner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Runner.class);

    private final ObservationRegistry observationRegistry;

    private final Tracer tracer;

    private final FileGateway fileGateway;

    Runner(ObservationRegistry observationRegistry, Tracer tracer, FileGateway fileGateway) {
        this.observationRegistry = observationRegistry;
        this.tracer = tracer;
        this.fileGateway = fileGateway;
    }

    @Override
    public void run(String... args) {
        Observation.createNotStarted("spring.integration", observationRegistry).observe(() -> {
            String trace = tracer.currentSpan().context().traceId();
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", trace);
            this.fileGateway.placeOrder(trace);
        });
    }

}

@MessagingGateway
interface FileGateway {

    @Gateway(requestChannel = "files.input")
    void placeOrder(String text);

}

@Configuration
class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    @Bean
    public IntegrationFlow files(Tracer tracer,
            @Value("${outputFile:${java.io.tmpdir}/spring-integration-micrometer-samples/output}") File file) {
        return IntegrationFlow.from("files.input").transform(message -> {
            String traceId = tracer.currentSpan().context().traceId();
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", traceId);
            return message;
        }).handle(Files.outboundAdapter(file)).get();
    }

}
