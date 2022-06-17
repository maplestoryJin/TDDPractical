package com.rest;

import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.Response;

import java.lang.annotation.Annotation;

public abstract class OutBoundResponse extends Response {
    abstract Annotation[] getAnnotations();

    abstract GenericEntity<?> getGenericEntity();

}
