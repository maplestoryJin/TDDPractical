package com.rest;

interface UriInfoBuilder {
    void pushMethodPath(String path);

    void addParameter(String name, String value);

    String getUnmatchedPath();
}
