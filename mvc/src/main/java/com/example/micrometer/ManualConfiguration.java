package com.example.micrometer;

import java.util.Collections;

import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ServerHttpObservationFilter;

/**
 * In this class we'll add all the manual configuration required for
 * Observability to work.
 */
@Configuration(proxyBeanMethods = false)
public class ManualConfiguration {
    // You must set this manually until this is registered in Boot
    @Bean
    FilterRegistrationBean observationWebFilter(ObservationRegistry observationRegistry) {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(
                new ServerHttpObservationFilter(observationRegistry));
        filterRegistrationBean.setDispatcherTypes(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.FORWARD,
                DispatcherType.INCLUDE, DispatcherType.REQUEST);
        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        // We provide a list of URLs that we want to create observations for
        filterRegistrationBean.setUrlPatterns(Collections.singletonList("/"));
        return filterRegistrationBean;
    }

}
