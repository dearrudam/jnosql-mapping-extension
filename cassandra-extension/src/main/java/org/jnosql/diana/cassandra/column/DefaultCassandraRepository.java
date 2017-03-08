/*
 * Copyright 2017 Otavio Santana and others
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jnosql.diana.cassandra.column;


import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Statement;
import org.jnosql.artemis.column.AbstractColumnRepository;
import org.jnosql.artemis.column.ColumnEntityConverter;
import org.jnosql.artemis.column.ColumnWorkflow;
import org.jnosql.diana.api.column.ColumnDeleteQuery;
import org.jnosql.diana.api.column.ColumnEntity;
import org.jnosql.diana.api.column.ColumnFamilyManager;
import org.jnosql.diana.api.column.ColumnQuery;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

class DefaultCassandraRepository extends AbstractColumnRepository implements CassandraRepository {

    private Instance<CassandraColumnFamilyManager> manager;

    private ColumnEntityConverter converter;

    private ColumnWorkflow flow;

    @Inject
    DefaultCassandraRepository(Instance<CassandraColumnFamilyManager> manager,
                               ColumnEntityConverter converter,
                               ColumnWorkflow flow) {
        this.manager = manager;
        this.converter = converter;
        this.flow = flow;
    }

    DefaultCassandraRepository() {
    }


    @Override
    protected ColumnEntityConverter getConverter() {
        return converter;
    }

    @Override
    protected ColumnFamilyManager getManager() {
        return manager.get();
    }

    @Override
    protected ColumnWorkflow getFlow() {
        return flow;
    }

    @Override
    public <T> T save(T entity, ConsistencyLevel level) throws NullPointerException {
        Objects.requireNonNull(entity, "entity is required");
        Objects.requireNonNull(level, "level is required");
        UnaryOperator<ColumnEntity> save = e -> manager.get().save(e, level);
        return getFlow().flow(entity, save);
    }

    @Override
    public <T> T save(T entity, Duration ttl, ConsistencyLevel level) throws NullPointerException {
        Objects.requireNonNull(entity, "entity is required");
        Objects.requireNonNull(ttl, "ttl is required");
        Objects.requireNonNull(level, "level is required");
        UnaryOperator<ColumnEntity> save = e -> manager.get().save(e, ttl, level);
        return getFlow().flow(entity, save);
    }

    @Override
    public void delete(ColumnDeleteQuery query, ConsistencyLevel level) throws NullPointerException {
        manager.get().delete(query, level);
    }

    @Override
    public <T> List<T> find(ColumnQuery query, ConsistencyLevel level) throws NullPointerException {
        return manager.get().find(query, level).stream()
                .map(c -> (T) converter.toEntity(c))
                .collect(Collectors.toList());
    }

    @Override
    public <T> List<T> cql(String query) throws NullPointerException {
        return manager.get().cql(query).stream()
                .map(c -> (T) converter.toEntity(c))
                .collect(Collectors.toList());
    }

    @Override
    public <T> List<T> execute(Statement statement) throws NullPointerException {
        return manager.get().execute(statement).stream()
                .map(c -> (T) converter.toEntity(c))
                .collect(Collectors.toList());
    }
}
