package com.rest;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.UriInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class MethodInvoker {

    private static ValueProvider pathParam = (parameter, uriInfo) ->
            Optional.ofNullable(parameter.getAnnotation(PathParam.class))
                    .map(annotation -> uriInfo.getPathParameters().get(annotation.value()));
    private static ValueProvider queryParam = (parameter, uriInfo) ->
            Optional.ofNullable(parameter.getAnnotation(QueryParam.class))
                    .map(annotation -> uriInfo.getPathParameters().get(annotation.value()));
    private static List<ValueProvider> providers = List.of(pathParam, queryParam);

    static Object invoke(Method method1, ResourceContext resourceContext, UriInfoBuilder builder) {
        Object result;
        try {
            UriInfo uriInfo = builder.createUriInfo();
            result = method1.invoke(builder.getLastMatchedResource(),
                    Arrays.stream(method1.getParameters()).map(parameter -> injectParameter(parameter, uriInfo)
                            .or(() -> injectContext(parameter, resourceContext, uriInfo))
                            .orElse(null)).collect(Collectors.toList()).toArray(Object[]::new));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private static Optional<Object> injectContext(Parameter parameter, ResourceContext resourceContext, UriInfo uriInfo) {
        if (parameter.getType().equals(ResourceContext.class)) return Optional.of(resourceContext);
        if (parameter.getType().equals(UriInfo.class)) return Optional.of(uriInfo);
        return Optional.of(resourceContext.getResource(parameter.getType()));
    }

    private static Optional<Object> injectParameter(Parameter parameter, UriInfo uriInfo) {
        return providers.stream().map(provider -> provider.provide(parameter, uriInfo))
                .filter(Optional::isPresent)
                .findFirst()
                .flatMap(values -> values.flatMap(it -> convert(parameter.getType(), it)));
    }

    public static Optional<Object> convert(Class<?> converter, List<String> values) {
        return DefaultResourceMethod.PrimitiveConverter.convert(converter, values)
                .or(() -> ConverterConstructor.convert(converter, values.get(0)))
                .or(() -> ConverterFactory.convert(converter, values.get(0)));
    }

    interface ValueProvider {
        Optional<List<String>> provide(Parameter parameter, UriInfo uriInfo);
    }
}
