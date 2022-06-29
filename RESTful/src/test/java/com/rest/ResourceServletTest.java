package com.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.ExceptionMapper;
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
import java.util.Date;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceServletTest extends ServletTest {
    private Runtime runtime;
    private ResourceRouter resourceRouter;
    private ResourceContext resourceContext;

    private Providers providers;
    private OutBoundResponseBuilder response;

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
        response = new OutBoundResponseBuilder();
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
        response.status(Response.Status.NOT_MODIFIED).returnFrom(resourceRouter);
        HttpResponse httpResponse = get("/test");

        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    void should_use_headers_from_response() throws Exception {
        response.headers("Set-Cookie", new NewCookie.Builder("SESSION_ID").value("session").build(), new NewCookie.Builder("USER_ID").value("user").build()).returnFrom(resourceRouter);
        HttpResponse httpResponse = get("/test");
        assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"}, httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));

    }

    @Test
    void should_writer_entity_to_http_response_using_message_body_writer() throws Exception {

        response.entity(new GenericEntity<>("entity", String.class), new Annotation[0]).returnFrom(resourceRouter);
        HttpResponse httpResponse = get("/test");
        assertEquals("entity", httpResponse.body());
    }

    @Test
    void should_use_response_from_web_application_exception() throws Exception {
        response.status(Response.Status.FORBIDDEN)
                .headers(HttpHeaders.SET_COOKIE, new NewCookie.Builder("SESSION_ID").value("session").build())
                .entity(new GenericEntity<>("error", String.class), new Annotation[0])
                .throwFrom(resourceRouter);
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
        assertArrayEquals(new String[]{"SESSION_ID=session"}, httpResponse.headers().allValues(HttpHeaders.SET_COOKIE).toArray(String[]::new));
        assertEquals("error", httpResponse.body());

    }

    @Test
    void should_build_response_by_exception_mapper_if_throw_runtime_exception() throws Exception {
        when(resourceRouter.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenReturn(exception -> response.status(Response.Status.FORBIDDEN).build());
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }


    @Test
    void should_not_call_message_body_writer_if_entity_is_null() throws Exception {

        response.entity(null, new Annotation[0]).returnFrom(resourceRouter);
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.OK.getStatusCode(), httpResponse.statusCode());
        assertEquals("", httpResponse.body());
    }

    @Test
    void should_respond_with_internal_server_error_if_no_message_body_writer() throws Exception {
        new OutBoundResponseBuilder().entity(new GenericEntity<>(1, Integer.class), new Annotation[0]).returnFrom(resourceRouter);
        when(providers.getExceptionMapper(eq(NullPointerException.class))).thenReturn(exception -> response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    void should_respond_with_internal_server_error_if_no_header_delegate() throws Exception {
        new OutBoundResponseBuilder().headers(HttpHeaders.DATE, new Date()).returnFrom(resourceRouter);
        when(providers.getExceptionMapper(eq(NullPointerException.class))).thenReturn(exception -> response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    void should_respond_with_internal_server_error_if_no_exception_mapper() throws Exception {
        when(resourceRouter.dispatch(any(), eq(resourceContext))).thenThrow(IllegalStateException.class);
        when(providers.getExceptionMapper(eq(NullPointerException.class))).thenReturn(exception -> response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), httpResponse.statusCode());
    }


    @Test
    void should_use_response_from_web_application_exception_thrown_by_exception_mapper() throws Exception {
        when(resourceRouter.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenReturn(exception -> {
            throw new WebApplicationException(response.status(Response.Status.FORBIDDEN).build());
        });
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());

    }

    @Test
    void should_map_exception_thrown_by_exception_mapper() throws Exception {
        when(resourceRouter.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenReturn(exception -> {
            throw new IllegalArgumentException();
        });
        when(providers.getExceptionMapper(eq(IllegalArgumentException.class))).thenReturn(exception -> response.status(Response.Status.FORBIDDEN).build());
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }


    @Test
    void should_map_exception_thrown_by_provider_gets_exception_mapper() throws Exception {
        when(resourceRouter.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenThrow(IllegalArgumentException.class);
        when(providers.getExceptionMapper(eq(IllegalArgumentException.class))).thenReturn(exception -> response.status(Response.Status.FORBIDDEN).build());
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());

    }

    @Test
    void should_use_response_from_web_application_exception_thrown_by_provider_gets_exception_mapper() throws Exception {
        WebApplicationException exception = new WebApplicationException(response.status(Response.Status.FORBIDDEN).build());
        when(resourceRouter.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenThrow(exception);
        when(providers.getExceptionMapper(eq(WebApplicationException.class))).thenReturn(e -> response.status(Response.Status.FORBIDDEN).build());
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    void should_use_response_from_web_application_exception_thrown_by_runtime_delegate() throws Exception {
        new OutBoundResponseBuilder().headers(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_TYPE).returnFrom(resourceRouter);
        WebApplicationException exception = new WebApplicationException(response.status(Response.Status.FORBIDDEN).build());
        when(RuntimeDelegate.getInstance().createHeaderDelegate(eq(MediaType.class))).thenThrow(exception);
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    void should_map_exception_thrown_by_runtime_delegate() throws Exception {
        new OutBoundResponseBuilder().headers(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_TYPE).returnFrom(resourceRouter);
        when(RuntimeDelegate.getInstance().createHeaderDelegate(eq(MediaType.class))).thenThrow(IllegalArgumentException.class);
        when(providers.getExceptionMapper(eq(IllegalArgumentException.class))).thenReturn(exception ->
                response.status(Response.Status.FORBIDDEN).build());
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    void should_use_response_from_web_application_exception_thrown_by_header_delegate_to_string() throws Exception {
        new OutBoundResponseBuilder().headers(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_TYPE).returnFrom(resourceRouter);
        when(RuntimeDelegate.getInstance().createHeaderDelegate(eq(MediaType.class))).thenReturn(new RuntimeDelegate.HeaderDelegate<>() {
            @Override
            public MediaType fromString(String value) {
                return null;
            }

            @Override
            public String toString(MediaType value) {
                throw new WebApplicationException(response.status(Response.Status.FORBIDDEN).build());
            }
        });
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());

    }

    @Test
    void should_map_exception_thrown_by_header_delegate_to_string() throws Exception {
        new OutBoundResponseBuilder().headers(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_TYPE).returnFrom(resourceRouter);
        when(RuntimeDelegate.getInstance().createHeaderDelegate(eq(MediaType.class))).thenReturn(new RuntimeDelegate.HeaderDelegate<>() {
            @Override
            public MediaType fromString(String value) {
                return null;
            }

            @Override
            public String toString(MediaType value) {
                throw new IllegalArgumentException();
            }
        });
        when(providers.getExceptionMapper(eq(IllegalArgumentException.class))).thenReturn(exception -> response.status(Response.Status.FORBIDDEN).build());
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());

    }

    @Test
    void should_use_response_from_web_application_exception_thrown_by_provider_get_message_body_writer() throws Exception {
        WebApplicationException exception = new WebApplicationException(response.status(Response.Status.FORBIDDEN).build());
        new OutBoundResponseBuilder().status(Response.Status.OK).entity(new GenericEntity<>(2.5, Double.class), new Annotation[0]).returnFrom(resourceRouter);
        when(providers.getMessageBodyWriter(eq(Double.class), eq(Double.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE)))
                .thenThrow(exception);
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());

    }

    @Test
    void should_map_exception_thrown_by_provider_get_message_body_writer() throws Exception {
        new OutBoundResponseBuilder().status(Response.Status.OK).entity(new GenericEntity<>(2.5, Double.class), new Annotation[0]).returnFrom(resourceRouter);
        when(providers.getMessageBodyWriter(eq(Double.class), eq(Double.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE)))
                .thenThrow(IllegalArgumentException.class);
        when(providers.getExceptionMapper(eq(IllegalArgumentException.class))).thenReturn(exception -> response.status(Response.Status.FORBIDDEN).build());
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    void should_use_response_from_web_application_exception_thrown_by_provider_get_message_body_writer_write() throws Exception {
        WebApplicationException exception = new WebApplicationException(response.status(Response.Status.FORBIDDEN).build());
        new OutBoundResponseBuilder().status(Response.Status.OK).entity(new GenericEntity<>(2.5, Double.class), new Annotation[0]).returnFrom(resourceRouter);
        when(providers.getMessageBodyWriter(eq(Double.class), eq(Double.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE)))
                .thenReturn(new MessageBodyWriter<>() {
                    @Override
                    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                        return false;
                    }

                    @Override
                    public void writeTo(Double aDouble, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
                        throw exception;
                    }
                });
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    void should_map_exception_thrown_by_provider_get_message_body_writer_write() throws Exception {
        new OutBoundResponseBuilder().status(Response.Status.OK).entity(new GenericEntity<>(2.5, Double.class), new Annotation[0]).returnFrom(resourceRouter);
        when(providers.getMessageBodyWriter(eq(Double.class), eq(Double.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE)))
                .thenReturn(new MessageBodyWriter<>() {
                    @Override
                    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                        return false;
                    }

                    @Override
                    public void writeTo(Double aDouble, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
                        throw new IllegalArgumentException();
                    }
                });
        when(providers.getExceptionMapper(eq(IllegalArgumentException.class))).thenReturn(exception -> response.status(Response.Status.FORBIDDEN).build());
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    class OutBoundResponseBuilder {
        private Response.Status status = Response.Status.OK;
        private MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        private GenericEntity<Object> entity = new GenericEntity<>("entity", String.class);
        private Annotation[] annotations = new Annotation[0];
        private MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;

        OutBoundResponseBuilder status(Response.Status status) {
            this.status = status;
            return this;
        }

        OutBoundResponseBuilder headers(String name, Object... values) {
            headers.addAll(name, values);
            return this;
        }

        OutBoundResponseBuilder entity(GenericEntity<Object> entity, Annotation[] annotations) {
            this.entity = entity;
            this.annotations = annotations;
            return this;
        }

        void returnFrom(ResourceRouter router) {
            builder(response -> when(router.dispatch(any(), eq(resourceContext))).thenReturn(response));

        }

        void throwFrom(ResourceRouter router) {
            builder(response -> {
                WebApplicationException exception = new WebApplicationException(response);
                when(router.dispatch(any(), eq(resourceContext))).thenThrow(exception);
            });

        }

        void builder(Consumer<OutBoundResponse> consumer) {
            OutBoundResponse response = build();
            consumer.accept(response);
        }

        OutBoundResponse build() {
            OutBoundResponse response = mock(OutBoundResponse.class);
            when(response.getStatus()).thenReturn(status.getStatusCode());
            when(response.getStatusInfo()).thenReturn(status);
            when(response.getHeaders()).thenReturn(headers);
            when(response.getGenericEntity()).thenReturn(entity);
            when(response.getAnnotations()).thenReturn(annotations);
            when(response.getMediaType()).thenReturn(mediaType);
            when(providers.getMessageBodyWriter(eq(String.class), eq(String.class), same(annotations), eq(mediaType)))
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
            return response;
        }
    }
}
