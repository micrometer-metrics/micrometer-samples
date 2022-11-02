package com.example.tests;

import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.tracing.brave.bridge.BraveFinishedSpan;
import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.otel.bridge.ArrayListSpanProcessor;
import io.micrometer.tracing.otel.bridge.OtelFinishedSpan;
import io.opentelemetry.api.trace.Tracer;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
public class MicrometerSamplesObservabilityTestAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    static class ObservabilityDumpingConfig {

        @Autowired
        MeterRegistry meterRegistry;

        @Autowired
        ToFinishedSpans toFinishedSpans;

        @Value("${spring.application.name}")
        String appName;

        @Value("${metrics.output.file:build/metrics.csv}")
        File metricsOutput;

        @Value("${metrics.output.file:build/spans.csv}")
        File spansOutput;

        @PreDestroy
        void dumpObservabilityData() throws IOException {
            storeMetricsAsFile();
            storeSpansAsFile();
        }

        void storeMetricsAsFile() throws IOException {
            List<Meter> meters = this.meterRegistry.getMeters();
            storeAsFile(this.metricsOutput, NameAndTags.fromMetrics(meters));
        }

        void storeSpansAsFile() throws IOException {
            List<FinishedSpan> spans = this.toFinishedSpans.getFinishedSpans();
            storeAsFile(this.spansOutput, NameAndTags.fromSpans(spans));
        }

        private void storeAsFile(File output, List<NameAndTags> nameAndTags) throws IOException {
            String lines = nameAndTags.stream()
                    .map(nt -> this.appName + ";" + nt.name + ";" + nt.tags.entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(",")))
                    .collect(Collectors.joining("\n"));

            Files.writeString(output.toPath(), lines);
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Tracer.class)
    static class TestOtelConfig {

        @Bean
        ArrayListSpanProcessor otelTestSpans() {
            return new ArrayListSpanProcessor();
        }

        @Bean
        ToFinishedSpans otelToFinishedSpans(ArrayListSpanProcessor arrayListSpanProcessor) {
            return () -> arrayListSpanProcessor.spans().stream().map(OtelFinishedSpan::fromOtel).toList();
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(brave.Tracer.class)
    static class BraveConfig {

        @Bean
        TestSpanHandler braveTestSpans() {
            return new TestSpanHandler();
        }

        @Bean
        ToFinishedSpans braveToFinishedSpans(TestSpanHandler testSpanHandler) {
            return () -> testSpanHandler.spans().stream().map(BraveFinishedSpan::fromBrave).toList();
        }

    }

    interface ToFinishedSpans {

        List<FinishedSpan> getFinishedSpans();

    }

    @SuppressWarnings("unchecked")
    static class TestSpanHandler extends SpanHandler implements Iterable<MutableSpan> {

        final List<MutableSpan> spans = new ArrayList();

        public List<MutableSpan> spans() {
            synchronized (this.spans) {
                return new ArrayList(this.spans);
            }
        }

        public boolean end(TraceContext context, MutableSpan span, SpanHandler.Cause cause) {
            synchronized (this.spans) {
                this.spans.add(span);
                return true;
            }
        }

        public Iterator<MutableSpan> iterator() {
            return this.spans().iterator();
        }

        public String toString() {
            return "TestSpanHandler{" + this.spans() + "}";
        }

    }

    static class NameAndTags {

        private final String name;

        private final Map<String, String> tags;

        NameAndTags(String name, Map<String, String> tags) {
            this.name = name;
            this.tags = tags;
        }

        static List<NameAndTags> fromMetrics(List<Meter> meters) {
            return meters.stream()
                    .map(meter -> new NameAndTags(meter.getId().getName(),
                            meter.getId().getTags().stream().collect(Collectors.toMap(Tag::getKey, Tag::getValue))))
                    .toList();
        }

        static List<NameAndTags> fromSpans(List<FinishedSpan> spans) {
            return spans.stream().map(span -> new NameAndTags(span.getName(), span.getTags())).toList();
        }

    }

}
