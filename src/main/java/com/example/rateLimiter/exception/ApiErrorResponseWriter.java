package com.example.rateLimiter.exception;

import com.example.rateLimiter.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component
public class ApiErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper=objectMapper;
    }

    public void sendError(HttpServletResponse response, int status, String error, String message) throws IOException {

        ErrorResponse body=ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .build();

        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}