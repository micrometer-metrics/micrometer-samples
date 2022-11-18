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
package io.micrometer.boot3.samples.db;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@RestController
class SampleController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleController.class);

    private final ObservationRegistry registry;

    private final Tracer tracer;

    private final JdbcTemplate jdbcTemplate;

    SampleController(ObservationRegistry registry, Tracer tracer, JdbcTemplate jdbcTemplate) {
        this.registry = registry;
        this.tracer = tracer;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/")
    public String span() {
        String traceId = this.tracer.currentSpan().context().traceId();
        LOGGER.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", traceId);
        return traceId;
    }

    @GetMapping("/trouble")
    String trouble() {
        LOGGER.info("<TEST_MARKER> 3,2,1... Boom!");
        throw new IllegalStateException("Noooooo!");
    }

    @GetMapping("/people")
    List<String> allPeople() {
        return Observation.createNotStarted("allPeople", registry).observe(slowDown(() -> jdbcTemplate
                .queryForList("SELECT * FROM emp").stream().map(map -> map.get("name").toString()).toList()));
    }

    @GetMapping("/greet/{name}")
    Map<String, String> greet(@PathVariable String name) {
        Observation observation = Observation.createNotStarted("greeting", registry).start();
        try (Observation.Scope scope = observation.openScope()) {
            if (foundByName(name)) {
                // only 2 names are valid (low cardinality)
                observation.lowCardinalityKeyValue("greeting.name", name);
                observation.event(Observation.Event.of("greeted"));
                return fetchDataSlowly(() -> Map.of("greeted", name));
            }
            else {
                observation.lowCardinalityKeyValue("greeting.name", "N/A");
                observation.highCardinalityKeyValue("greeting.name", name);
                observation.event(Observation.Event.of("failed"));
                throw new IllegalArgumentException("Invalid name!");
            }
        }
        catch (Exception exception) {
            observation.error(exception);
            return Map.of();
        }
        finally {
            observation.stop();
        }
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleIllegalState(IllegalStateException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setTitle(exception.getMessage());

        return problemDetail;
    }

    private boolean foundByName(String name) {
        return jdbcTemplate.queryForObject("SELECT count(name) FROM emp where name=?", Integer.class, name) > 0;
    }

    private <T> Supplier<T> slowDown(Supplier<T> supplier) {
        return () -> {
            try {
                LOGGER.info("<TEST_MARKER> Fetching the data");
                if (Math.random() < 0.02) { // huge latency, less frequent
                    Thread.sleep(1_000);
                }
                Thread.sleep(((int) (Math.random() * 100)) + 100); // +base latency
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            return supplier.get();
        };
    }

    private <T> T fetchDataSlowly(Supplier<T> supplier) {
        return slowDown(supplier).get();
    }

}
