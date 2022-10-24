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
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class DefaultResourceMethodTest {

    private CallableResourceMethods resource;
    private ResourceContext resourceContext;
    private UriInfoBuilder uriInfoBuilder;
    private UriInfo uriInfo;
    private MultivaluedHashMap<String, String> parameters;

    @BeforeEach
    void setUp() {
        resource = mock(CallableResourceMethods.class);
        resourceContext = mock(ResourceContext.class);
        uriInfo = mock(UriInfo.class);
        uriInfoBuilder = mock(UriInfoBuilder.class);

        when(uriInfoBuilder.getLastMatchedResource()).thenReturn(resource);
        when(uriInfoBuilder.createUriInfo()).thenReturn(uriInfo);
        parameters = new MultivaluedHashMap<>();
        when(uriInfo.getPathParameters()).thenReturn(parameters);
    }

    @Test
    void should_call_resource_method() throws NoSuchMethodException {
        when(resource.get()).thenReturn("resource called");
        DefaultResourceMethod method = getResourceMethod("get");
        assertEquals(new GenericEntity<>("resource called", String.class), method.call(resourceContext, uriInfoBuilder));
    }

    @Test
    void should_call_resource_method_with_void_return_type() throws NoSuchMethodException {
        DefaultResourceMethod method = getResourceMethod("post");
        assertNull(method.call(resourceContext, uriInfoBuilder));
    }

    @Test
    void should_use_resource_method_generic_return_type() throws NoSuchMethodException {
        when(resource.getList()).thenReturn(List.of());

        DefaultResourceMethod method = getResourceMethod("getList");

        assertEquals(new GenericEntity<>(List.of(), CallableResourceMethods.class.getMethod("getList").getGenericReturnType()),
                method.call(resourceContext, uriInfoBuilder));
    }

    @Test
    void should_inject_string_to_path_param() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("getPathParam", String.class);

        parameters.put("path", List.of("path"));
        resourceMethod.call(resourceContext, uriInfoBuilder);

        verify(resource).getPathParam(eq("path"));
    }

    @Test
    void should_inject_int_to_path_param() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("getPathParam", int.class);

        parameters.put("path", List.of("1"));
        resourceMethod.call(resourceContext, uriInfoBuilder);

        verify(resource).getPathParam(eq(1));
    }

    @Test
    void should_inject_string_to_query_param() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("getQueryParam", String.class);

        parameters.put("query", List.of("query"));
        resourceMethod.call(resourceContext, uriInfoBuilder);

        verify(resource).getQueryParam(eq("query"));
    }

    @Test
    void should_inject_int_to_query_param() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("getQueryParam", int.class);

        parameters.put("query", List.of("1"));
        resourceMethod.call(resourceContext, uriInfoBuilder);

        verify(resource).getQueryParam(eq(1));
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

    interface CallableResourceMethods {

        @POST
        void post();

        @GET
        String get();

        @GET
        List<String> getList();

        @GET
        String getPathParam(@PathParam("path") String value);

        @GET
        String getPathParam(@PathParam("path") int value);

        @GET
        String getQueryParam(@QueryParam("query") String value);

        @GET
        String getQueryParam(@QueryParam("query") int value);
    }
}