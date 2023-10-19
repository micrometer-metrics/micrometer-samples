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

import io.micrometer.core.instrument.binder.okhttp3.OkHttpObservationInterceptor;
import io.micrometer.observation.ObservationRegistry;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

@SpringBootApplication
public class RetrofitApplication {

    public static void main(String... args) {
        SpringApplication.run(RetrofitApplication.class, args);
    }

    @Bean
    GreetingClient greetingClient(ObservationRegistry observationRegistry,
            @Value("${greeting.endpoint}") String greetingEndpoint) {
        return new Retrofit.Builder().baseUrl(greetingEndpoint)
            .client(new OkHttpClient.Builder()
                .addInterceptor(
                        OkHttpObservationInterceptor.builder(observationRegistry, "http.client.requests").build())
                .build())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(GreetingClient.class);
    }

}
