package com.example.micrometer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

// Uncomment the properties to rebuild the projects
// @formatter:off
@SpringBootTest(
//		properties = {"io.micrometer.samples.rebuild-projects=true", "io.micrometer.samples.project-root=${user.home}/repo/micrometer-samples"}
)
@Testcontainers
// @formatter:on
class RabbitAcceptanceTests extends AcceptanceTestsBase {

    @Container
    static RabbitMQContainer broker = new RabbitMQContainer("rabbitmq:3.7.25-management-alpine");

    @Test
    void should_pass_tracing_context_from_stream_producer_to_consumer(TestInfo testInfo) throws Exception {
        // given
        String consumerId = wait10seconds(() -> deploy(testInfo, "stream-consumer", brokerSetup()));

        // when
        String producerId = deploy(testInfo, "stream-producer", brokerSetup());

        // then
        assertThatTraceIdGotPropagated(producerId, consumerId);
    }

    @Test
    void should_pass_tracing_context_from_stream_reactive_producer_to_reactive_consumer(TestInfo testInfo)
            throws Exception {
        // given
        String consumerId = wait10seconds(() -> deploy(testInfo, "stream-reactive-consumer", brokerSetup()));

        // when
        String producerId = deploy(testInfo, "stream-reactive-producer", brokerSetup());

        // then
        assertThatTraceIdGotPropagated(producerId, consumerId);
    }

    @Test
    void should_pass_tracing_context_with_bus(TestInfo testInfo) {
        // when
        String producerId = deploy(testInfo, "bus", brokerSetup());

        // then
        assertThatTraceIdGotPropagated(producerId);
    }

    private Map<String, String> brokerSetup() {
        return Map.of("spring.rabbitmq.port", broker.getAmqpPort().toString());
    }

}
