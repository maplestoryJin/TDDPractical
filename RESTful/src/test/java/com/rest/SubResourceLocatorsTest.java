package com.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class SubResourceLocatorsTest {


    @ParameterizedTest(name = "{2}")
    @CsvSource(textBlock = """
            /hello,         hello,      fully matched with URI
            /topic/1234,    1234,       multiple matched choices
            /topic/1,       id,         matched with variable
            """)
    void should_match_path_with_uri(String path, String message, String context) {
        StubUriInfoBuilder uriInfoBuilder = new StubUriInfoBuilder();
        uriInfoBuilder.addMatchedResource(new Messages());

        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());

        assertTrue(locators.findResourceMethods(path, "GET", new String[]{MediaType.TEXT_PLAIN}, mock(ResourceContext.class), uriInfoBuilder)
                .isPresent());

        assertEquals(message, ((Message) uriInfoBuilder.getLastMatchedResource()).message);
    }

    @ParameterizedTest(name = "{1}")
    @CsvSource(textBlock = """
            /missing,           unmatched resource method
            /hello/content,     unmatched sub-resource method
            """)
    void should_return_empty_if_not_match_uri(String path, String context) {
        StubUriInfoBuilder uriInfoBuilder = new StubUriInfoBuilder();
        uriInfoBuilder.addMatchedResource(new Messages());

        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());

        assertTrue(locators.findResourceMethods(path, "GET", new String[]{MediaType.TEXT_PLAIN}, mock(ResourceContext.class), uriInfoBuilder)
                .isEmpty());

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
