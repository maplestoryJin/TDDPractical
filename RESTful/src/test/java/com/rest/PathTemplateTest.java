package com.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PathTemplateTest {

    @ParameterizedTest
    @CsvSource({"/users,/orders", "/users/{id:[0-9]+},/users/id", "/users,/unit/users"})
    void should_not_match_path(String pattern, String path) {
        PathTemplate template = new PathTemplate(pattern);
        Optional<UriTemplate.MatchResult> result = template.match(path);
        assertTrue(result.isEmpty());
    }

    @Test
    void should_return_match_result_if_path_matched() {
        PathTemplate template = new PathTemplate("/users");
        UriTemplate.MatchResult result = template.match("/users/1").get();
        assertEquals("/users", result.getMatched());
        assertEquals("/1", result.getRemaining());
        assertTrue(result.getMatchedPathParameters().isEmpty());
    }

    @Test
    void should_return_match_result_if_path_match_with_variable() {
        PathTemplate template = new PathTemplate("/users/{id}");
        UriTemplate.MatchResult result = template.match("/users/1").get();
        assertEquals("/users/1", result.getMatched());
        assertNull(result.getRemaining());
        assertFalse(result.getMatchedPathParameters().isEmpty());
        assertEquals("1", result.getMatchedPathParameters().get("id"));

    }

    @Test
    void should_extract_variable_value_by_given_pattern() {
        PathTemplate template = new PathTemplate("/users/{id:[0-9]+}");
        UriTemplate.MatchResult result = template.match("/users/1").get();
        assertEquals("1", result.getMatchedPathParameters().get("id"));
    }

    @Test
    void should_throw_illegal_argument_exception_if_variable_redefined() {
        assertThrows(IllegalArgumentException.class, () -> new PathTemplate("/users/{id:[0-9]+}/{id}"));
    }

    @ParameterizedTest
    @CsvSource({"/users/123456,/users/123456,/users/{id}", "/users/1234567890/order,/{resource}/1234567890/{action},/users/{id}/order", "/users/1,/users/{id:[0-9]+},/users/{id}"})
    void first_pattern_should_be_smaller_than_second(String path, String smallerTemplate, String largerTemplate) {
        assertSmaller(path, smallerTemplate, largerTemplate);
    }


    @Test
    void should_compare_equal_match_result() {
        UriTemplate template = new PathTemplate("/users/{id}");
        UriTemplate.MatchResult result = template.match("/users/1").get();
        assertEquals(0, result.compareTo(result));
    }

    private void assertSmaller(String path, String smallerTemplate, String largeTemplate) {
        UriTemplate smaller = new PathTemplate(smallerTemplate);
        UriTemplate larger = new PathTemplate(largeTemplate);
        UriTemplate.MatchResult lhs = smaller.match(path).get();
        UriTemplate.MatchResult rhs = larger.match(path).get();
        assertTrue(lhs.compareTo(rhs) < 0);
        assertTrue(rhs.compareTo(lhs) > 0);
    }
}
