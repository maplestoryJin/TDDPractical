package com.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        PathTemplate getUriTemplate();
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
        Optional<ResourceMethod> method = rootResources.stream().map(resource -> match(path, resource))
                .filter(Result::isMatched)
                .sorted()
                .findFirst()
                .flatMap(result -> result.findResourceMethod(req, uri));
        if (method.isEmpty()) return (OutBoundResponse) Response.status(Response.Status.NOT_FOUND).build();
        return (OutBoundResponse) method.map(m -> m.call(rc, uri))
                .map(entity -> Response.ok(entity).build())
                .orElseGet(() -> Response.noContent().build());
    }

    private Result match(String path, RootResource resource) {
        return new Result(resource.getUriTemplate().match(path), resource);
    }

    record Result(Optional<UriTemplate.MatchResult> matched, RootResource resource) implements Comparable<Result> {
        private boolean isMatched() {
            return matched.isPresent();
        }

        @Override
        public int compareTo(Result o) {
            return matched.flatMap(x -> o.matched.map(x::compareTo)).orElse(0);
        }

        private Optional<ResourceMethod> findResourceMethod(HttpServletRequest req, UriInfoBuilder uri) {
            return matched.flatMap(result -> resource.match(matched.get().getRemaining(), req.getMethod(),
                    Collections.list(req.getHeaders(HttpHeaders.ACCEPT)).toArray(String[]::new), uri));
        }
    }

}

class RootResourceClass implements ResourceRouter.RootResource {
    private PathTemplate uriTemplate;
    private Class<?> resourceClass;
    private List<ResourceRouter.ResourceMethod> resourceMethods;

    public RootResourceClass(Class<?> resourceClass) {
        this.resourceClass = resourceClass;
        this.uriTemplate = new PathTemplate(resourceClass.getAnnotation(Path.class).value());

        resourceMethods = Arrays.stream(resourceClass.getMethods()).filter(m -> Arrays.stream(m.getAnnotations())
                        .anyMatch(a -> a.annotationType().isAnnotationPresent(HttpMethod.class)))
                .map(m -> ((ResourceRouter.ResourceMethod) new DefaultResourceMethod(m)))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ResourceRouter.ResourceMethod> match(String path, String method, String[] mediaTypes, UriInfoBuilder builder) {
        UriTemplate.MatchResult result = uriTemplate.match(path).get();
        String remaining = result.getRemaining();
        return resourceMethods.stream().filter(m -> m.getUriTemplate().match(remaining).map(r -> r.getRemaining() == null).orElse(false))
                .findFirst();
    }

    @Override
    public UriTemplate getUriTemplate() {
        return uriTemplate;
    }

    static class DefaultResourceMethod implements ResourceRouter.ResourceMethod {
        private PathTemplate uriTemplate;
        private Method method;

        public DefaultResourceMethod(Method method) {
            this.method = method;
            this.uriTemplate = new PathTemplate(method.getAnnotation(Path.class).value());
        }

        @Override
        public GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder) {
            return null;
        }

        @Override
        public String toString() {
            return method.getDeclaringClass().getSimpleName() + "." + method.getName();
        }

        @Override
        public PathTemplate getUriTemplate() {
            return uriTemplate;
        }
    }

}
