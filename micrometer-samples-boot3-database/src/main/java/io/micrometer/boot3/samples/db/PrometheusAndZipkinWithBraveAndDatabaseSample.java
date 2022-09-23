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

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class PrometheusAndZipkinWithBraveAndDatabaseSample {

    public static void main(String[] args) {
        new SpringApplicationBuilder(PrometheusAndZipkinWithBraveAndDatabaseSample.class)
                // TODO: Until we remove tracing from Boot
                .properties("spring.main.allow-bean-definition-overriding=true").build().run(args);
    }

}
