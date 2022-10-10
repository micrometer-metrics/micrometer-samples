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
package io.micrometer.boot3.samples;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.observation.HttpRequestsObservationFilter;

import static jakarta.servlet.DispatcherType.*;
import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

@SpringBootApplication
public class PrometheusAndZipkinWithBraveSample {

    public static void main(String[] args) {
        SpringApplication.run(PrometheusAndZipkinWithBraveSample.class, args);
    }

    // TODO: remove after Boot auto-configuration is added
    @Bean
    FilterRegistrationBean<HttpRequestsObservationFilter> traceWebFilter(ObservationRegistry observationRegistry) {
        var filterRegistrationBean = new FilterRegistrationBean<>(
                new HttpRequestsObservationFilter(observationRegistry));
        filterRegistrationBean.setDispatcherTypes(ASYNC, ERROR, FORWARD, INCLUDE, REQUEST);
        filterRegistrationBean.setOrder(LOWEST_PRECEDENCE);

        return filterRegistrationBean;
    }

}
