package com.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.rest.DefaultResourceMethod.ValueConverter.singleValued;

public interface ResourceRouter {
    OutBoundResponse dispatch(HttpServletRequest req, ResourceContext rc);


    interface Resource extends UriHandler {
        Optional<ResourceMethod> match(UriTemplate.MatchResult result, String httpMethod, String[] mediaTypes, ResourceContext resourceContext, UriInfoBuilder builder);
    }

    interface ResourceMethod extends UriHandler {
        GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder);

        UriTemplate getUriTemplate();

        String getHttpMethod();
    }

}

class DefaultResourceRouter implements ResourceRouter {

    private Runtime runtime;
    private List<Resource> rootResources;

    public DefaultResourceRouter(Runtime runtime, List<Resource> rootResources) {
        this.runtime = runtime;
        this.rootResources = rootResources;
    }

    @Override
    public OutBoundResponse dispatch(HttpServletRequest req, ResourceContext resourceContext) {
        String path = req.getServletPath();
        UriInfoBuilder uri = runtime.createUriInfoBuilder(req);
        Optional<ResourceMethod> method = UriHandlers.mapMatched(path, rootResources, (result, resource) -> findResourceMethod(req, resourceContext, uri, result, resource));
        if (method.isEmpty()) return (OutBoundResponse) Response.status(Response.Status.NOT_FOUND).build();
        return (OutBoundResponse) method.map(m -> m.call(resourceContext, uri))
                .map(entity -> (entity.getEntity() instanceof OutBoundResponse) ? ((OutBoundResponse) entity.getEntity()) : Response.ok(entity).build())
                .orElseGet(() -> Response.noContent().build());
    }


    private Optional<ResourceMethod> findResourceMethod(HttpServletRequest req, ResourceContext resourceContext, UriInfoBuilder uri, Optional<UriTemplate.MatchResult> matched, Resource handler) {
        return handler.match(matched.get(), req.getMethod(), Collections.list(req.getHeaders(HttpHeaders.ACCEPT)).toArray(String[]::new), resourceContext,
                uri);
    }

}

class ResourceMethods {

    private Map<String, List<ResourceRouter.ResourceMethod>> methods;

    public ResourceMethods(Method[] methods) {
        this.methods = getResourceMethods(methods);
    }

    private static Map<String, List<ResourceRouter.ResourceMethod>> getResourceMethods(Method[] methods) {
        return Arrays.stream(methods).filter(m -> Arrays.stream(m.getAnnotations())
                        .anyMatch(a -> a.annotationType().isAnnotationPresent(HttpMethod.class)))
                .map(DefaultResourceMethod::new)
                .collect(Collectors.groupingBy(ResourceRouter.ResourceMethod::getHttpMethod));
    }


    public Optional<ResourceRouter.ResourceMethod> findResourceMethods(String path, String method) {
        return findMethod(path, method).or(() -> alternative(path, method));
    }

    private Optional<ResourceRouter.ResourceMethod> alternative(String path, String method) {
        if (HttpMethod.HEAD.equals(method)) return findMethod(path, HttpMethod.GET).map(HeadResourceMethod::new);
        if (HttpMethod.OPTIONS.equals(method)) return Optional.of(new OptionResourceMethod(path));
        return Optional.empty();
    }

    private Optional<ResourceRouter.ResourceMethod> findMethod(String path, String httpMethod) {
        return Optional.ofNullable(methods.get(httpMethod)).flatMap(methods -> UriHandlers.match(path, methods, r -> r.getRemaining() == null));
    }

    class OptionResourceMethod implements ResourceRouter.ResourceMethod {
        private String path;

        public OptionResourceMethod(String path) {
            this.path = path;
        }

        @Override
        public GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder) {

            return new GenericEntity<>(Response.noContent().allow(findAllowedMethods()).build(), Response.class);
        }

        private Set<String> findAllowedMethods() {
            Set<String> allowed = List.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.PUT, HttpMethod.POST, HttpMethod.DELETE, HttpMethod.PATCH).stream()
                    .filter(method -> findMethod(path, method).isPresent()).collect(Collectors.toSet());
            allowed.add(HttpMethod.OPTIONS);
            if (allowed.contains(HttpMethod.GET)) allowed.add(HttpMethod.HEAD);
            return allowed;
        }

        @Override
        public UriTemplate getUriTemplate() {
            return new PathTemplate(path);
        }

        @Override
        public String getHttpMethod() {
            return HttpMethod.OPTIONS;
        }
    }
}


class DefaultResourceMethod implements ResourceRouter.ResourceMethod {
    private String httpMethod;
    private PathTemplate uriTemplate;
    private Method method;

    public DefaultResourceMethod(Method method) {
        this.method = method;
        this.uriTemplate = new PathTemplate(Optional.ofNullable(method.getAnnotation(Path.class)).map(Path::value).orElse(""));
        this.httpMethod = Arrays.stream(method.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(HttpMethod.class))
                .findFirst().get().annotationType().getAnnotation(HttpMethod.class).value();
    }

    @Override
    public GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder) {
        Object result = MethodInvoker.invoke(method, resourceContext, builder);
        return result != null ? new GenericEntity<>(result, method.getGenericReturnType()) : null;
    }


    class PrimitiveConverter {
        private static Map<Type, ValueConverter<Object>> primitive = Map.of(int.class, singleValued(Integer::parseInt),
                byte.class, singleValued(Byte::parseByte),
                char.class, singleValued(s -> s.charAt(0)),
                short.class, singleValued(Short::parseShort),
                String.class, singleValued(s -> s),
                boolean.class, singleValued(Boolean::parseBoolean),
                float.class, singleValued(Float::parseFloat),
                double.class, singleValued(Double::parseDouble));

        static Optional<Object> convert(Class<?> converter, List<String> values) {
            return Optional.ofNullable(primitive.get(converter)).map(it -> it.fromString(values));
        }
    }


    interface ValueConverter<T> {
        static <T> ValueConverter<T> singleValued(Function<String, T> converter) {
            return values -> converter.apply(values.get(0));
        }

        T fromString(List<String> value);
    }

    @Override
    public String toString() {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    @Override
    public PathTemplate getUriTemplate() {
        return uriTemplate;
    }

    @Override
    public String getHttpMethod() {
        return httpMethod;
    }
}

class HeadResourceMethod implements ResourceRouter.ResourceMethod {
    private ResourceRouter.ResourceMethod resourceMethod;

    HeadResourceMethod(ResourceRouter.ResourceMethod resourceMethod) {
        this.resourceMethod = resourceMethod;
    }

    @Override
    public GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder) {
        resourceMethod.call(resourceContext, builder);
        return null;
    }

    @Override
    public UriTemplate getUriTemplate() {
        return resourceMethod.getUriTemplate();
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.HEAD;
    }

    @Override
    public String toString() {
        return resourceMethod.toString();
    }
}


class ConverterConstructor {

    public static Optional<Object> convert(Class<?> converter, String value) {
        try {
            return Optional.of(converter.getConstructor(String.class).newInstance(value));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return Optional.empty();
        }
    }
}

class ConverterFactory {

    public static Optional<Object> convert(Class<?> converter, String value) {
        try {
            return Optional.of(converter.getMethod("valueOf", String.class).invoke(null, value));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return Optional.empty();
        }
    }
}


class SubResourceLocators {

    private final List<ResourceRouter.Resource> subResourceLocators;

    public SubResourceLocators(Method[] methods) {
        subResourceLocators = Arrays.stream(methods).filter(m -> m.isAnnotationPresent(Path.class) &&
                        Arrays.stream(m.getAnnotations()).noneMatch(a -> a.annotationType().isAnnotationPresent(HttpMethod.class)))
                .map(m -> (ResourceRouter.Resource) new SubResourceLocator(m))
                .toList();

    }

    public Optional<ResourceRouter.ResourceMethod> findResourceMethods(String path, String method, String[] mediaTypes, ResourceContext resourceContext, UriInfoBuilder uriInfoBuilder) {
        return UriHandlers.mapMatched(path, subResourceLocators, (result, locator) ->
                locator.match(result.get(), method, mediaTypes, resourceContext, uriInfoBuilder));
    }

    static class SubResourceLocator implements ResourceRouter.Resource {
        private PathTemplate uriTemplate;
        private Method method;

        public SubResourceLocator(Method method) {
            this.method = method;
            this.uriTemplate = new PathTemplate(method.getAnnotation(Path.class).value());
        }

        @Override
        public PathTemplate getUriTemplate() {
            return uriTemplate;
        }

        @Override
        public String toString() {
            return method.getDeclaringClass().getSimpleName() + "." + method.getName();
        }

        @Override
        public Optional<ResourceRouter.ResourceMethod> match(UriTemplate.MatchResult result, String httpMethod, String[] mediaTypes, ResourceContext resourceContext, UriInfoBuilder builder) {
            try {
                builder.addMatchedPathParameters(result.getMatchedPathParameters());
                Object subResource = MethodInvoker.invoke(method, resourceContext, builder);
                return new ResourceHandler(subResource, uriTemplate).match(excludePathParameters(result), httpMethod, mediaTypes, resourceContext, builder);
            } catch (WebApplicationException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private UriTemplate.MatchResult excludePathParameters(UriTemplate.MatchResult result) {
            return new UriTemplate.MatchResult() {
                @Override
                public String getMatched() {
                    return result.getMatched();
                }

                @Override
                public String getRemaining() {
                    return result.getRemaining();
                }

                @Override
                public Map<String, String> getMatchedPathParameters() {
                    return new HashMap<>();
                }

                @Override
                public int compareTo(UriTemplate.MatchResult o) {
                    return result.compareTo(o);
                }
            };
        }

    }
}

class ResourceHandler implements ResourceRouter.Resource {
    private ResourceMethods resourceMethods;
    private UriTemplate uriTemplate;
    private SubResourceLocators subResourceLocators;
    private Function<ResourceContext, Object> resource;

    public ResourceHandler(Class<?> resourceClass) {
        this(resourceClass, new PathTemplate(getTemplate(resourceClass)), rc -> rc.getResource(resourceClass));
    }

    private static String getTemplate(Class<?> resourceClass) {
        if (!resourceClass.isAnnotationPresent(Path.class)) throw new IllegalArgumentException();
        return resourceClass.getAnnotation(Path.class).value();
    }

    public ResourceHandler(Object resource, UriTemplate uriTemplate) {
        this(resource.getClass(), uriTemplate, rc -> resource);
    }

    private ResourceHandler(Class<?> resourceClass, UriTemplate uriTemplate, Function<ResourceContext, Object> resource) {
        this.uriTemplate = uriTemplate;
        this.resourceMethods = new ResourceMethods(resourceClass.getMethods());
        this.subResourceLocators = new SubResourceLocators(resourceClass.getMethods());
        this.resource = resource;
    }

    @Override
    public Optional<ResourceRouter.ResourceMethod> match(UriTemplate.MatchResult result, String httpMethod, String[] mediaTypes, ResourceContext resourceContext, UriInfoBuilder builder) {
        builder.addMatchedResource(resource.apply(resourceContext));
        builder.addMatchedPathParameters(result.getMatchedPathParameters());
        String remaining = Optional.ofNullable(result.getRemaining()).orElse("");
        return resourceMethods.findResourceMethods(remaining, httpMethod)
                .or(() ->
                        subResourceLocators.findResourceMethods(remaining, httpMethod, mediaTypes, resourceContext, builder));
    }

    @Override
    public UriTemplate getUriTemplate() {
        return uriTemplate;
    }

}

