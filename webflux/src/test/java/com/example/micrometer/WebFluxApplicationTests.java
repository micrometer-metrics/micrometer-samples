package com.example.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingObservationHandler;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.BDDAssertions.then;

@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebFluxApplicationTests {

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry meterRegistry;

    @Autowired
    ObservationRegistry registry;

    @Autowired
    Tracer tracer;

    @Test
    void should_record_metrics() {
        String response = new RestTemplate().getForObject("http://localhost:" + port + "/", String.class);

        then(response).isNotBlank();
        MeterRegistryAssert.then(meterRegistry).hasTimerWithNameAndTagKeys("http.server.requests", "error", "exception",
                "method", "outcome", "status", "uri");
    }

    @Test
    void should_propagate_tracing_context() {
        Observation parent = Observation.start("parent", registry);

        parent.scoped(() -> {
            then(registry.getCurrentObservation()).isSameAs(parent);
            then(tracer.currentSpan().context()).isEqualTo(spanContextFromObservation(parent));

            Observation mvc = Observation.createNotStarted("mvc", registry).parentObservation(parent).start();

            Observation child = Observation.createNotStarted("child", registry).parentObservation(mvc).start();

            child.scoped(() -> {
                then(registry.getCurrentObservation()).isSameAs(child);
                then(tracer.currentSpan().context()).isEqualTo(spanContextFromObservation(child));
            });

            child.stop();

            then(registry.getCurrentObservation()).isSameAs(parent);
            then(tracer.currentSpan().context()).isEqualTo(spanContextFromObservation(parent));

            mvc.stop();
        });

        parent.stop();

        then(registry.getCurrentObservation()).isNull();
        then(tracer.currentSpan()).isNull();
    }

    private TraceContext spanContextFromObservation(Observation observation) {
        TracingObservationHandler.TracingContext tracingContext = observation.getContextView().getOrDefault(
                TracingObservationHandler.TracingContext.class, new TracingObservationHandler.TracingContext());
        return tracingContext.getSpan() != null ? tracingContext.getSpan().context() : null;
    }

}
