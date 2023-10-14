package com.example.micrometer;

import io.micrometer.core.instrument.binder.okhttp3.OkHttpObservationInterceptor;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.GET;

import java.io.IOException;

@SpringBootApplication
public class RetrofitApplication implements CommandLineRunner {

    public static void main(String... args) {
        new SpringApplicationBuilder(RetrofitApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Autowired
    RetrofitService retrofitService;

    @Override
    public void run(String... args) throws Exception {
        this.retrofitService.call();
    }

}

@Configuration
class Config {

    @Bean
    RetrofitClient retrofitClient(ObservationRegistry observationRegistry, Tracer tracer,
            @Value("${url:http://localhost:7100}") String url) {

        Retrofit retrofit = new Retrofit.Builder().baseUrl(url)
            .client(new OkHttpClient.Builder()
                .addInterceptor(OkHttpObservationInterceptor.builder(observationRegistry, "okhttp").build())
                .build())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build();

        return retrofit.create(RetrofitClient.class);
    }

}

@Service
class RetrofitService {

    private static final Logger log = LoggerFactory.getLogger(RetrofitService.class);

    private final RetrofitClient retrofitClient;

    private final Tracer tracer;

    RetrofitService(RetrofitClient retrofitClient, Tracer tracer) {
        this.retrofitClient = retrofitClient;
        this.tracer = tracer;
    }

    String call() throws IOException {
        Span span = this.tracer.nextSpan();
        try (Tracer.SpanInScope ws = this.tracer.withSpan(span.start())) {
            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", this.tracer.currentSpan().context().traceId());
            Call<String> retrofitCall = retrofitClient.get();
            Response<String> response = retrofitCall.execute();
            return response.body();
        }
        finally {
            span.end();
        }
    }

}

interface RetrofitClient {

    @GET("/")
    Call<String> get();

}
