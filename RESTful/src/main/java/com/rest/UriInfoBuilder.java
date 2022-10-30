package com.rest;

import jakarta.ws.rs.core.UriInfo;

import java.util.Map;

interface UriInfoBuilder {
    Object getLastMatchedResource();

    void addMatchedResource(Object resource);

    void addMatchedPathParameters(Map<String, String> pathParameters);

    UriInfo createUriInfo();
}
