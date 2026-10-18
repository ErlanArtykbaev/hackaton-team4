package com.hackathon.team4.kitchen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;

/**
 * Stores a small list of a ticket line (condiments, allergens, dangers) as JSON text: the lines are always read
 * with their ticket and never queried by these values.
 */
abstract class JsonColumnConverter<T> implements AttributeConverter<T, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TypeReference<T> type;

    JsonColumnConverter(TypeReference<T> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(T value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot store a ticket line value as JSON", e);
        }
    }

    @Override
    public T convertToEntityAttribute(String column) {
        try {
            return MAPPER.readValue(column, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read a ticket line value from JSON", e);
        }
    }
}
