package com.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public interface ResourceRouter {
    OutBoundResponse dispatch(HttpServletRequest req, ResourceContext rc);


    interface Resource {
        Optional<ResourceMethod> match(String path, String method, String[] mediaTypes, UriInfoBuilder builder);
    }

    interface RootResource extends Resource {
        UriTemplate getUriTemplate();
    }

    interface ResourceMethod {
        GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder);
    }

}

class DefaultResourceRouter implements ResourceRouter {

    private Runtime runtime;
    private List<RootResource> rootResources;

    public DefaultResourceRouter(Runtime runtime, List<RootResource> rootResources) {
        this.runtime = runtime;
        this.rootResources = rootResources;
    }

    @Override
    public OutBoundResponse dispatch(HttpServletRequest req, ResourceContext rc) {
        String path = req.getServletPath();
        UriInfoBuilder uri = runtime.createUriInfoBuilder(req);
        Optional<Result> matched = rootResources.stream().map(resource -> new Result(resource.getUriTemplate().match(path), resource))
                .filter(result -> result.matched.isPresent()).sorted(Comparator.comparing(x -> x.matched.get()))
                .findFirst();
        Optional<ResourceMethod> method = matched.flatMap(result -> result.resource.match(result.matched.get().getRemaining(), req.getMethod(), Collections.list(req.getHeaders(HttpHeaders.ACCEPT)).toArray(String[]::new), uri));
        GenericEntity<?> entity = method.map(m -> m.call(rc, uri)).get();
        return (OutBoundResponse) Response.ok().entity(entity).build();
    }

    record Result(Optional<UriTemplate.MatchResult> matched, RootResource resource) {
    }

}
