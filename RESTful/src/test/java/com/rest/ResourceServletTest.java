package com.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.*;
import org.mockito.stubbing.OngoingStubbing;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
        response = response();
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

    @Nested
    class RespondForOutboundResponse {


        @Test
        void should_use_status_code_from_response() {
            response.status(Response.Status.NOT_MODIFIED).returnFrom(resourceRouter);
            HttpResponse httpResponse = get("/test");

            assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
        }

        @Test
        void should_use_headers_from_response() {
            response.headers("Set-Cookie", new NewCookie.Builder("SESSION_ID").value("session").build(), new NewCookie.Builder("USER_ID").value("user").build()).returnFrom(resourceRouter);
            HttpResponse httpResponse = get("/test");
            assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"}, httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));

        }

        @Test
        void should_writer_entity_to_http_response_using_message_body_writer() {

            response.entity(new GenericEntity<>("entity", String.class), new Annotation[0]).returnFrom(resourceRouter);
            HttpResponse httpResponse = get("/test");
            assertEquals("entity", httpResponse.body());
        }


        @Test
        void should_not_call_message_body_writer_if_entity_is_null() {

            response.entity(null, new Annotation[0]).returnFrom(resourceRouter);
            HttpResponse httpResponse = get("/test");
            assertEquals(Response.Status.OK.getStatusCode(), httpResponse.statusCode());
            assertEquals("", httpResponse.body());
        }
    }


    @TestFactory
    public List<DynamicTest> RespondWhenExtensionMissing() {
        List<DynamicTest> tests = new ArrayList<>();
        Map<String, org.junit.jupiter.api.function.Executable> extensions = Map.of("MessageBodyWriter", () -> response().entity(new GenericEntity<>(1, Integer.class), new Annotation[0]).returnFrom(resourceRouter),
                "HeaderDelegate", () -> response().headers(HttpHeaders.DATE, new Date()).returnFrom(resourceRouter),
                "ExceptionMapper", () -> when(resourceRouter.dispatch(any(), eq(resourceContext))).thenThrow(IllegalStateException.class));
        for (String name : extensions.keySet()) {
            tests.add(DynamicTest.dynamicTest(name + " not found", () -> {
                extensions.get(name).execute();
                when(providers.getExceptionMapper(eq(NullPointerException.class))).thenReturn(e -> response().status(Response.Status.INTERNAL_SERVER_ERROR).build());
                HttpResponse httpResponse = get("/test");
                assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), httpResponse.statusCode());
            }));
        }
        return tests;
    }


    @TestFactory
    public List<DynamicTest> respondForException() {
        List<DynamicTest> tests = new ArrayList<>();

        Map<String, Consumer<Consumer<RuntimeException>>> exceptions = Map.of("WebApplicationException", this::webApplicationExceptionThrownFrom,
                "OtherException", this::otherExceptionThrownFrom);
        Map<String, Consumer<RuntimeException>> callers = getCallers();

        for (Map.Entry<String, Consumer<RuntimeException>> caller : callers.entrySet()) {
            for (Map.Entry<String, Consumer<Consumer<RuntimeException>>> exceptionThrownFrom : exceptions.entrySet()) {
                tests.add(DynamicTest.dynamicTest(caller.getKey() + " thrown " + exceptionThrownFrom.getKey(), () -> exceptionThrownFrom.getValue().accept(caller.getValue())));
            }
        }
        return tests;
    }


    private void webApplicationExceptionThrownFrom(Consumer<RuntimeException> caller) {
        RuntimeException exception = new WebApplicationException(response.status(Response.Status.FORBIDDEN).build());
        caller.accept(exception);

        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    private void otherExceptionThrownFrom(Consumer<RuntimeException> caller) {
        RuntimeException exception = new IllegalArgumentException();
        caller.accept(exception);

        when(providers.getExceptionMapper(eq(IllegalArgumentException.class))).thenReturn(exception1 -> response.status(Response.Status.FORBIDDEN).build());
        HttpResponse httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }


    @Retention(RetentionPolicy.RUNTIME)
    @interface ExceptionThrownFrom {


    }

    @ExceptionThrownFrom
    void resourceRouter_dispatch(RuntimeException exception) {
        when(resourceRouter.dispatch(any(), eq(resourceContext))).thenThrow(exception);
    }


    @ExceptionThrownFrom
    private void providers_getExceptionMapper(RuntimeException exception) {
        when(resourceRouter.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenThrow(exception);
    }

    @ExceptionThrownFrom
    private void exceptionMapper_toResponse(RuntimeException exception) {
        when(resourceRouter.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenReturn(ex -> {
            throw exception;
        });
    }


    @ExceptionThrownFrom
    private void runtimeDelegate_createHeaderDelegate(RuntimeException exception) {
        response().headers(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_TYPE).returnFrom(resourceRouter);
        when(RuntimeDelegate.getInstance().createHeaderDelegate(eq(MediaType.class))).thenThrow(exception);
    }

    @ExceptionThrownFrom
    private void headerDelegate_toString(RuntimeException exception) {
        response().headers(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_TYPE).returnFrom(resourceRouter);
        when(RuntimeDelegate.getInstance().createHeaderDelegate(eq(MediaType.class))).thenReturn(new RuntimeDelegate.HeaderDelegate<>() {
            @Override
            public MediaType fromString(String value) {
                return null;
            }

            @Override
            public String toString(MediaType value) {
                throw exception;
            }
        });
    }

    @ExceptionThrownFrom
    private void providers_getMessageBodyWriter(RuntimeException exception) {
        response().status(Response.Status.OK).entity(new GenericEntity<>(2.5, Double.class), new Annotation[0]).returnFrom(resourceRouter);
        when(providers.getMessageBodyWriter(eq(Double.class), eq(Double.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE)))
                .thenThrow(exception);
    }

    @ExceptionThrownFrom
    private void messageBodyWriter_writeTo(RuntimeException exception) {
        response().status(Response.Status.OK).entity(new GenericEntity<>(2.5, Double.class), new Annotation[0]).returnFrom(resourceRouter);
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
    }


    private OutBoundResponseBuilder response() {
        return new OutBoundResponseBuilder();
    }

    private Map<String, Consumer<RuntimeException>> getCallers() {
        Map<String, Consumer<RuntimeException>> callers = new HashMap<>();
        for (final Method method : Arrays.stream(this.getClass().getDeclaredMethods()).filter(m -> m.isAnnotationPresent(ExceptionThrownFrom.class)).collect(Collectors.toList())) {
            String name = method.getName();
            String testName = name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", ".");
            callers.put(testName, (exception) -> {
                try {
                    method.invoke(this, exception);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            });

        }
        return callers;
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
            stubMessageBodyWriter();
            return response;
        }

        private OngoingStubbing<MessageBodyWriter<String>> stubMessageBodyWriter() {
            return when(providers.getMessageBodyWriter(eq(String.class), eq(String.class), same(annotations), eq(mediaType)))
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
    }
}
