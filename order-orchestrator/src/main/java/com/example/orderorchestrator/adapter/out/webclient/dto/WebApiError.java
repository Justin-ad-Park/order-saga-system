package com.example.orderorchestrator.adapter.out.webclient.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WebApiError {
    private final String code;
    private final String message;

    @JsonCreator
    public WebApiError(
            @JsonProperty("code") String code,
            @JsonProperty("message") String message
    ) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
