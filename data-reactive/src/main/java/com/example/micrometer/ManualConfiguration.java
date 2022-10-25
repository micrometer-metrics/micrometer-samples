package com.example.micrometer;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.observation.docs.ObservationDocumentation;
import io.micrometer.observation.transport.Kind;
import io.micrometer.observation.transport.SenderContext;
import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.proxy.callback.DelegatingContextView;
import io.r2dbc.proxy.callback.ProxyConfig;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.ConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.function.Function;

@Configuration(proxyBeanMethods = false)
class ManualConfiguration {

    @Bean
    LazyObservationProxyExecutionListener lazyObservationProxyExecutionListener(BeanFactory beanFactory) {
        return new LazyObservationProxyExecutionListener(beanFactory);
    }

    @Bean
    static ObservationConnectionFactoryBeanPostProcessor observationConnectionFactoryBeanPostProcessor(
            BeanFactory beanFactory) {
        return new ObservationConnectionFactoryBeanPostProcessor(beanFactory);
    }

}

/**
 * Instrumentation code for R2DBC.
 */
class ObservationProxyExecutionListener implements ProxyExecutionListener {

    private static final Log log = LogFactory.getLog(ObservationProxyExecutionListener.class);

    private final ConnectionFactory connectionFactory;

    private final ObservationRegistry observationRegistry;

    private final R2dbcProperties r2dbcProperties;

    public ObservationProxyExecutionListener(ObservationRegistry observationRegistry,
            ConnectionFactory connectionFactory, R2dbcProperties r2dbcProperties) {
        this.observationRegistry = observationRegistry;
        this.connectionFactory = connectionFactory;
        this.r2dbcProperties = r2dbcProperties;
    }

    @Override
    public void beforeQuery(QueryExecutionInfo executionInfo) {
        if (observationRegistry.isNoop()) {
            return;
        }
        Observation parentObservation = executionInfo.getValueStore()
                .getOrDefault(ContextView.class, new DelegatingContextView(Context.empty()))
                .getOrDefault(ObservationThreadLocalAccessor.KEY, observationRegistry.getCurrentObservation()); // TODO:
                                                                                                                // Won't
                                                                                                                // work
                                                                                                                // until
                                                                                                                // https://github.com/r2dbc/r2dbc-proxy/pull/121
                                                                                                                // gets
                                                                                                                // merged
        if (parentObservation == null) {
            if (log.isDebugEnabled()) {
                log.debug("Parent observation not present, won't do any instrumentation");
            }
            return;
        }
        String name = this.connectionFactory.getMetadata().getName();
        Observation observation = clientObservation(parentObservation, executionInfo, name);
        if (log.isDebugEnabled()) {
            log.debug("Created a new child observation before query [" + observation + "]");
        }
        tagQueries(executionInfo, observation);
        executionInfo.getValueStore().put(ObservationThreadLocalAccessor.KEY, observation);
    }

    Observation clientObservation(Observation parentObservation, QueryExecutionInfo executionInfo, String name) {
        String url = r2dbcProperties.getUrl();
        // @formatter:off
        SenderContext<?> context = new SenderContext<>((carrier, key, value) -> { }, Kind.CLIENT);
        context.setRemoteServiceName(name);
        context.setRemoteServiceAddress(url);
        Observation observation = R2DbcObservationDocumentation.R2DBC_QUERY_OBSERVATION.observation(observationRegistry, () -> context)
                .parentObservation(parentObservation)
                .lowCardinalityKeyValue(R2DbcObservationDocumentation.LowCardinalityKeys.CONNECTION.withValue(name))
                .lowCardinalityKeyValue(R2DbcObservationDocumentation.LowCardinalityKeys.THREAD.withValue(executionInfo.getThreadName()));
        // @formatter:on
        return observation.start();
    }

    private void tagQueries(QueryExecutionInfo executionInfo, Observation observation) {
        int i = 0;
        for (QueryInfo queryInfo : executionInfo.getQueries()) {
            observation.highCardinalityKeyValue(
                    String.format(R2DbcObservationDocumentation.HighCardinalityKeys.QUERY.name(), i),
                    queryInfo.getQuery());
            i = i + 1;
        }
    }

    @Override
    public void afterQuery(QueryExecutionInfo executionInfo) {
        Observation observation = executionInfo.getValueStore().get(ObservationThreadLocalAccessor.KEY,
                Observation.class);
        if (observation != null) {
            if (log.isDebugEnabled()) {
                log.debug("Continued the child observation in after query [" + observation + "]");
            }
            final Throwable throwable = executionInfo.getThrowable();
            if (throwable != null) {
                observation.error(throwable);
            }
            observation.stop();
        }
    }

    @Override
    public void eachQueryResult(QueryExecutionInfo executionInfo) {
        Observation observation = executionInfo.getValueStore().get(ObservationThreadLocalAccessor.KEY,
                Observation.class);
        if (observation != null) {
            if (log.isDebugEnabled()) {
                log.debug("Marking after query result for observation [" + observation + "]");
            }
            observation.event(R2DbcObservationDocumentation.Events.QUERY_RESULT);
        }
    }

}

/**
 * BPP wrapping connection factories.
 */
class ObservationConnectionFactoryBeanPostProcessor implements BeanPostProcessor {

    private final BeanFactory beanFactory;

    public ObservationConnectionFactoryBeanPostProcessor(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ConnectionFactory) {
            return wrapConnectionFactory((ConnectionFactory) bean);
        }
        return bean;
    }

    ConnectionFactory wrapConnectionFactory(ConnectionFactory bean) {
        ObservationProxyConnectionFactoryWrapper proxyPostProcessor = new ObservationProxyConnectionFactoryWrapper(
                this.beanFactory);
        return proxyPostProcessor.apply(bean);
    }

}

/**
 * Since we don't want to eagerly read beans - we will create a lazy proxy.
 */
class LazyObservationProxyExecutionListener
        implements ProxyExecutionListener, ApplicationListener<ContextRefreshedEvent> {

    private ObservationProxyExecutionListener delegate;

    private final BeanFactory beanFactory;

    LazyObservationProxyExecutionListener(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public void beforeMethod(MethodExecutionInfo executionInfo) {
        if (this.delegate == null) {
            return;
        }
        this.delegate.beforeMethod(executionInfo);
    }

    @Override
    public void afterMethod(MethodExecutionInfo executionInfo) {
        if (this.delegate == null) {
            return;
        }
        this.delegate.afterMethod(executionInfo);
    }

    @Override
    public void beforeQuery(QueryExecutionInfo execInfo) {
        if (this.delegate == null) {
            return;
        }
        this.delegate.beforeQuery(execInfo);
    }

    @Override
    public void afterQuery(QueryExecutionInfo execInfo) {
        if (this.delegate == null) {
            return;
        }
        this.delegate.afterQuery(execInfo);
    }

    @Override
    public void eachQueryResult(QueryExecutionInfo execInfo) {
        if (this.delegate == null) {
            return;
        }
        this.delegate.eachQueryResult(execInfo);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.delegate = new ObservationProxyExecutionListener(this.beanFactory.getBean(ObservationRegistry.class),
                this.beanFactory.getBean(ConnectionFactory.class), this.beanFactory.getBean(R2dbcProperties.class));
    }

}

/**
 * Function converting the current ConnectionFactory into observed one.
 */
class ObservationProxyConnectionFactoryWrapper implements Function<ConnectionFactory, ConnectionFactory> {

    private final BeanFactory beanFactory;

    private ObjectProvider<ProxyConfig> proxyConfig;

    ObservationProxyConnectionFactoryWrapper(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public ConnectionFactory apply(ConnectionFactory connectionFactory) {
        ProxyConnectionFactory.Builder builder = ProxyConnectionFactory.builder(connectionFactory);
        proxyConfig().ifAvailable(builder::proxyConfig);
        builder.listener(this.beanFactory.getBean(LazyObservationProxyExecutionListener.class));
        return builder.build();
    }

    private ObjectProvider<ProxyConfig> proxyConfig() {
        if (this.proxyConfig == null) {
            this.proxyConfig = this.beanFactory.getBeanProvider(ProxyConfig.class);
        }
        return this.proxyConfig;
    }

}

/**
 * Enum for R2DBC Observations.
 */
enum R2DbcObservationDocumentation implements ObservationDocumentation {

    /**
     * Span created on the Kafka consumer side.
     */
    R2DBC_QUERY_OBSERVATION {
        @Override
        public String getName() {
            return "r2dbc.query";
        }

        @Override
        public String getContextualName() {
            return "query";
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return LowCardinalityKeys.values();
        }

        @Override
        public KeyName[] getHighCardinalityKeyNames() {
            return HighCardinalityKeys.values();
        }

        @Override
        public Observation.Event[] getEvents() {
            return Events.values();
        }

        @Override
        public String getPrefix() {
            return "r2dbc.";
        }
    };

    enum Events implements Observation.Event {

        /**
         * Annotated before executing a method annotated with @ContinueSpan or @NewSpan.
         */
        QUERY_RESULT {
            @Override
            public String getName() {
                return "r2dbc.query_result";
            }
        }

    }

    enum LowCardinalityKeys implements KeyName {

        /**
         * Name of the R2DBC connection.
         */
        CONNECTION {
            @Override
            public String asString() {
                return "r2dbc.connection";
            }
        },

        /**
         * Name of the R2DBC thread.
         */
        THREAD {
            @Override
            public String asString() {
                return "r2dbc.thread";
            }
        }

    }

    enum HighCardinalityKeys implements KeyName {

        /**
         * Name of the R2DBC query.
         */
        QUERY {
            @Override
            public String asString() {
                return "r2dbc.query[%s]";
            }
        },

    }

}
