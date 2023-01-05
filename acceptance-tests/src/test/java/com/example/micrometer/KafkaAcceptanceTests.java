package com.example.micrometer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

// Uncomment the properties to rebuild the projects
// @formatter:off
@SpringBootTest(
//		properties = {"io.micrometer.samples.rebuild-projects=true", "io.micrometer.samples.project-root=${user.home}/repo/micrometer-samples"}
)
@Testcontainers
// @formatter:on
class KafkaAcceptanceTests extends AcceptanceTestsBase {

    @Container
    static KafkaContainer broker = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka").withTag("5.4.3"));

    @Test
    void should_pass_tracing_context_from_kafka_producer_to_consumer(TestInfo testInfo) throws Exception {
        // given
        String consumerId = wait10seconds(() -> deploy(testInfo, "kafka-consumer", brokerSetup()));

        // when
        String producerId = deploy(testInfo, "kafka-producer", brokerSetup());

        // then
        assertThatTraceIdGotPropagated(producerId, consumerId);
    }

    // @Test
    // void
    // should_pass_tracing_context_from_kafka_reactive_producer_to_reactive_consumer(TestInfo
    // testInfo)
    // throws Exception {
    // // given
    // String consumerId = wait10seconds(() -> deploy(testInfo, "kafka-reactive-consumer",
    // brokerSetup()));
    //
    // // when
    // String producerId = deploy(testInfo, "kafka-reactive-producer", brokerSetup());
    //
    // // then
    // assertThatTraceIdGotPropagated(producerId, consumerId);
    // }

    private Map<String, String> brokerSetup() {
        return Map.of("spring.kafka.bootstrap-servers", broker.getBootstrapServers());
    }

}
