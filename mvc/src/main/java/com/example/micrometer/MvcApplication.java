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
package com.example.micrometer;

import java.util.Map;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class MvcApplication {

    public static void main(String... args) {
        new SpringApplication(MvcApplication.class).run(args);
    }

}

@RestController
class MvcController {

    private static final Logger log = LoggerFactory.getLogger(MvcController.class);

    private final Tracer tracer;

    MvcController(Tracer tracer) {
        this.tracer = tracer;
    }

    // TODO: Uncomment this once Mvc gets instrumented in Framework
    // @GetMapping("/")
    // public String span() {
    // String traceId = this.tracer.currentSpan().context().traceId();
    // log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", traceId);
    // return traceId;
    // }

    @GetMapping("/")
    public String span(@RequestHeader Map<String, String> headers) {
        String traceId = headers.get("traceparent");
        Assert.notNull(traceId, "traceparent must not be null");
        log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", traceId.split("-")[1]);
        return traceId;
    }

}
