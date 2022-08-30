package com.rest;

import jakarta.ws.rs.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Path("/messages")
    static class Messages {
        @Path("/hello")
        public Message hello() {
            return new Message();
        }

        @Path("/topic/{id}")
        public Message message() {
            return new Message();
        }

        @Path("/topic/1234")
        public Message message1234() {
            return new Message();
        }
    }


    static class Message {

    }


}
