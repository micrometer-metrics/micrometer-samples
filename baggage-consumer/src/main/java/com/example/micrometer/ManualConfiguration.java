package com.example.micrometer;

import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.propagation.BaggageTextMapPropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * In this class we'll add all the manual configuration required for Observability to
 * work.
 */
@Configuration(proxyBeanMethods = false)
public class ManualConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(OtelCurrentTraceContext.class)
    static class OtelConfig {

        private TextMapPropagator otelRemoteFieldsBaggageTextMapPropagator(TracingProperties tracingProperties,
                OtelCurrentTraceContext otelCurrentTraceContext) {
            List<String> remoteFields = tracingProperties.getBaggage().getRemoteFields();
            return new BaggageTextMapPropagator(remoteFields,
                    new OtelBaggageManager(otelCurrentTraceContext, remoteFields, Collections.emptyList()));
        }

    }

}
