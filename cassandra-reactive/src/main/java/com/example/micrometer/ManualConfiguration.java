package com.example.micrometer;

import com.datastax.oss.driver.api.core.CqlSession;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.data.cassandra.ReactiveSession;
import org.springframework.data.cassandra.config.SessionBuilderConfigurer;
import org.springframework.data.cassandra.observability.ObservableCqlSessionFactory;
import org.springframework.data.cassandra.observability.ObservableReactiveSessionFactory;
import org.springframework.data.cassandra.observability.ObservationRequestTracker;

public class ManualConfiguration {

    @Bean
    public ObservationBeanPostProcessor observationBeanPostProcessor(ObservationRegistry observationRegistry) {
        return new ObservationBeanPostProcessor(observationRegistry);
    }

    @Bean
    public SessionBuilderConfigurer getSessionBuilderConfigurer() {
        return sessionBuilder -> sessionBuilder.addRequestTracker(ObservationRequestTracker.INSTANCE);
    }

    class ObservationBeanPostProcessor implements BeanPostProcessor {

        public final ObservationRegistry observationRegistry;

        public ObservationBeanPostProcessor(ObservationRegistry observationRegistry) {
            this.observationRegistry = observationRegistry;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

            if (bean instanceof CqlSession) {
                return ObservableCqlSessionFactory.wrap((CqlSession) bean, observationRegistry);
            }

            if (bean instanceof ReactiveSession) {
                return ObservableReactiveSessionFactory.wrap((ReactiveSession) bean, observationRegistry);
            }

            return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
        }
    }
}
