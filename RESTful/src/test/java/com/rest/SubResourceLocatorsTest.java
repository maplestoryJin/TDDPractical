package com.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SubResourceLocatorsTest {


    @ParameterizedTest(name = "{2}")
    @CsvSource(textBlock = """
            /hello,         Messages.hello,             fully matched with URI
            /hello/content, Messages.hello,             matched with URI
            /topic/1234,    Messages.message1234,       multiple matched choice
            """)
    void should_match_path_with_uri(String path, String resourceMethod, String context) {
        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());
        ResourceRouter.SubResourceLocator locator = locators.findSubResource(path).get();

        assertEquals(resourceMethod, locator.toString());
    }

    @Test
    void should_return_empty_if_not_match_uri() {
        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());
        assertTrue(locators.findSubResource("/missing").isEmpty());

    }

    @Test
    void should_call_locator_method_to_generate_sub_resource() {
        StubUriInfoBuilder uriInfoBuilder = new StubUriInfoBuilder();
        uriInfoBuilder.addMatchedResource(new Messages());

        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());
        ResourceRouter.SubResourceLocator subResourceLocator = locators.findSubResource("/hello").get();

        ResourceRouter.Resource subResource = subResourceLocator.getSubResource(mock(ResourceContext.class), uriInfoBuilder);

        UriTemplate.MatchResult result = mock(UriTemplate.MatchResult.class);
        when(result.getRemaining()).thenReturn(null);

        ResourceRouter.ResourceMethod resourceMethod = subResource.match(result, "GET", new String[]{MediaType.TEXT_PLAIN}, null, uriInfoBuilder).get();
        assertEquals("Message.content", resourceMethod.toString());
        assertEquals("hello", ((Message) uriInfoBuilder.getLastMatchedResource()).message);


    }

    @Path("/messages")
    static class Messages {
        @Path("/hello")
        public Message hello() {
            return new Message("hello");
        }

        @Path("/topic/{id}")
        public Message message() {
            return new Message("id");
        }

        @Path("/topic/1234")
        public Message message1234() {
            return new Message("1234");
        }
    }


    static class Message {
        private String message;

        public Message(String message) {

            this.message = message;
        }

        @GET
        public String content() {
            return "content";
        }
    }


}
