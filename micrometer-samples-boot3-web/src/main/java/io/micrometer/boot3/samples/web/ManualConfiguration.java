package io.micrometer.boot3.samples.web;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.ServletException;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerHttpObservationDocumentation;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationConvention;

import java.io.IOException;

@Configuration(proxyBeanMethods = false)
public class ManualConfiguration {

    static final String CUSTOMIZER_NAME = "observedTomcatWebServerFactoryCustomizer";

    // If you want to have logs in error logs of Tomcat uncomment the @Bean method
    // - this will result in 2 spans on the server side that look the same
    // but one will be longer (the one on Tomcat level). The other one comes from MVC.

    // @Bean(name = CUSTOMIZER_NAME)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> observedTomcatWebServerFactoryCustomizer(
            ObservationRegistry observationRegistry) {
        return factory -> factory.addEngineValves(new ObservedValve(observationRegistry));

    }

}

class ObservedValve extends ValveBase {

    private static final ServerRequestObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultServerRequestObservationConvention();

    private final ObservationRegistry observationRegistry;

    private final ServerRequestObservationConvention observationConvention;

    public ObservedValve(ObservationRegistry observationRegistry,
            ServerRequestObservationConvention observationConvention) {
        this.observationRegistry = observationRegistry;
        this.observationConvention = observationConvention;
        setAsyncSupported(true);
    }

    public ObservedValve(ObservationRegistry observationRegistry) {
        this(observationRegistry, null);
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        Observation observation = (Observation) request.getAttribute(Observation.class.getName());
        if (observation != null) {
            // this could happen for async dispatch
            try (Observation.Scope scope = observation.openScope()) {
                Valve next = getNext();
                if (null == next) {
                    // no next valve
                    return;
                }
                next.invoke(request, response);
                return;
            }
        }
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, response);
        observation = ServerHttpObservationDocumentation.HTTP_SERVLET_SERVER_REQUESTS
            .observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> context,
                    this.observationRegistry)
            .start();
        request.setAttribute(Observation.class.getName(), observation);
        try (Observation.Scope scope = observation.openScope()) {
            Valve next = getNext();
            if (null == next) {
                // no next valve
                return;
            }
            next.invoke(request, response);
        }
        catch (Exception exception) {
            observation.error(exception);
            throw exception;
        }
        finally {
            observation.stop();
        }
    }

}
