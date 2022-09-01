package com.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceMethodsTest {

    @ParameterizedTest(name = "{3}")
    @CsvSource(textBlock = """
            GET,       /messages/hello,        Messages.hello,        GET and URI match
            POST,      /messages/hello,        Messages.postHello,    POST and URI match
            PUT,       /messages/hello,        Messages.putHello,     PUT and URI match
            DELETE,    /messages/hello,        Messages.deleteHello,  DELETE and URI match
            PATCH,     /messages/hello,        Messages.patchHello,   PATCH and URI match
            HEAD,      /messages/hello,        Messages.headHello,    HEAD and URI match
            OPTIONS,   /messages/hello,        Messages.optionsHello, OPTIONS and URI match
            GET,       /messages/topic/1234,   Messages.topic1234,    GET with multiply choices
            GET,       /messages,              Messages.get,          GET with resource method without Path
            HEAD,      /messages/head,         Messages.getHead,      HEAD with GET resource method 
            """)
    public void should_match_resource_method_in_root_resource(String httpMethod, String path, String resourceMethod, String context) {
        ResourceMethods resourceMethods = new ResourceMethods(Messages.class.getMethods());
        UriTemplate.MatchResult result = new PathTemplate("/messages").match(path).get();
        String remaining = result.getRemaining() == null ? "" : result.getRemaining();
        ResourceRouter.ResourceMethod method = resourceMethods.findResourceMethods(remaining, httpMethod).get();

        assertEquals(resourceMethod, method.toString());
    }

    @ParameterizedTest(name = "{2}")
    @CsvSource(textBlock = """
            GET,    /missing.messages/1,       URI not matched
            POST,   /missing.messages,       Http method not matched
            """)
    public void should_return_empty_if_not_matched(String httpMethod, String uri, String context) {
        ResourceMethods resourceMethods = new ResourceMethods(Messages.class.getMethods());
        UriTemplate.MatchResult result = new PathTemplate("/missing.messages").match(uri).get();
        String remaining = result.getRemaining() == null ? "" : result.getRemaining();
        assertTrue(resourceMethods.findResourceMethods(remaining, httpMethod).isEmpty());
    }

    @Test
    void should_convert_get_http_method_to_head_http_method() {
        ResourceMethods resourceMethods = new ResourceMethods(Messages.class.getMethods());
        UriTemplate.MatchResult result = new PathTemplate("/messages").match("/messages/head").get();
        ResourceRouter.ResourceMethod method = resourceMethods.findResourceMethods(result.getRemaining(), "HEAD").get();
        assertInstanceOf(HeadResourceMethod.class, method);
    }

    @Test
    void should_get_options_for_given_uri() {
        RuntimeDelegate delegate = mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);
        when(delegate.createResponseBuilder()).thenReturn(new StubResponseBuilder());
        ResourceContext resourceContext = mock(ResourceContext.class);
        UriInfoBuilder uriInfoBuilder = mock(UriInfoBuilder.class);
        ResourceMethods resourceMethods = new ResourceMethods(Messages.class.getMethods());
        UriTemplate.MatchResult result = new PathTemplate("/messages").match("/messages/head").get();
        ResourceRouter.ResourceMethod method = resourceMethods.findResourceMethods(result.getRemaining(), "OPTIONS").get();

        GenericEntity<?> entity = method.call(resourceContext, uriInfoBuilder);
        Response response = (Response) entity.getEntity();

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS), response.getAllowedMethods());

    }

    @Test
    void should_not_include_head_in_options_if_given_uri_not_have_get_method() {
        RuntimeDelegate delegate = mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);
        when(delegate.createResponseBuilder()).thenReturn(new StubResponseBuilder());
        ResourceContext resourceContext = mock(ResourceContext.class);
        UriInfoBuilder uriInfoBuilder = mock(UriInfoBuilder.class);
        ResourceMethods resourceMethods = new ResourceMethods(Messages.class.getMethods());
        UriTemplate.MatchResult result = new PathTemplate("/messages").match("/messages/no-head").get();
        ResourceRouter.ResourceMethod method = resourceMethods.findResourceMethods(result.getRemaining(), "OPTIONS").get();

        GenericEntity<?> entity = method.call(resourceContext, uriInfoBuilder);
        Response response = (Response) entity.getEntity();

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(Set.of(HttpMethod.POST, HttpMethod.OPTIONS), response.getAllowedMethods());

    }

    @Path("/messages")
    static class Messages {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "messages";
        }


        @GET
        @Path("/head")
        @Produces(MediaType.TEXT_PLAIN)
        public String getHead() {
            return "head";
        }

        @POST
        @Path("/no-head")
        @Produces(MediaType.TEXT_PLAIN)
        public void postNoHead() {
        }

        @GET
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "hello";
        }

        @POST
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String postHello() {
            return "hello";
        }

        @PUT
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String putHello() {
            return "hello";
        }

        @DELETE
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String deleteHello() {
            return "hello";
        }

        @PATCH
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String patchHello() {
            return "hello";
        }


        @HEAD
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String headHello() {
            return "hello";
        }


        @OPTIONS
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String optionsHello() {
            return "hello";
        }

        @GET
        @Path("/topic/{id}")
        @Produces(MediaType.TEXT_PLAIN)
        public String topicId() {
            return "topicId";
        }

        @GET
        @Path("/topic/1234")
        @Produces(MediaType.TEXT_PLAIN)
        public String topic1234() {
            return "topicId";
        }
    }

}
