package com.example.micrometer;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication implements CommandLineRunner {

    public static void main(String... args) {
        new SpringApplicationBuilder(ConfigServerApplication.class).run(args);
    }

    @Autowired
    WebClientService webClientService;

    @Override
    public void run(String... args) throws Exception {
        this.webClientService.call();
    }

}

@Configuration
class Config {

    // You must register RestTemplate as a bean!
    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

}

@Service
class WebClientService {

    private static final Logger log = LoggerFactory.getLogger(WebClientService.class);

    private final Environment environment;

    private final RestTemplate restTemplate;

    private final ObservationRegistry observationRegistry;

    WebClientService(Environment environment, RestTemplate restTemplate, ObservationRegistry observationRegistry) {
        this.environment = environment;
        this.restTemplate = restTemplate;
        this.observationRegistry = observationRegistry;
    }

    void call() {
        Observation.createNotStarted("hello", observationRegistry)
                .observe(() -> {
                    int port = environment.getProperty("server.port", Integer.class, 8888);
                    log.info("Got back the following response from config server \n{}",
                            this.restTemplate.getForObject("http://localhost:" + port + "/main-application.yml", String.class));
                });

    }

}
