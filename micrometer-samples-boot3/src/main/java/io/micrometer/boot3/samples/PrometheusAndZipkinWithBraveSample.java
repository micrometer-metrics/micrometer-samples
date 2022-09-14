/*
 * Copyright 2022 VMware, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.boot3.samples;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.tracing.Tracer;
import io.prometheus.client.exemplars.tracer.common.SpanContextSupplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PrometheusAndZipkinWithBraveSample {

    public static void main(String[] args) {
        SpringApplication.run(PrometheusAndZipkinWithBraveSample.class, args);
    }

    @Bean
    MeterFilter meterFilter() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().equals("greeting")) {
                    return DistributionStatisticConfig.builder().percentilesHistogram(true).build().merge(config);
                }
                else {
                    return config;
                }
            }
        };
    }

    @Bean
    SpanContextSupplier tracingSpanContextSupplier(ObjectProvider<Tracer> tracerProvider) {
        return new LazyTracingSpanContextSupplier(tracerProvider);
    }

    static class LazyTracingSpanContextSupplier implements SpanContextSupplier, SmartInitializingSingleton {

        private final ObjectProvider<Tracer> tracerProvider;

        private SpanContextSupplier delegate;

        LazyTracingSpanContextSupplier(ObjectProvider<Tracer> tracerProvider) {
            this.tracerProvider = tracerProvider;
        }

        @Override
        public String getTraceId() {
            return this.delegate.getTraceId();
        }

        @Override
        public String getSpanId() {
            return this.delegate.getSpanId();
        }

        @Override
        public boolean isSampled() {
            return this.delegate != null && this.delegate.isSampled();
        }

        @Override
        public void afterSingletonsInstantiated() {
            Tracer tracer = tracerProvider.getIfAvailable();
            if (tracer != null) {
                this.delegate = new TracingSpanContextSupplier(tracer);
            }
        }

    }

    static class TracingSpanContextSupplier implements SpanContextSupplier {

        private final Tracer tracer;

        TracingSpanContextSupplier(Tracer tracer) {
            this.tracer = tracer;
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
            return tracer.currentSpan() != null ? tracer.currentSpan().context().sampled() : false;
        }

    }

}
