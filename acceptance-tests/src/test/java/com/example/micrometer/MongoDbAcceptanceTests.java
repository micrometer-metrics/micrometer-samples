package com.example.micrometer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.MongoDBContainer;
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
class MongoDbAcceptanceTests extends AcceptanceTestsBase {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:4.4.7");

    @Test
    void should_pass_tracing_context_with_mongo(TestInfo testInfo) {
        // when
        String producerId = deploy(testInfo, "mongodb-reactive", replicaSetUri());

        // then
        assertThatTraceIdGotPropagated(producerId);
    }

    private Map<String, String> replicaSetUri() {
        return Map.of("spring.data.mongodb.uri", mongo.getReplicaSetUrl());
    }

}
