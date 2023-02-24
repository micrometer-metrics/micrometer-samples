package com.example.micrometer;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import io.grpc.testing.protobuf.SimpleServiceGrpc.SimpleServiceBlockingStub;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcClientInterceptor;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GrpcClientApplication {

    @Value("${url:localhost:9090}")
    String url;

    private static final Logger log = LoggerFactory.getLogger(GrpcClientApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(GrpcClientApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public ObservationGrpcClientInterceptor interceptor(ObservationRegistry observationRegistry) {
        return new ObservationGrpcClientInterceptor(observationRegistry);
    }

    @Bean
    SimpleServiceBlockingStub client(ObservationGrpcClientInterceptor interceptor) {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(url).usePlaintext().intercept(interceptor).build();
        return SimpleServiceGrpc.newBlockingStub(channel);
    }

    @Bean
    CommandLineRunner runner(SimpleServiceBlockingStub blockingStub, ObservationRegistry observationRegistry,
            Tracer tracer) {
        return (args) -> {
            SimpleRequest request = SimpleRequest.newBuilder().setRequestMessage("Hello").build();
            Observation.createNotStarted("grpc.client", observationRegistry).observe(() -> {
                log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", tracer.currentSpan().context().traceId());
                SimpleResponse response = blockingStub.unaryRpc(request);
                log.info("Greeting: " + response.getResponseMessage());
            });
        };
    }

}
