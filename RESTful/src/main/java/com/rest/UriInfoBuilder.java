package com.rest;

interface UriInfoBuilder {
    Object getLastMatchedResource();

    void addMatchedResource(Object resource);
}
