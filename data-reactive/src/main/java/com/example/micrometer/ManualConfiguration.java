package com.example.micrometer;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.observation.docs.ObservationDocumentation;
import io.micrometer.observation.transport.Kind;
import io.micrometer.observation.transport.SenderContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.proxy.callback.ProxyConfig;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.ConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.util.context.ContextView;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.function.Function;

@Configuration(proxyBeanMethods = false)
class ManualConfiguration {

    @Bean
    ObservationProxyExecutionListener observationProxyExecutionListener(ObservationRegistry observationRegistry, ConnectionFactory connectionFactory, R2dbcProperties r2dbcProperties) {
        return new ObservationProxyExecutionListener(observationRegistry, connectionFactory, r2dbcProperties);
    }

    @Bean
    static ObservationConnectionFactoryBeanPostProcessor observationConnectionFactoryBeanPostProcessor(BeanFactory beanFactory) {
        return new ObservationConnectionFactoryBeanPostProcessor(beanFactory);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 5)
    R2DbcTracingHandler r2DbcTracingHandler(Tracer tracer) {
        return new R2DbcTracingHandler(tracer);
    }
}

/**
 * Sender Context for R2DBC.
 */
class R2DbcContext extends SenderContext {

    private URI uri;

    public R2DbcContext(String name) {
        super((carrier, key, value) -> {
        }, Kind.CLIENT);
        setRemoteServiceName(name);
    }

    URI getUri() {
        return uri;
    }

    void setUri(URI host) {
        this.uri = host;
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

    public ObservationProxyExecutionListener(ObservationRegistry observationRegistry, ConnectionFactory connectionFactory, R2dbcProperties r2dbcProperties) {
        this.observationRegistry = observationRegistry;
        this.connectionFactory = connectionFactory;
        this.r2dbcProperties = r2dbcProperties;
    }

    @Override
    public void beforeQuery(QueryExecutionInfo executionInfo) {
        if (observationRegistry.isNoop()) {
            return;
        }
        ContextView contextView = executionInfo.getValueStore().get(ContextView.class, ContextView.class);
        Observation parentObservation = contextView.getOrDefault(ObservationThreadLocalAccessor.KEY, observationRegistry.getCurrentObservation());
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
        executionInfo.getValueStore().put(Observation.class, observation);
    }

    Observation clientObservation(Observation parentObservation, QueryExecutionInfo executionInfo, String name) {
        String url = r2dbcProperties.getUrl();
        // @formatter:off
        R2DbcContext context = new R2DbcContext(name);
        Observation observation = R2DbcObservationDocumentation.R2DBC_QUERY_OBSERVATION.observation(observationRegistry, () -> context)
                .parentObservation(parentObservation)
                .lowCardinalityKeyValue(R2DbcObservationDocumentation.LowCardinalityKeys.CONNECTION.withValue(name))
                .lowCardinalityKeyValue(R2DbcObservationDocumentation.LowCardinalityKeys.THREAD.withValue(executionInfo.getThreadName()));
        // @formatter:on

        if (StringUtils.hasText(url)) {
            try {
                URI uri = URI.create(url);
                context.setUri(uri);
            }
            catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to parse the url [" + url + "]. Won't set this value as a tag");
                }
            }
        }
        return observation.start();
    }

    private void tagQueries(QueryExecutionInfo executionInfo, Observation observation) {
        int i = 0;
        for (QueryInfo queryInfo : executionInfo.getQueries()) {
            observation.highCardinalityKeyValue(String.format(R2DbcObservationDocumentation.HighCardinalityKeys.QUERY.name(), i), queryInfo.getQuery());
            i = i + 1;
        }
    }

    @Override
    public void afterQuery(QueryExecutionInfo executionInfo) {
        Observation observation = executionInfo.getValueStore().get(Observation.class, Observation.class);
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
        Observation observation = executionInfo.getValueStore().get(Observation.class, Observation.class);
        if (observation != null) {
            if (log.isDebugEnabled()) {
                log.debug("Marking after query result for observation [" + observation + "]");
            }
            observation.event(R2DbcObservationDocumentation.Events.QUERY_RESULT);
        }
    }

}

/**
 * Dedicated Tracing Handler for R2DBC.
 */
class R2DbcTracingHandler extends DefaultTracingObservationHandler {

    R2DbcTracingHandler(Tracer tracer) {
        super(tracer);
    }

    @Override
    public void onStart(Observation.Context context) {
        super.onStart(context);
        R2DbcContext r2DbcContext = (R2DbcContext) context;
        if (r2DbcContext.getUri() != null) {
            getTracingContext(context).getSpan().remoteIpAndPort(r2DbcContext.getUri().getHost(), r2DbcContext.getUri().getPort());
        }
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof R2DbcContext;
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
class LazyObservationProxyExecutionListener implements ProxyExecutionListener {

    private ObservationProxyExecutionListener delegate;

    private final BeanFactory beanFactory;

    private final ConnectionFactory connectionFactory;

    LazyObservationProxyExecutionListener(BeanFactory beanFactory, ConnectionFactory connectionFactory) {
        this.beanFactory = beanFactory;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void beforeMethod(MethodExecutionInfo executionInfo) {
        delegate().beforeMethod(executionInfo);
    }

    @Override
    public void afterMethod(MethodExecutionInfo executionInfo) {
        delegate().afterMethod(executionInfo);
    }

    @Override
    public void beforeQuery(QueryExecutionInfo execInfo) {
        delegate().beforeQuery(execInfo);
    }

    @Override
    public void afterQuery(QueryExecutionInfo execInfo) {
        delegate().afterQuery(execInfo);
    }

    @Override
    public void eachQueryResult(QueryExecutionInfo execInfo) {
        delegate().eachQueryResult(execInfo);
    }

    private ProxyExecutionListener delegate() {
        if (this.delegate == null) {
            this.delegate = new ObservationProxyExecutionListener(this.beanFactory.getBean(ObservationRegistry.class), this.connectionFactory, this.beanFactory.getBean(R2dbcProperties.class));
        }
        return this.delegate;
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
        builder.listener(new LazyObservationProxyExecutionListener(this.beanFactory, connectionFactory));
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
