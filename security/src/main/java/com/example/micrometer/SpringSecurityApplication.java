/*
 * Copyright 2021-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.micrometer;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextChangedListener;

/**
 * Sample app to demonstrate instrumentation of Spring Security.
 *
 * @author Jonatan Ivanov
 */
@SpringBootApplication
public class SpringSecurityApplication {

    private static final Logger log = LoggerFactory.getLogger(SpringSecurityApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringSecurityApplication.class, args);
    }

    // This only needed for tests - you don't need this bean
    @Bean
    SecurityContextChangedListener testSecurityContextChangedListener(BeanFactory beanFactory) {
        return event -> {
            Tracer tracer = beanFactory.getBean(Tracer.class);
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", tracer.currentSpan().context().traceId());
        };
    }

}
