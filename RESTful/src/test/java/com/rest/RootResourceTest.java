package com.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RootResourceTest {

    private ResourceContext resourceContext;
    private Messages rootResource;

    @BeforeEach
    void setUp() {
        rootResource = new Messages();
        resourceContext = mock(ResourceContext.class);
        when(resourceContext.getResource(eq(Messages.class))).thenReturn(rootResource);
    }

    @Test
    void should_get_uri_template_from_path_annotation() {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate uriTemplate = resource.getUriTemplate();
        assertTrue(uriTemplate.match("/messages/hello").isPresent());
    }

    // TODO find resource method, matches the http request and http method



    @ParameterizedTest(name = "{3}")
    @CsvSource(textBlock = """
            GET,       /messages,               Messages.get,               Map to resource method
            GET,       /messages/1/content,     Message.content,            Map to sub-resource method
            GET,       /messages/1/body,        MessageBody.get,        Map to sub-sub-resource method
            """)
    public void should_match_resource_method_in_root_resource(String httpMethod, String path, String resourceMethod, String context) {
        StubUriInfoBuilder uriInfoBuilder = new StubUriInfoBuilder();

        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate.MatchResult result = resource.getUriTemplate().match(path).get();
        ResourceRouter.ResourceMethod method = resource.match(result, httpMethod, new String[]{MediaType.TEXT_PLAIN}, resourceContext, uriInfoBuilder).get();

        assertEquals(resourceMethod, method.toString());
    }

    @Test
    void should_match_resource_method_in_sub_resource() {
        SubResource subResource = new SubResource(new Message());
        UriTemplate.MatchResult result = mock(UriTemplate.MatchResult.class);
        when(result.getRemaining()).thenReturn("/content");
        assertTrue(subResource.match(result, "GET", new String[]{MediaType.TEXT_PLAIN}, resourceContext, mock(UriInfoBuilder.class)).isPresent());
    }

    @ParameterizedTest(name = "{2}")
    @CsvSource(textBlock = """
            GET,    /messages/hello,       not matched resource method
            """)
    public void should_return_empty_if_not_matched(String httpMethod, String uri, String context) {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate.MatchResult result = resource.getUriTemplate().match(uri).get();
        assertTrue(resource.match(result, httpMethod, new String[]{MediaType.TEXT_PLAIN}, resourceContext, mock(UriInfoBuilder.class)).isEmpty());
    }

    @Test
    void should_add_ast_matched_resource_to_uri_info_builder() {
        StubUriInfoBuilder uriInfoBuilder = new StubUriInfoBuilder();
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate.MatchResult result = resource.getUriTemplate().match("/messages").get();

        resource.match(result, "GET", new String[]{MediaType.TEXT_PLAIN}, resourceContext, uriInfoBuilder);

        assertTrue(uriInfoBuilder.getLastMatchedResource() instanceof Messages);
    }

    // TODO if resource class does not have a path annotation, throw illegal argument
    // TODO Head and Options special case


    @Path("/messages")
    static class Messages {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "messages";
        }

        @Path("/{id:[0-9]+}")
        public Message getById() {
            return new Message();
        }
    }

    static class Message {

        @GET
        @Path("/content")
        @Produces(MediaType.TEXT_PLAIN)
        public String content() {
            return "content";
        }

        @Path("/body")
        public MessageBody body() {
            return new MessageBody();
        }
    }

    static class MessageBody {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "body";
        }
    }

}
