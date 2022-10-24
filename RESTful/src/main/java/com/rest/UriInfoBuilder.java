package com.rest;

import jakarta.ws.rs.core.UriInfo;

interface UriInfoBuilder {
    Object getLastMatchedResource();

    void addMatchedResource(Object resource);

    UriInfo createUriInfo();
}
