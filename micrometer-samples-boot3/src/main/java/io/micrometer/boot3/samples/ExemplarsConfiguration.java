package io.micrometer.boot3.samples;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.prometheus.client.exemplars.tracer.common.SpanContextSupplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
@AutoConfigureBefore(PrometheusMetricsExportAutoConfiguration.class)
@ConditionalOnClass(SpanContextSupplier.class)
public class ExemplarsConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SpanContextSupplier spanContextSupplier(ObjectProvider<Tracer> tracerProvider) {
        return new LazyTracingSpanContextSupplier(tracerProvider);
    }

    /**
     * Since the MeterRegistry can depend on the {@link Tracer} (Exemplars) and the {@link Tracer} can depend on the MeterRegistry (recording metrics),
     * this {@link SpanContextSupplier} breaks the circle by lazily loading the {@link Tracer}.
     */
    static class LazyTracingSpanContextSupplier implements SpanContextSupplier, SmartInitializingSingleton {
        private final ObjectProvider<Tracer> tracerProvider;

        private Tracer tracer;

        LazyTracingSpanContextSupplier(ObjectProvider<Tracer> tracerProvider) {
            this.tracerProvider = tracerProvider;
        }

        @Override
        public String getTraceId() {
            return tracer.currentSpan().context().traceId();
        }

        @Override
        public String getSpanId() {
            return tracer.currentSpan().context().spanId();
        }

        @Override
        public boolean isSampled() {
            return tracer != null && isSampled(tracer);
        }

        private boolean isSampled(@NonNull Tracer tracer) {
            Span currentSpan = tracer.currentSpan();
            return currentSpan != null && currentSpan.context().sampled();
        }

        @Override
        public void afterSingletonsInstantiated() {
            this.tracer = tracerProvider.getIfAvailable();
        }
    }

}
