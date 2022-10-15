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
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.observation.HttpRequestsObservationFilter;

import static jakarta.servlet.DispatcherType.*;
import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

@SpringBootApplication
@ImportRuntimeHints(PrometheusAndZipkinWithBraveAndDatabaseSample.ResourceHints.class)
public class PrometheusAndZipkinWithBraveAndDatabaseSample {

    public static void main(String[] args) {
        SpringApplication.run(PrometheusAndZipkinWithBraveAndDatabaseSample.class, args);
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

    @Bean
    ObservationHandler<Observation.Context> errorHandler() {
        return new ObservationHandler<Observation.Context>() {
            private static final Logger LOGGER = LoggerFactory.getLogger("errorHandler");

            @Override
            public void onError(Observation.Context context) {
                LOGGER.error("Ooops!", context.getError());
            }

            @Override
            public boolean supportsContext(Observation.Context context) {
                return true;
            }
        };
    }

    static class ResourceHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.resources().registerResource(new ClassPathResource("itest-data.sql"));
            hints.resources().registerResource(new ClassPathResource("itest-schema.sql"));
        }

    }

}
