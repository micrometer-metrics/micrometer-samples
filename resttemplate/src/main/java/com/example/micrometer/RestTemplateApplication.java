package com.example.micrometer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class RestTemplateApplication implements CommandLineRunner {

    public static void main(String... args) {
        new SpringApplicationBuilder(RestTemplateApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Autowired
    RestTemplateService restTemplateService;

    @Value("${url:http://localhost:7100}")
    String url;

    @Override
    public void run(String... args) throws Exception {
        this.restTemplateService.call(url);
    }

}

@Configuration
class Config {

    // You must register RestTemplate as a bean!
    // We're using username and password in case you need basic authentication
    @Bean
    RestTemplate restTemplate(@Value("${username:user}") String username,
            @Value("${password:password}") String password, RestTemplateBuilder builder) {
        return builder.basicAuthentication(username, password).build();
    }

}

@Service
class RestTemplateService {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateService.class);

    private final RestTemplate restTemplate;

    private final Tracer tracer;

    private final ObservationRegistry observationRegistry;

    RestTemplateService(RestTemplate restTemplate, Tracer tracer, ObservationRegistry observationRegistry) {
        this.restTemplate = restTemplate;
        this.tracer = tracer;
        this.observationRegistry = observationRegistry;
    }

    String call(String url) {
        return Observation.start("rest-template-sample", observationRegistry).observe(() -> {
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", this.tracer.currentSpan().context().traceId());
            return this.restTemplate.getForObject(url, String.class);
        });
    }

}
