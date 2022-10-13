package com.example.micrometer;

import java.time.Duration;

import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import com.mongodb.RequestContext;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandSucceededEvent;

import reactor.core.publisher.Mono;

@SpringBootApplication
public class ReactiveMongoApplication {

    private static final Logger log = LoggerFactory.getLogger(ReactiveMongoApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(ReactiveMongoApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public CommandLineRunner demo(com.example.micrometer.BasicUserRepository basicUserRepository, Tracer tracer) {
        return (args) -> {
            Span nextSpan = tracer.nextSpan().name("mongo-reactive-app");
            Mono.just(nextSpan).doOnNext(span -> tracer.withSpan(nextSpan.start())).flatMap(span -> {
                log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", tracer.currentSpan().context().traceId());
                return basicUserRepository.save(new User("foo", "bar", "baz", null))
                        .flatMap(user -> basicUserRepository.findUserByUsername("foo"));
            }).contextWrite(context -> context.put(Span.class, nextSpan).put(TraceContext.class, nextSpan.context()))
                    .doFinally(signalType -> nextSpan.end()).block(Duration.ofMinutes(1));
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
                RequestContext requestContext = event.getRequestContext();
                if (requestContext == null) {
                    return;
                }
                Span parent = spanFromContext(tracer, currentTraceContext, requestContext);
                if (parent == null) {
                    return;
                }
                try (Tracer.SpanInScope withSpan = tracer.withSpan(parent)) {
                    log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer",
                            tracer.currentSpan().context().traceId());
                }
            }
        });
    }

    private static Span spanFromContext(Tracer tracer, CurrentTraceContext currentTraceContext,
            RequestContext context) {
        Span span = context.getOrDefault(Span.class, null);
        if (span != null) {
            if (log.isDebugEnabled()) {
                log.debug("Found a span in mongo context [" + span + "]");
            }
            return span;
        }
        TraceContext traceContext = context.getOrDefault(TraceContext.class, null);
        if (traceContext != null) {
            try (CurrentTraceContext.Scope scope = currentTraceContext.maybeScope(traceContext)) {
                if (log.isDebugEnabled()) {
                    log.debug("Found a trace context in mongo context [" + traceContext + "]");
                }
                return tracer.currentSpan();
            }
        }
        return null;
    }

}
