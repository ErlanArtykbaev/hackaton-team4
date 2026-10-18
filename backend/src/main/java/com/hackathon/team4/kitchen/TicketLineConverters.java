package com.hackathon.team4.kitchen;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.DangerFor;
import jakarta.persistence.Converter;
import java.util.List;

final class TicketLineConverters {

    private TicketLineConverters() {
    }

    @Converter
    static class Strings extends JsonColumnConverter<List<String>> {
        Strings() {
            super(new TypeReference<>() {
            });
        }
    }

    @Converter
    static class Allergens extends JsonColumnConverter<List<Allergen>> {
        Allergens() {
            super(new TypeReference<>() {
            });
        }
    }

    @Converter
    static class Dangers extends JsonColumnConverter<List<DangerFor>> {
        Dangers() {
            super(new TypeReference<>() {
            });
        }
    }
}
