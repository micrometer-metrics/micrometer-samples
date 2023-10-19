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
    GreetingClient greetingClient(
        ObservationRegistry observationRegistry,
        @Value("${greeting.endpoint}") String greetingEndpoint) {
        return new Retrofit.Builder().baseUrl(greetingEndpoint)
            .client(new OkHttpClient.Builder()
                .addInterceptor(OkHttpObservationInterceptor.builder(observationRegistry, "http.client.requests").build())
                .build()
            )
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(GreetingClient.class);
    }

}
