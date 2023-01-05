package com.example.micrometer;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
// @AutoConfigureObservability
@Disabled
class ReactiveCassandraApplicationTests {

    @Container
    static CassandraContainer cassandra = new CassandraContainer("cassandra:3.11.2");

    @DynamicPropertySource
    static void setup(DynamicPropertyRegistry registry) {
        registry.add("spring.cassandra.contact-points",
                () -> cassandra.getContainerIpAddress() + ":" + cassandra.getFirstMappedPort());
        Cluster cluster = cassandra.getCluster();
        try (Session session = cluster.connect()) {
            session.execute("CREATE KEYSPACE IF NOT EXISTS example WITH replication = \n"
                    + "{'class':'SimpleStrategy','replication_factor':'1'};");
        }
    }

    @Test
    void should_work() {

    }

}
