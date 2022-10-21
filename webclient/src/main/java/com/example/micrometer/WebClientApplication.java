package com.example.micrometer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
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
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@SpringBootApplication
public class WebClientApplication implements CommandLineRunner {

    public static void main(String... args) {
        new SpringApplicationBuilder(WebClientApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Autowired
    WebClientService webClientService;

    @Override
    public void run(String... args) throws Exception {
        this.webClientService.call().block(Duration.ofSeconds(5));
        // To ensure that the spans got successfully reported
        Thread.sleep(500);
    }

}

@Configuration
class Config {

    // You must register WebClient as a bean!
    @Bean
    WebClient webClient(WebClient.Builder builder, @Value("${url:http://localhost:7110}") String url) {
        return builder.baseUrl(url).build();
    }

}

@Service
class WebClientService {

    private static final Logger log = LoggerFactory.getLogger(WebClientService.class);

    private final WebClient webClient;

    private final Tracer tracer;

    private final ObservationRegistry observationRegistry;

    WebClientService(WebClient webClient, Tracer tracer, ObservationRegistry observationRegistry) {
        this.webClient = webClient;
        this.tracer = tracer;
        this.observationRegistry = observationRegistry;
    }

    Mono<String> call() {
        Observation observation = Observation.start("client", observationRegistry);
        return Mono.just(observation).flatMap(span -> {
            observation.scoped(() -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer",
                    this.tracer.currentSpan().context().traceId()));
            return this.webClient.get().retrieve().bodyToMono(String.class);
        }).doFinally(signalType -> observation.stop())
                .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation));
    }

}
