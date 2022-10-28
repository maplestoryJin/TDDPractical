package com.rest;

import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class InjectableCallerTest {
    protected ResourceContext resourceContext;
    protected UriInfoBuilder builder;
    protected UriInfo uriInfo;
    protected MultivaluedHashMap<String, String> parameters;
    protected LastCall lastCall;
    protected SomeServiceInContext service;
    protected Object resource;

    @BeforeEach
    void setUp() {
        resource = initResource();
        resourceContext = mock(ResourceContext.class);
        uriInfo = mock(UriInfo.class);
        builder = mock(UriInfoBuilder.class);
        service = mock(SomeServiceInContext.class);

        when(builder.getLastMatchedResource()).thenReturn(resource);
        when(builder.createUriInfo()).thenReturn(uriInfo);
        parameters = new MultivaluedHashMap<>();
        when(uriInfo.getPathParameters()).thenReturn(parameters);
        when(resourceContext.getResource(eq(SomeServiceInContext.class))).thenReturn(service);
    }

    protected String getMethodName(String name, List<? extends Class<?>> types) {
        return name + "(" + types.stream().map(Class::getSimpleName).collect(Collectors.joining(",")) + ")";
    }

    @TestFactory
    public List<DynamicTest> injectable_convertible_types() {
        List<DynamicTest> tests = new ArrayList<>();

        List<InjectableTypeTestCase> typeCases = List.of(
                new InjectableTypeTestCase(String.class, "string", "string"),
                new InjectableTypeTestCase(double.class, "3.5", 3.5),
                new InjectableTypeTestCase(float.class, "2.5f", 2.5f),
                new InjectableTypeTestCase(short.class, "2", (short) 2),
                new InjectableTypeTestCase(boolean.class, "true", true),
                new InjectableTypeTestCase(byte.class, "-1", ((byte) -1)),
                new InjectableTypeTestCase(char.class, "c", 'c'),
                new InjectableTypeTestCase(int.class, "1", 1),
                new InjectableTypeTestCase(Converter.class, "Factory", Converter.Factory),
                new InjectableTypeTestCase(BigDecimal.class, "12345", new BigDecimal("12345"))
        );

        List<String> paramTypes = List.of("getPathParam", "getQueryParam");
        for (final String type : paramTypes) {
            for (final InjectableTypeTestCase typeCase : typeCases) {
                Optional<Method> existMethod = Arrays.stream(DefaultResourceMethodTest.CallableResourceMethods.class.getMethods()).filter(m -> m.getName().equals(type))
                        .filter(m -> Arrays.stream(m.getParameterTypes()).toList().equals(List.of(typeCase.type)))
                        .findFirst();
                existMethod.ifPresent(m -> tests.add(DynamicTest.dynamicTest("should inject " + typeCase.type.getSimpleName() +
                        " to " + type, () -> {
                    verifyResourceMethod(type, typeCase.type, typeCase.string, typeCase.value);
                })));
            }
        }
        return tests;
    }

    @TestFactory
    public List<DynamicTest> injectable_context_object() {

        List<DynamicTest> tests = new ArrayList<>();

        List<InjectableTypeTestCase> typeCases = List.of(
                new InjectableTypeTestCase(SomeServiceInContext.class, "N/A", service),
                new InjectableTypeTestCase(ResourceContext.class, "N/A", resourceContext),
                new InjectableTypeTestCase(UriInfo.class, "N/A", uriInfo));
        for (final InjectableTypeTestCase typeCase : typeCases) {
            tests.add(DynamicTest.dynamicTest("should inject " + typeCase.type.getSimpleName() +
                    " to " + "getContext", () -> verifyResourceMethod("getContext", typeCase.type, typeCase.string, typeCase.value)));
        }
        return tests;

    }

    private void verifyResourceMethod(String method, Class<?> type, String paramString, Object paramValue) throws NoSuchMethodException {
        parameters.put("param", List.of(paramString));


        callInjectable(method, type);

        assertEquals(getMethodName(method, List.of(type)), lastCall.name());
        assertEquals(List.of(paramValue), lastCall.arguments());
    }

    protected abstract void callInjectable(String method, Class<?> type) throws NoSuchMethodException;

    protected abstract Object initResource();

    record LastCall(String name, List<Object> arguments) {
    }

    record InjectableTypeTestCase(Class<?> type, String string, Object value) {
    }
}
