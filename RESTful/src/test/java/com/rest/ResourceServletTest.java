package com.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
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

    private Providers providers;

    @Override
    protected Servlet getServlet() {
        runtime = mock(Runtime.class);
        resourceRouter = mock(ResourceRouter.class);
        resourceContext = mock(ResourceContext.class);
        providers = mock(Providers.class);
        when(runtime.createResourceContext(any(), any())).thenReturn(resourceContext);
        when(runtime.getResourceRouter()).thenReturn(resourceRouter);
        when(runtime.getProviders()).thenReturn(providers);
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
        when(providers.getMessageBodyWriter(eq(String.class), eq(String.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE)))
                .thenReturn(new MessageBodyWriter<>() {
                    @Override
                    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                        return false;
                    }

                    @Override
                    public void writeTo(String o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
                        PrintWriter writer = new PrintWriter(entityStream);
                        writer.write(o);
                        writer.flush();
                    }
                });
    }

    @Test
    void should_use_status_code_from_response() throws Exception {
        response(Response.Status.NOT_MODIFIED.getStatusCode(), new MultivaluedHashMap<>(), new GenericEntity<>("entity", String.class), new Annotation[0], MediaType.TEXT_PLAIN_TYPE);
        HttpResponse httpResponse = get("/test");

        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    void should_use_headers_from_response() throws Exception {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.addAll("Set-Cookie", new NewCookie.Builder("SESSION_ID").value("session").build(), new NewCookie.Builder("USER_ID").value("user").build());
        response(Response.Status.NOT_MODIFIED.getStatusCode(), headers, new GenericEntity<>("entity", String.class), new Annotation[0], MediaType.TEXT_PLAIN_TYPE);
        HttpResponse httpResponse = get("/test");
        assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"}, httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));

    }

    @Test
    void should_writer_entity_to_http_response_using_message_body_writer() throws Exception {
        GenericEntity<String> entity = new GenericEntity<>("entity", String.class);
        response(Response.Status.OK.getStatusCode(), new MultivaluedHashMap<>(), entity, new Annotation[0], MediaType.TEXT_PLAIN_TYPE);
        HttpResponse httpResponse = get("/test");
        assertEquals("entity", httpResponse.body());
    }

    // TODO: writer body using MessageBodyWriter
    // TODO: 500 if MessageBodyWriter not found
    // TODO: throw WebException with response, use response
    // TODO: throw WebException with null response, use ExceptionMapper build response
    // TODO: throw other Exception, use ExceptionMapper build response



    private void response(int statusCode, MultivaluedHashMap<String, Object> headers, GenericEntity<String> entity, Annotation[] annotations, MediaType mediaType) {
        OutBoundResponse response = mock(OutBoundResponse.class);
        when(response.getStatus()).thenReturn(statusCode);
        when(response.getHeaders()).thenReturn(headers);
        when(response.getGenericEntity()).thenReturn(entity);
        when(response.getAnnotations()).thenReturn(annotations);
        when(response.getMediaType()).thenReturn(mediaType);
        when(resourceRouter.dispatch(any(), eq(resourceContext))).thenReturn(response);
    }
}
