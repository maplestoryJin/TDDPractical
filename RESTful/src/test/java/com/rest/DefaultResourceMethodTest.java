package com.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


enum Converter {
    Primitive, Constructor, Factory
}


interface SomeServiceInContext {

}

class DefaultResourceMethodTest extends InjectableCallerTest {

    @Override
    protected Object initResource() {
        return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{CallableResourceMethods.class}, (proxy, method, args) -> {
            lastCall = new LastCall(getMethodName(method.getName(), Arrays.stream(method.getParameters()).map(p -> p.getType()).collect(Collectors.toList())),
                    args != null ? List.of(args) : List.of());
            return "getList".equals(method.getName()) ? new ArrayList<>() : null;
        });
    }

    @Test
    void should_call_resource_method() throws NoSuchMethodException {
        DefaultResourceMethod method = getResourceMethod("get");
        method.call(resourceContext, builder);
        assertEquals("get()", lastCall.name());
    }


    @Test
    void should_call_resource_method_with_void_return_type() throws NoSuchMethodException {
        DefaultResourceMethod method = getResourceMethod("post");
        assertNull(method.call(resourceContext, builder));
    }

    @Test
    void should_use_resource_method_generic_return_type() throws NoSuchMethodException {

        DefaultResourceMethod method = getResourceMethod("getList");

        assertEquals(new GenericEntity<>(List.of(), CallableResourceMethods.class.getMethod("getList").getGenericReturnType()),
                method.call(resourceContext, builder));
    }

    @Override
    protected void callInjectable(String method, Class<?> type) throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod(method, type);
        resourceMethod.call(resourceContext, builder);
    }


    private DefaultResourceMethod getResourceMethod(String name, Class<?>... types) throws NoSuchMethodException {
        return new DefaultResourceMethod(CallableResourceMethods.class.getMethod(name, types));
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
        String getPathParam(@PathParam("param") short value);

        @GET
        String getPathParam(@PathParam("param") float value);

        @GET
        String getPathParam(@PathParam("param") boolean value);

        @GET
        String getPathParam(@PathParam("param") byte value);

        @GET
        String getPathParam(@PathParam("param") char value);

        @GET
        String getPathParam(@PathParam("param") BigDecimal value);

        @GET
        String getPathParam(@PathParam("param") Converter value);

        @GET
        String getQueryParam(@QueryParam("param") String value);

        @GET
        String getQueryParam(@QueryParam("param") int value);

        @GET
        String getQueryParam(@QueryParam("param") double value);

        @GET
        String getQueryParam(@QueryParam("param") short value);

        @GET
        String getQueryParam(@QueryParam("param") float value);

        @GET
        String getQueryParam(@QueryParam("param") boolean value);

        @GET
        String getQueryParam(@QueryParam("param") byte value);

        @GET
        String getQueryParam(@QueryParam("param") char value);

        @GET
        String getQueryParam(@QueryParam("param") BigDecimal value);

        @GET
        String getQueryParam(@QueryParam("param") Converter value);

        @GET
        String getContext(@Context SomeServiceInContext service);

        @GET
        String getContext(@Context ResourceContext context);

        @GET
        String getContext(@Context UriInfo uriInfo);
    }

    // TODO using default converters for path, query, matrix(uri) form, header, cookie (request)
    // TODO default converters for List, Set, SortSet

}