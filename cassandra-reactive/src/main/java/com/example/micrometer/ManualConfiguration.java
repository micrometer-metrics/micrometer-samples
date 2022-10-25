package com.example.micrometer;

import com.datastax.oss.driver.api.core.CqlSession;
import io.micrometer.observation.ObservationRegistry;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.ReactiveSession;
import org.springframework.data.cassandra.config.SessionBuilderConfigurer;
import org.springframework.data.cassandra.observability.ObservableCqlSessionFactory;
import org.springframework.data.cassandra.observability.ObservableReactiveSessionFactory;
import org.springframework.data.cassandra.observability.ObservationRequestTracker;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

@Configuration
public class ManualConfiguration {

    @Bean
    static ObservationBeanPostProcessor observationBeanPostProcessor(ObjectProvider<ObservationRegistry> observationRegistry) {
        return new ObservationBeanPostProcessor(observationRegistry);
    }

    @Bean
    public SessionBuilderConfigurer getSessionBuilderConfigurer() {
        return sessionBuilder -> sessionBuilder.addRequestTracker(ObservationRequestTracker.INSTANCE);
    }

    static class ObservationBeanPostProcessor implements BeanPostProcessor {

        public final ObjectProvider<ObservationRegistry> observationRegistry;

        public ObservationBeanPostProcessor(ObjectProvider<ObservationRegistry> observationRegistry) {
            this.observationRegistry = observationRegistry;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

            if (bean instanceof CqlSession) {
                return lazyWrap(registry -> ObservableCqlSessionFactory.wrap((CqlSession) bean, registry), bean, observationRegistry, CqlSession.class);
            }

            if (bean instanceof ReactiveSession) {
                return lazyWrap(registry -> ObservableReactiveSessionFactory.wrap((ReactiveSession) bean, registry), bean, observationRegistry, ReactiveSession.class);
            }

            return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
        }
    }

    private static Object lazyWrap(Function<ObservationRegistry, Object> function, Object bean, ObjectProvider<ObservationRegistry> observationRegistry, Class<?> clazz) {

        Assert.notNull(function, "Supplier must not be null");
        Assert.notNull(observationRegistry, "ObservationRegistry must not be null");

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(bean);
        proxyFactory.addAdvice(new MethodInterceptor() {

            private Object delegate;

            @Nullable
            @Override
            public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
                if (this.delegate == null) {
                    this.delegate = function.apply(observationRegistry.getIfAvailable());
                }
                return invocation.getMethod().invoke(this.delegate, invocation.getArguments());
            }
        });
        proxyFactory.addInterface(clazz);

        return proxyFactory.getProxy();
    }
}
