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

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
@EnableFeignClients
public class OpenFeignApplication implements CommandLineRunner {

    public static void main(String... args) {
        new SpringApplicationBuilder(OpenFeignApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Autowired
    OpenFeignService openFeignService;

    @Override
    public void run(String... args) throws Exception {
        this.openFeignService.call();
    }

}

@Service
class OpenFeignService {

    private static final Logger log = LoggerFactory.getLogger(OpenFeignService.class);

    private final ProducerClient producerClient;

    private final Tracer tracer;

    OpenFeignService(ProducerClient producerClient, Tracer tracer) {
        this.producerClient = producerClient;
        this.tracer = tracer;
    }

    String call() {
        Span span = this.tracer.nextSpan();
        try (Tracer.SpanInScope ws = this.tracer.withSpan(span.start())) {
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", this.tracer.currentSpan().context().traceId());
            return this.producerClient.callProducer();
        }
        finally {
            span.end();
        }
    }

}

@FeignClient(url = "${url:http://localhost:7100}", name = "producer")
interface ProducerClient {

    @GetMapping
    String callProducer();

}
