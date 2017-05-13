/*
 * Copyright 2017 Otavio Santana and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jnosql.artemis.orientdb.document;

import org.jnosql.artemis.DynamicQueryException;
import org.jnosql.artemis.reflection.ClassRepresentations;
import org.jnosql.artemis.reflection.Reflections;
import org.jnosql.diana.api.column.ColumnQuery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.inject.Inject;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

@RunWith(WeldJUnit4Runner.class)
public class OrientDBRepositoryAsyncProxyTest {


    private OrientDBDocumentTemplateAsync repository;

    @Inject
    private ClassRepresentations classRepresentations;

    @Inject
    private Reflections reflections;

    private PersonAsyncRepository personRepository;


    @Before
    public void setUp() {
        this.repository = Mockito.mock(OrientDBDocumentTemplateAsync.class);

        OrientDBRepositoryAsyncProxy handler = new OrientDBRepositoryAsyncProxy(repository,
                classRepresentations, PersonAsyncRepository.class, reflections);


        personRepository = (PersonAsyncRepository) Proxy.newProxyInstance(PersonAsyncRepository.class.getClassLoader(),
                new Class[]{PersonAsyncRepository.class},
                handler);
    }



    @Test(expected = DynamicQueryException.class)
    public void shouldReturnError() {
        personRepository.findByName("Ada");
    }



    @Test
    public void shouldFindNoCallback() {
        ArgumentCaptor<ColumnQuery> captor = ArgumentCaptor.forClass(ColumnQuery.class);
        personRepository.queryName("Ada");
    }

    @Test
    public void shouldFindByNameFromCQL() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        Consumer<List<Person>> callBack = p -> {
        };
        personRepository.queryName("Ada", callBack);

        verify(repository).find(Mockito.eq("select * from Person where name= ?"), Mockito.eq(callBack), captor.capture());
        Object value = captor.getValue();
        assertEquals("Ada", value);

    }

    interface PersonAsyncRepository extends OrientDBCrudRepositoryAsync<Person, String> {

        Person findByName(String name);


        @SQL("select * from Person where name= ?")
        void queryName(String name);

        @SQL("select * from Person where name= ?")
        void queryName(String name, Consumer<List<Person>> callBack);
    }
}