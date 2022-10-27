package com.rest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConverterConstructorTest {

    @Test
    void should_convert_via_converter_constructor() {
        assertEquals(Optional.of(new BigDecimal("12345")), ConverterConstructor.convert(BigDecimal.class, "12345"));
    }

    @Test
    void should_not_convert_if_no_converter_constructor() {
        assertEquals(Optional.empty(), ConverterConstructor.convert(NoConverter.class, "12345"));
    }

    @Test
    void should_convert_via_constructor_factory() {
        assertEquals(Optional.of(Converter.Factory), ConverterFactory.convert(Converter.class, "Factory"));
    }

    @Test
    void should_not_convert_if_no_constructor_factory() {
        assertEquals(Optional.empty(), ConverterFactory.convert(NoConverter.class, "Factory"));
    }
}

class NoConverter {
    NoConverter valueOf(String value) {
        return new NoConverter();
    }
}