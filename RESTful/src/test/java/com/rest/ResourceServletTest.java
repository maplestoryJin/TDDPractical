package com.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceServletTest extends ServletTest {
    private Runtime runtime;
    private ResourceRouter resourceRouter;
    private ResourceContext resourceContext;

    @Override
    protected Servlet getServlet() {
        runtime = mock(Runtime.class);
        resourceRouter = mock(ResourceRouter.class);
        resourceContext = mock(ResourceContext.class);
        when(runtime.createResourceContext(any(), any())).thenReturn(resourceContext);
        when(runtime.getResourceRouter()).thenReturn(resourceRouter);
        return new ResourceServlet(runtime);
    }

    @BeforeEach
    void setUp() {
        RuntimeDelegate runtimeDelegate = mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(runtimeDelegate);
        when(runtimeDelegate.createHeaderDelegate(NewCookie.class)).thenReturn(new RuntimeDelegate.HeaderDelegate<>() {
            @Override
            public NewCookie fromString(String value) {
                return null;
            }

            @Override
            public String toString(NewCookie value) {
                return value.getName() + "=" + value.getValue();
            }
        });
    }

    @Test
    void should_use_status_code_from_response() throws Exception {
        response(Response.Status.NOT_MODIFIED.getStatusCode(), new MultivaluedHashMap<>());
        HttpResponse httpResponse = get("/test");

        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    void should_use_headers_from_response() throws Exception {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.addAll("Set-Cookie", new NewCookie.Builder("SESSION_ID").value("session").build(), new NewCookie.Builder("USER_ID").value("user").build());
        response(Response.Status.NOT_MODIFIED.getStatusCode(), headers);
        HttpResponse httpResponse = get("/test");
        assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"}, httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));

    }

    private void response(int statusCode, MultivaluedHashMap<String, Object> headers) {
        OutBoundResponse response = mock(OutBoundResponse.class);
        when(response.getStatus()).thenReturn(statusCode);
        when(response.getHeaders()).thenReturn(headers);
        when(resourceRouter.dispatch(any(), eq(resourceContext))).thenReturn(response);
    }

    // TODO: writer body using MessageBodyWriter
    // TODO: 500 if MessageBodyWriter not found
    // TODO: throw WebException with response, use response
    // TODO: throw WebException with null response, use ExceptionMapper build response
    // TODO: throw other Exception, use ExceptionMapper build response
}
