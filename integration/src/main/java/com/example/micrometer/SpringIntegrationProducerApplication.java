package com.example.micrometer;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.io.File;

@SpringBootApplication
public class SpringIntegrationProducerApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringIntegrationProducerApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(SpringIntegrationProducerApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Autowired
    FileGateway fileGateway;

    @Autowired
    Tracer tracer;

    @Override
    public void run(String... args) throws Exception {
        Span span = this.tracer.nextSpan();
        try (Tracer.SpanInScope ws = this.tracer.withSpan(span.start())) {
            String trace = span.context().traceId();
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", trace);
            this.fileGateway.placeOrder(trace);
        }
        finally {
            span.end();
        }
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
            @Value("${outputFile:${java.io.tmpdir}/spring-integration-sleuth-samples/output}") File file) {
        return IntegrationFlow.from("files.input").transform(message -> {
            String traceId = tracer.currentSpan().context().traceId();
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", traceId);
            return message;
        }).handle(Files.outboundAdapter(file)).get();
    }

}
