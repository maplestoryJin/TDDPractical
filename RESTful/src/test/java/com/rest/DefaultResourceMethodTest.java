package com.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultResourceMethodTest {

    private CallableResourceMethods resource;
    private ResourceContext resourceContext;
    private UriInfoBuilder uriInfoBuilder;
    private UriInfo uriInfo;
    private MultivaluedHashMap<String, String> parameters;

    private LastCall lastCall;

    @BeforeEach
    void setUp() {
        resource = (CallableResourceMethods) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{CallableResourceMethods.class}, (proxy, method, args) -> {
            lastCall = new LastCall(getMethodName(method.getName(), Arrays.stream(method.getParameters()).map(p -> p.getType()).collect(Collectors.toList())),
                    args != null ? List.of(args) : List.of());
            return "getList".equals(method.getName()) ? new ArrayList<>() : null;
        });
        resourceContext = mock(ResourceContext.class);
        uriInfo = mock(UriInfo.class);
        uriInfoBuilder = mock(UriInfoBuilder.class);

        when(uriInfoBuilder.getLastMatchedResource()).thenReturn(resource);
        when(uriInfoBuilder.createUriInfo()).thenReturn(uriInfo);
        parameters = new MultivaluedHashMap<>();
        when(uriInfo.getPathParameters()).thenReturn(parameters);
    }

    private String getMethodName(String name, List<? extends Class<?>> types) {
        return name + "(" + types.stream().map(Class::getSimpleName).collect(Collectors.joining(",")) + ")";
    }

    @Test
    void should_call_resource_method() throws NoSuchMethodException {
        DefaultResourceMethod method = getResourceMethod("get");
        method.call(resourceContext, uriInfoBuilder);
        assertEquals("get()", lastCall.name);
    }



    @Test
    void should_call_resource_method_with_void_return_type() throws NoSuchMethodException {
        DefaultResourceMethod method = getResourceMethod("post");
        assertNull(method.call(resourceContext, uriInfoBuilder));
    }

    @Test
    void should_use_resource_method_generic_return_type() throws NoSuchMethodException {

        DefaultResourceMethod method = getResourceMethod("getList");

        assertEquals(new GenericEntity<>(List.of(), CallableResourceMethods.class.getMethod("getList").getGenericReturnType()),
                method.call(resourceContext, uriInfoBuilder));
    }

    @TestFactory
    public List<DynamicTest> injectableTypes() {
        List<DynamicTest> tests = new ArrayList<>();

        List<InjectableTypeTestCase> typeCases = List.of(
                new InjectableTypeTestCase(String.class, "string", "string"),
                new InjectableTypeTestCase(double.class, "3.5", 3.5),
                new InjectableTypeTestCase(int.class, "1", 1)
        );

        List<String> paramTypes = List.of("getPathParam", "getQueryParam");
        for (final String type : paramTypes) {
            for (final InjectableTypeTestCase typeCase : typeCases) {
                tests.add(DynamicTest.dynamicTest("should inject " + typeCase.type.getSimpleName() +
                        " to " + type, () -> {
                    verifyResourceMethod(type, typeCase.type, typeCase.string, typeCase.value);
                }));
            }
        }
        return tests;
    }

    private void verifyResourceMethod(String method, Class<?> type, String paramString, Object paramValue) throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod(method, type);
        parameters.put("param", List.of(paramString));
        resourceMethod.call(resourceContext, uriInfoBuilder);

        assertEquals(getMethodName(method, List.of(type)), lastCall.name);
        assertEquals(List.of(paramValue), lastCall.arguments);
    }


    interface CallableResourceMethods {

        @POST
        void post();

        @GET
        String get();

        @GET
        List<String> getList();

        @GET
        String getPathParam(@PathParam("param") String value);

        @GET
        String getPathParam(@PathParam("param") int value);

        @GET
        String getPathParam(@PathParam("param") double value);

        @GET
        String getQueryParam(@QueryParam("param") String value);

        @GET
        String getQueryParam(@QueryParam("param") int value);

        @GET
        String getQueryParam(@QueryParam("param") double value);
    }


    record LastCall(String name, List<Object> arguments) {
    }

    private DefaultResourceMethod getResourceMethod(String name, Class<?>... types) throws NoSuchMethodException {
        return new DefaultResourceMethod(CallableResourceMethods.class.getMethod(name, types));
    }

    // TODO using default converters for path, query, matrix(uri) form, header, cookie (request)
    // TODO default converters for int, short, float, double, byte, char, string and boolean
    // TODO default converters for class with converter constructor
    // TODO default converters for class with converter factory
    // TODO default converters for List, Set, SortSet
    // TODO injection - get injectable from resource context
    // TODO injection - can inject from resource itself
    // TODO injection - can inject uri info from uri info builder

    record InjectableTypeTestCase(Class<?> type, String string, Object value) {

    }
}