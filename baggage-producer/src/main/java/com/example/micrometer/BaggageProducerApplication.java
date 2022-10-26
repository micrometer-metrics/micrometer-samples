package com.example.micrometer;

import io.micrometer.tracing.BaggageInScope;
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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@SpringBootApplication
public class BaggageProducerApplication implements CommandLineRunner {

    public static void main(String... args) {
        new SpringApplicationBuilder(BaggageProducerApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Autowired
    BaggageRestTemplateService baggageRestTemplateService;

    @Value("${url:http://localhost:7200}")
    String url;

    @Override
    public void run(String... args) throws Exception {
        this.baggageRestTemplateService.call(url);
    }

}

@Configuration
class Config {

    // You must register RestTemplate as a bean!
    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // Test code to print out all headers - you don't need this code
                .additionalInterceptors((request, body, execution) -> {
                    request.getHeaders()
                            .forEach((s, strings) -> System.out.println("HEADER [" + s + "] VALUE " + strings));
                    return execution.execute(request, body);
                }).build();
    }

}

@Service
class BaggageRestTemplateService {

    private static final Logger log = LoggerFactory.getLogger(BaggageRestTemplateService.class);

    private final RestTemplate restTemplate;

    private final Tracer tracer;

    BaggageRestTemplateService(RestTemplate restTemplate, Tracer tracer) {
        this.restTemplate = restTemplate;
        this.tracer = tracer;
    }

    String call(String url) {
        Span span = this.tracer.nextSpan();
        try (Tracer.SpanInScope ws = this.tracer.withSpan(span.start())) {
            try (BaggageInScope baggage = this.tracer.createBaggage("mybaggage", "my-baggage-value")) {
                log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer",
                        this.tracer.currentSpan().context().traceId());
                log.info("<BAGGAGE VALUE: {}> Baggage is set", baggage.get());
                return this.restTemplate.exchange(
                        RequestEntity.get(URI.create(url)).header("myremotefield", "my-remote-field-value").build(),
                        String.class).getBody();
            }
        }
        finally {
            span.end();
        }
    }

}
