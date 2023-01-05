package com.example.micrometer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ReactiveCustomerRepository extends ReactiveCrudRepository<ReactiveCustomer, Long> {

    Flux<ReactiveCustomer> findByLastName(String lastName);

    Mono<ReactiveCustomer> findById(long id);

}
