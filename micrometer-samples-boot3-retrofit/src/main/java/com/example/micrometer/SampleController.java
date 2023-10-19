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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
public class SampleController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleController.class);

    private final GreetingClient client;

    public SampleController(GreetingClient client) {
        this.client = client;
    }

    @GetMapping("/greet/{name}")
    Map<String, String> greet(@PathVariable String name) throws IOException {
        LOGGER.info("<TEST_MARKER> Greeting {}...", name);
        return Map.of("greeting", client.getGreeting(name).execute().body());
    }

    @GetMapping("/greeting/{name}")
    String greeting(@PathVariable String name) {
        return "Hi %s!".formatted(name);
    }

}
