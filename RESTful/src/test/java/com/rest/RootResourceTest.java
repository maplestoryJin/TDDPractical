package com.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RootResourceTest {
    @Test
    void should_get_uri_template_from_path_annotation() {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate uriTemplate = resource.getUriTemplate();
        assertTrue(uriTemplate.match("/messages/hello").isPresent());
    }

    // TODO find resource method, matches the http request and http method



    @ParameterizedTest(name = "{3}")
    @CsvSource(textBlock = """
            GET,       /messages,        Messages.get,        map to resource methods
            """)
    public void should_match_resource_method_in_root_resource(String httpMethod, String path, String resourceMethod, String context) {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate.MatchResult result = resource.getUriTemplate().match(path).get();
        ResourceRouter.ResourceMethod method = resource.match(result, httpMethod, new String[]{MediaType.TEXT_PLAIN}, mock(UriInfoBuilder.class)).get();

        assertEquals(resourceMethod, method.toString());
    }

    @Test
    void should_match_resource_method_in_sub_resource() {
        SubResource subResource = new SubResource(new Message());
        UriTemplate.MatchResult result = mock(UriTemplate.MatchResult.class);
        when(result.getRemaining()).thenReturn("/content");
        assertTrue(subResource.match(result, "GET", new String[]{MediaType.TEXT_PLAIN}, mock(UriInfoBuilder.class)).isPresent());
    }

    // TODO if sub resource locator matches uri, using it to do follow up matching

    @ParameterizedTest(name = "{2}")
    @CsvSource(textBlock = """
            GET,    /messages/content,       not matched resource method
            """)
    public void should_return_empty_if_not_matched(String httpMethod, String uri, String context) {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate.MatchResult result = resource.getUriTemplate().match(uri).get();
        assertTrue(resource.match(result, httpMethod, new String[]{MediaType.TEXT_PLAIN}, mock(UriInfoBuilder.class)).isEmpty());
    }

    // TODO if no method/ sub resource locator matches, return 404
    // TODO if resource class does not have a path annotation, throw illegal argument
    // TODO Head and Options special case


    @Path("/messages")
    static class Messages {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "messages";
        }
    }

    static class Message {

        @GET
        @Path("/content")
        @Produces(MediaType.TEXT_PLAIN)
        public String content() {
            return "content";
        }
    }

}
