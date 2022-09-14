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
package io.micrometer.boot3.samples.controller;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SampleController {

    private final List<String> PEOPLE = Arrays.asList("suzy", "mike");

    private final ObservationRegistry registry;

    private final Tracer tracer;

    SampleController(ObservationRegistry registry, Tracer tracer) {
        this.registry = registry;
        this.tracer = tracer;
    }

    @GetMapping("/")
    List<String> allPeople() {
        return Observation.createNotStarted("allPeople", registry).observe(slowDown(() -> PEOPLE));
    }

    @GetMapping("/greet/{name}")
    String greet(@PathVariable String name) {
        Observation observation = Observation.createNotStarted("greeting", registry).start();
        try (Observation.Scope scope = observation.openScope()) {
            if (PEOPLE.contains(name)) {
                // only 2 names are valid (low cardinality)
                observation.lowCardinalityKeyValue("greeting.name", name);
                observation.event(Observation.Event.of("greeted"));
                return fetchDataSlowly(() -> String.format("Hello %s!", name));
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
            throw exception;
        }
        finally {
            observation.stop();
        }
    }

    private <T> Supplier<T> slowDown(Supplier<T> supplier) {
        return () -> {
            try {
                if (Math.random() < 0.1) { // larger latency, less frequent
                    Thread.sleep(500);
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
