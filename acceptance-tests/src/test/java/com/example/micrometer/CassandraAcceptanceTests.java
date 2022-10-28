package com.example.micrometer;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.CassandraContainer;
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
class CassandraAcceptanceTests extends AcceptanceTestsBase {

    @Container
    static CassandraContainer cassandra = new CassandraContainer("cassandra:3.11.2");

    @BeforeAll
    static void setup() {
        Cluster cluster = cassandra.getCluster();
        try (Session session = cluster.connect()) {
            session.execute("CREATE KEYSPACE IF NOT EXISTS example WITH replication = \n"
                    + "{'class':'SimpleStrategy','replication_factor':'1'};");
        }
    }

    @Test
    void should_pass_tracing_context_with_cassandra(TestInfo testInfo) {
        // when
        String producerId = deploy(testInfo, "cassandra", port());

        // then
        assertThatLogsContainPropagatedIdAtLeastXNumberOfTimes(producerId, "cassandra", 7);
    }

    @Test
    void should_pass_tracing_context_with_cassandra_reactive(TestInfo testInfo) {
        // when
        String producerId = deploy(testInfo, "cassandra-reactive", port());

        // then
        assertThatLogsContainPropagatedIdAtLeastXNumberOfTimes(producerId, "cassandra-reactive", 1);
    }

    private Map<String, String> port() {
        return Map.of("spring.cassandra.contact-points",
                cassandra.getContainerIpAddress() + ":" + cassandra.getFirstMappedPort());
    }

}
