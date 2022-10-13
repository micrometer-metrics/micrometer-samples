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
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.vault.client.WebClientCustomizer;
import org.springframework.vault.core.ReactiveVaultTemplate;
import org.springframework.vault.support.VaultResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

@SpringBootApplication
public class VaultWebClientApplication {

    public static void main(String... args) {
        new SpringApplicationBuilder(VaultWebClientApplication.class).web(WebApplicationType.NONE).run(args);
    }

}

@Configuration
class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    @Bean
    CommandLineRunner myCommandLineRunner(WebClientService webClientService) {
        return args -> {
            webClientService.call().block(Duration.ofSeconds(5));
            // To ensure that the spans got successfully reported
            try {
                Thread.sleep(500);
            }
            catch (Exception ex) {

            }
        };
    }

    @Bean
    WebClientCustomizer testWebClientCustomizer(Tracer tracer) {
        return webClientBuilder -> webClientBuilder.filter((request, next) -> {
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", tracer.currentSpan().context().traceId());
            return next.exchange(request);
        });
    }

}

@Service
class WebClientService {

    private static final Logger log = LoggerFactory.getLogger(WebClientService.class);

    private final ReactiveVaultTemplate reactiveVaultTemplate;

    private final Tracer tracer;

    WebClientService(ReactiveVaultTemplate reactiveVaultTemplate, Tracer tracer) {
        this.reactiveVaultTemplate = reactiveVaultTemplate;
        this.tracer = tracer;
    }

    Mono<VaultResponse> call() {
        Span nextSpan = this.tracer.nextSpan().name("client");
        return Mono.just(nextSpan).doOnNext(span -> this.tracer.withSpan(span.start())).flatMap(span -> {
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", this.tracer.currentSpan().context().traceId());
            return this.reactiveVaultTemplate.read("/secrets/foo");
        }).doFinally(signalType -> nextSpan.end());
    }

}
