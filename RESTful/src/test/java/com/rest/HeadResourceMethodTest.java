package com.rest;

import jakarta.ws.rs.container.ResourceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

class HeadResourceMethodTest {

    private ResourceRouter.ResourceMethod method;
    private ResourceContext resourceContext;
    private UriInfoBuilder uriInfoBuilder;

    @BeforeEach
    void setUp() {

        method = mock(ResourceRouter.ResourceMethod.class);
        resourceContext = mock(ResourceContext.class);
        uriInfoBuilder = mock(UriInfoBuilder.class);
    }

    @Test
    void should_call_method_and_ignore_return_value() {
        HeadResourceMethod headResourceMethod = new HeadResourceMethod(method);

        headResourceMethod.call(resourceContext, uriInfoBuilder);

        verify(method).call(same(resourceContext), same(uriInfoBuilder));
    }

    @Test
    void should_delegate_to_method_for_uri_template() {
        HeadResourceMethod headResourceMethod = new HeadResourceMethod(method);

        UriTemplate uriTemplate = mock(UriTemplate.class);
        when(method.getUriTemplate()).thenReturn(uriTemplate);

        assertEquals(uriTemplate, headResourceMethod.getUriTemplate());
    }

    @Test
    void should_delegate_to_method_for_http_method() {

        HeadResourceMethod headResourceMethod = new HeadResourceMethod(method);

        when(method.getHttpMethod()).thenReturn("GET");

        assertEquals("HEAD", headResourceMethod.getHttpMethod());
    }
}