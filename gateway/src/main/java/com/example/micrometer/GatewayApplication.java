package com.example.micrometer;

import io.micrometer.observation.Observation;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class GatewayApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GatewayApplication.class);

    public static void main(String... args) {
        new SpringApplication(GatewayApplication.class).run(args);
    }

    @Bean
    RouteLocator myRouteLocator(RouteLocatorBuilder builder, Tracer tracer,
            @Value("${url:http://localhost:7100}") String url) {
        return builder.routes().route("mvc_route",
                route -> route.path("/mvc/**").filters(f -> f.stripPrefix(1).filter((exchange, chain) -> {
                    Observation gatewayObservation = exchange.getRequiredAttribute(ServerWebExchangeUtils.GATEWAY_OBSERVATION_ATTR);
                    gatewayObservation.scoped(() -> {
                        String traceId = tracer.currentSpan().context().traceId();
                        log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", traceId);
                    });
                    return chain.filter(exchange);
                }, Ordered.LOWEST_PRECEDENCE)).uri(url)).build();
    }

    @Autowired
    Environment environment;

    @Override
    public void run(String... args) throws Exception {
        try {
            new RestTemplate().getForObject("http://localhost:" + environment.getProperty("server.port") + "/mvc/",
                    String.class);
        }
        catch (Exception exception) {
            log.error("Failed to reach the mvc application", exception);
        }
    }

}
