/*
 *  Copyright (c) 2017 Otávio Santana and others
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.jnosql.artemis.couchbase.document;


import com.couchbase.client.java.document.json.JsonObject;
import org.jnosql.artemis.RepositoryAsync;
import org.jnosql.artemis.document.DocumentTemplateAsync;
import org.jnosql.artemis.document.query.AbstractDocumentRepositoryAsync;
import org.jnosql.artemis.document.query.AbstractDocumentRepositoryAsyncProxy;
import org.jnosql.artemis.document.query.DocumentQueryDeleteParser;
import org.jnosql.artemis.document.query.DocumentQueryParser;
import org.jnosql.artemis.reflection.ClassRepresentation;
import org.jnosql.artemis.reflection.ClassRepresentations;
import org.jnosql.artemis.reflection.Reflections;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

class CouchbaseRepositoryAsyncProxy<T> extends AbstractDocumentRepositoryAsyncProxy<T> {

    private static final Consumer NOOP = t -> {
    };

    private final Class<T> typeClass;

    private final CouchbaseTemplateAsync template;


    private final DocumentCrudRepositoryAsync repository;

    private final ClassRepresentation classRepresentation;

    private final DocumentQueryParser queryParser;

    private final DocumentQueryDeleteParser deleteParser;


    CouchbaseRepositoryAsyncProxy(CouchbaseTemplateAsync template, ClassRepresentations classRepresentations,
                                  Class<?> repositoryType, Reflections reflections) {
        this.template = template;
        this.typeClass = Class.class.cast(ParameterizedType.class.cast(repositoryType.getGenericInterfaces()[0])
                .getActualTypeArguments()[0]);
        this.classRepresentation = classRepresentations.get(typeClass);
        this.queryParser = new DocumentQueryParser();
        this.repository = new DocumentCrudRepositoryAsync(template, classRepresentation, reflections);
        this.deleteParser = new DocumentQueryDeleteParser();
    }


    @Override
    protected RepositoryAsync getRepository() {
        return repository;
    }

    @Override
    protected DocumentQueryParser getQueryParser() {
        return queryParser;
    }

    @Override
    protected DocumentTemplateAsync getTemplate() {
        return template;
    }

    @Override
    protected DocumentQueryDeleteParser getDeleteParser() {
        return deleteParser;
    }

    @Override
    protected ClassRepresentation getClassRepresentation() {
        return classRepresentation;
    }

    @Override
    public Object invoke(Object instance, Method method, Object[] args) throws Throwable {

        N1QL n1QL = method.getAnnotation(N1QL.class);
        if (Objects.nonNull(n1QL)) {
            Consumer callBack = NOOP;
            if (Consumer.class.isInstance(args[args.length - 1])) {
                callBack = Consumer.class.cast(args[args.length - 1]);
            }

            Optional<JsonObject> params = getParams(args);
            if (params.isPresent()) {
                template.n1qlQuery(n1QL.value(), params.get(), callBack);
                return Void.class;
            } else {
                template.n1qlQuery(n1QL.value(), callBack);
                return Void.class;
            }
        }
        return super.invoke(instance, method, args);
    }

    private Optional<JsonObject> getParams(Object[] args) {
        return Stream.of(Optional.ofNullable(args).orElse(new Object[0]))
                .filter(a -> JsonObject.class.isInstance(a))
                .map(c -> JsonObject.class.cast(c))
                .findFirst();
    }

    class DocumentCrudRepositoryAsync extends AbstractDocumentRepositoryAsync implements RepositoryAsync {

        private final DocumentTemplateAsync template;
        private final ClassRepresentation classRepresentation;
        private final Reflections reflections;

        DocumentCrudRepositoryAsync(DocumentTemplateAsync template, ClassRepresentation classRepresentation, Reflections reflections) {
            this.template = template;
            this.classRepresentation = classRepresentation;
            this.reflections = reflections;
        }

        @Override
        protected DocumentTemplateAsync getTemplate() {
            return template;
        }

        @Override
        protected Reflections getReflections() {
            return reflections;
        }

        @Override
        protected ClassRepresentation getClassRepresentation() {
            return classRepresentation;
        }
    }
}