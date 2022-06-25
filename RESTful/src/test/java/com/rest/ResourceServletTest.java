package com.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;
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
    private OutBoundResponseBuilder reponse;

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
        reponse = new OutBoundResponseBuilder();
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
        reponse.status(Response.Status.NOT_MODIFIED).returnFrom(resourceRouter);
        HttpResponse httpResponse = get("/test");

        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    void should_use_headers_from_response() throws Exception {
        reponse.headers("Set-Cookie", new NewCookie.Builder("SESSION_ID").value("session").build(), new NewCookie.Builder("USER_ID").value("user").build()).returnFrom(resourceRouter);
        HttpResponse httpResponse = get("/test");
        assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"}, httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));

    }

    @Test
    void should_writer_entity_to_http_response_using_message_body_writer() throws Exception {

        reponse.entity(new GenericEntity<>("entity", String.class), new Annotation[0]).returnFrom(resourceRouter);
        HttpResponse httpResponse = get("/test");
        assertEquals("entity", httpResponse.body());
    }

    // TODO: throw WebException with response, use response

    @Test
    @Disabled
    void should_use_response_from_web_application_exception() {
//        builder.status(Response.Status.FORBIDDEN).build(resourceRouter);

    }

    // TODO: throw WebException with null response, use ExceptionMapper build response
    // TODO: throw other Exception, use ExceptionMapper build response


    // TODO: 500 if MessageBodyWriter not found
    class OutBoundResponseBuilder {
        private Response.Status status = Response.Status.OK;
        private MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        private GenericEntity<String> entity = new GenericEntity<>("entity", String.class);
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

        OutBoundResponseBuilder entity(GenericEntity<String> entity, Annotation[] annotations) {
            this.entity = entity;
            this.annotations = annotations;
            return this;
        }

        void returnFrom(ResourceRouter router) {
            builder(response -> when(router.dispatch(any(), eq(resourceContext))).thenReturn(response));

        }
        void throwFrom(ResourceRouter router) {
            builder(response -> when(router.dispatch(any(), eq(resourceContext))).thenThrow(WebApplicationException.class));

        }
        void builder(Consumer<OutBoundResponse> consumer) {
            OutBoundResponse response = mock(OutBoundResponse.class);
            when(response.getStatus()).thenReturn(status.getStatusCode());
            when(response.getHeaders()).thenReturn(headers);
            when(response.getGenericEntity()).thenReturn(entity);
            when(response.getAnnotations()).thenReturn(annotations);
            when(response.getMediaType()).thenReturn(mediaType);
            consumer.accept(response);
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
        }
    }
}
