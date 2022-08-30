package com.rest;

import java.util.ArrayList;
import java.util.List;

class StubUriInfoBuilder implements UriInfoBuilder {
    private List<Object> matchedResult = new ArrayList<>();

    public StubUriInfoBuilder() {
    }

    @Override
    public Object getLastMatchedResource() {
        return matchedResult.get(matchedResult.size() - 1);
    }

    @Override
    public void addMatchedResource(Object resource) {
        matchedResult.add(resource);
    }
}
