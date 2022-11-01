package com.example.micrometer;

import com.mongodb.RequestContext;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandSucceededEvent;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import java.time.Duration;

@SpringBootApplication
public class ReactiveMongoApplication {

    private static final Logger log = LoggerFactory.getLogger(ReactiveMongoApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(ReactiveMongoApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public CommandLineRunner demo(com.example.micrometer.BasicUserRepository basicUserRepository,
            ObservationRegistry observationRegistry, Tracer tracer) {
        return (args) -> {
            Observation observation = Observation.start("mongo-reactive-app", observationRegistry);
            Mono.just(observation).flatMap(obs -> {
                obs.scoped(() -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer",
                        tracer.currentSpan().context().traceId()));
                long time = System.currentTimeMillis();
                return basicUserRepository.save(new User("foo" + time, "bar", "baz", null))
                        .flatMap(user -> basicUserRepository.findUserByUsername("foo" + time));
            }).contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation))
                    .doFinally(signalType -> observation.stop()).block(Duration.ofMinutes(1));
            log.info("Done!");
        };
    }

    // This is for tests only. You don't need this in your production code.
    @Bean
    public MongoClientSettingsBuilderCustomizer testMongoClientSettingsBuilderCustomizer(Tracer tracer,
            CurrentTraceContext currentTraceContext) {
        return new TestMongoClientSettingsBuilderCustomizer(tracer, currentTraceContext);
    }

}

// This is for tests only. You don't need this in your production code.
class TestMongoClientSettingsBuilderCustomizer implements MongoClientSettingsBuilderCustomizer {

    private static final Logger log = LoggerFactory.getLogger(TestMongoClientSettingsBuilderCustomizer.class);

    private final Tracer tracer;

    private final CurrentTraceContext currentTraceContext;

    public TestMongoClientSettingsBuilderCustomizer(Tracer tracer, CurrentTraceContext currentTraceContext) {
        this.tracer = tracer;
        this.currentTraceContext = currentTraceContext;
    }

    @Override
    public void customize(com.mongodb.MongoClientSettings.Builder clientSettingsBuilder) {
        clientSettingsBuilder.addCommandListener(new CommandListener() {

            @Override
            public void commandSucceeded(CommandSucceededEvent event) {
                if (!event.getCommandName().equals("insert")) {
                    return;
                }
                RequestContext requestContext = event.getRequestContext();
                if (requestContext == null) {
                    return;
                }
                Observation parent = requestContext.getOrDefault(ObservationThreadLocalAccessor.KEY, null);
                if (parent == null) {
                    return;
                }
                parent.scoped(() -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer",
                        tracer.currentSpan().context().traceId()));
            }
        });
    }

}
