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

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.DispatcherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.observation.HttpRequestsObservationFilter;

import java.util.Collections;

@SpringBootApplication
public class MvcApplication {

    public static void main(String... args) {
        new SpringApplication(MvcApplication.class).run(args);
    }

    // You must set this manually until this is registered in Boot
    @Bean
    FilterRegistrationBean observationWebFilter(ObservationRegistry observationRegistry) {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(
                new HttpRequestsObservationFilter(observationRegistry));
        filterRegistrationBean.setDispatcherTypes(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.FORWARD,
                DispatcherType.INCLUDE, DispatcherType.REQUEST);
        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        // We provide a list of URLs that we want to create observations for
        filterRegistrationBean.setUrlPatterns(Collections.singletonList("/"));
        return filterRegistrationBean;
    }

}

@RestController
class MvcController {

    private static final Logger log = LoggerFactory.getLogger(MvcController.class);

    private final Tracer tracer;

    MvcController(Tracer tracer) {
        this.tracer = tracer;
    }

    @GetMapping("/")
    public String span() {
        String traceId = this.tracer.currentSpan().context().traceId();
        log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", traceId);
        return traceId;
    }

}
