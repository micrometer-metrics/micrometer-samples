/*
 * Copyright 2013-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.micrometer;

import reactor.core.publisher.Mono;

import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * Simple repository interface for {@link User} instances. The interface is used to
 * declare so called query methods, methods to retrieve single entities or collections of
 * them.
 *
 * @author Mark Paluch
 */
public interface BasicUserRepository extends ReactiveCrudRepository<User, Long> {

    /**
     * Sample method annotated with {@link Query}. This method executes the CQL from the
     * {@link Query} value.
     * @param id
     * @return
     */
    @Query("SELECT * from users where user_id in(?0)")
    Mono<User> findUserByIdIn(long id);

    /**
     * Derived query method. This query corresponds with
     * {@code SELECT * FROM users WHERE uname = ?0}. {@link User#username} is not part of
     * the primary so it requires a secondary index.
     * @param username
     * @return
     */
    Mono<User> findUserByUsername(String username);

}
