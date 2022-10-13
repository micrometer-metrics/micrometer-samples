package com.example.micrometer;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.vault.client.RestTemplateCustomizer;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

@SpringBootApplication
public class VaultRestTemplateApplication {

    private static final Logger log = LoggerFactory.getLogger(VaultRestTemplateApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(VaultRestTemplateApplication.class).web(WebApplicationType.NONE).run(args);
    }

    // This bean is just for acceptance tests - you don't need it in your code
    @Bean
    RestTemplateCustomizer myRestTemplateCustomizer(Tracer tracer) {
        return restTemplate -> restTemplate.getInterceptors().add((request, body, execution) -> {
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", tracer.currentSpan().context().traceId());
            return execution.execute(request, body);
        });
    }

    @Bean
    CommandLineRunner myCommandLineRunner(RestTemplateService restTemplateService) {
        return args -> restTemplateService.call();
    }

}

@Service
class RestTemplateService {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateService.class);

    private final VaultTemplate vaultTemplate;

    private final Tracer tracer;

    RestTemplateService(VaultTemplate vaultTemplate, Tracer tracer) {
        this.vaultTemplate = vaultTemplate;
        this.tracer = tracer;
    }

    VaultResponse call() {
        Span span = this.tracer.nextSpan().name("rest-template");
        try (Tracer.SpanInScope ws = this.tracer.withSpan(span.start())) {
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", this.tracer.currentSpan().context().traceId());
            return this.vaultTemplate.read("/secret/foo");
        }
        finally {
            span.end();
        }
    }

}
