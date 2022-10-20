package com.example.micrometer;

import org.springframework.context.annotation.Configuration;

/**
 * In this class we'll add all the manual configuration required for
 * Observability to work.
 */
@Configuration(proxyBeanMethods = false)
public class ManualConfiguration {
    // You must set this manually until this is registered in Boot
  /*  @Bean
    RSocketResponderTracingObservationHandler rSocketResponderTracingObservationHandler(Tracer tracer, Propagator propagator) {
        return new RSocketResponderTracingObservationHandler(
                tracer,
                propagator,
                new ByteBufGetter(),
                false);
    }
*/
    // TODO: Needs RSocket 1.2.0
}
