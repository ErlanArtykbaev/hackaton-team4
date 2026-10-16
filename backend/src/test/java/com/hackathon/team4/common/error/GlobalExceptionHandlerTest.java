package com.hackathon.team4.common.error;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ErrorProbeController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @ParameterizedTest
    @CsvSource({
            "CREDIT_LIMIT_EXCEEDED, 409, CREDIT_LIMIT_EXCEEDED",
            "ROOM_NOT_READY,        409, ROOM_NOT_READY",
            "NOT_FOUND,             404, NOT_FOUND",
            "SERVICE_UNAVAILABLE,   503, UPSTREAM_UNAVAILABLE",
            "NETWORK_ERROR,         503, UPSTREAM_UNAVAILABLE",
            "AUTH_INVALID,          500, INTERNAL_ERROR",
            "UNKNOWN_MENU_ITEM,     500, INTERNAL_ERROR"
    })
    void shouldMapSandboxErrorToProblemDetail(String sandboxCode, int status, String code) throws Exception {
        mvc.perform(get("/test/sandbox-error/" + sandboxCode))
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    void shouldHideUnexpectedErrorDetails() throws Exception {
        mvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(GlobalExceptionHandler.INTERNAL_ERROR))
                .andExpect(jsonPath("$.detail").value("Something went wrong"));
    }

    @Test
    void shouldReturnValidationErrorWhenRequestInvalid() throws Exception {
        mvc.perform(post("/test/validated").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GlobalExceptionHandler.VALIDATION_ERROR));
    }
}
