/*
 * Copyright 2023 VMware, Inc.
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

package com.example.micrometer;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CassandraApplication {

    private static final Logger logger = LoggerFactory.getLogger(CassandraApplication.class);

    public static void main(String... args) {
        new SpringApplicationBuilder(CassandraApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public CommandLineRunner demo(SampleRepository repository, Tracer tracer, ObservationRegistry observationRegistry) {
        return (args) -> Observation.createNotStarted("cassandra-app", observationRegistry).observe(() -> {
            logger.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", tracer.currentSpan().context().traceId());
            SampleEntity savedEntity = repository.save(new SampleEntity("test"));
            logger.info("Found entity: " + repository.findById(savedEntity.getId()));
        });
    }

    @Bean
    CqlSession cqlSession(CqlSessionBuilder cqlSessionBuilder) {
        try (CqlSession session = cqlSessionBuilder.build()) {
            session.execute(
                    "CREATE KEYSPACE IF NOT EXISTS example WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };");
        }
        return cqlSessionBuilder.withKeyspace("example").build();
    }

}
