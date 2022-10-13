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
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;

@SpringBootApplication
public class RsocketClient implements CommandLineRunner {

    public static void main(String... args) {
        new SpringApplicationBuilder(RsocketClient.class).web(WebApplicationType.NONE).run(args);
    }

    @Autowired
    RsocketService rsocketService;

    @Override
    public void run(String... args) throws Exception {
        this.rsocketService.call().block(Duration.ofSeconds(5));
        // To ensure that the spans got successfully reported
        Thread.sleep(500);
    }

}

@Configuration
class Config {

    // You must register RSocketRequester as a bean!
    @Bean
    RSocketRequester myRSocketRequester(@Value("${url:ws://localhost:7112/rsocket}") String url,
            RSocketRequester.Builder builder) {
        return builder.websocket(URI.create(url));
    }

}

@Service
class RsocketService {

    private static final Logger log = LoggerFactory.getLogger(RsocketService.class);

    private final RSocketRequester rSocketRequester;

    private final Tracer tracer;

    RsocketService(RSocketRequester rSocketRequester, Tracer tracer) {
        this.rSocketRequester = rSocketRequester;
        this.tracer = tracer;
    }

    Mono<String> call() {
        Span nextSpan = this.tracer.nextSpan().name("client");
        return Mono.just(nextSpan).doOnNext(span -> this.tracer.withSpan(span.start())).flatMap(span -> {
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", this.tracer.currentSpan().context().traceId());
            return this.rSocketRequester.route("foo").retrieveMono(String.class);
        }).doFinally(signalType -> nextSpan.end());
    }

}
