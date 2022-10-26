package com.rest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConverterConstructorTest {
    @Test
    void should_convert_via_constructor_converter() {

        assertEquals(Optional.of(new BigDecimal("12345")), ConverterConstructor.convert(BigDecimal.class, "12345"));
    }
}