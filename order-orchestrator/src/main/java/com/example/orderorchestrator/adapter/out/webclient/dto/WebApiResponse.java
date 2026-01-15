package com.example.orderorchestrator.adapter.out.webclient.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WebApiResponse<T> {
    private final boolean success;
    private final T data;
    private final WebApiError error;

    @JsonCreator
    public WebApiResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("data") T data,
            @JsonProperty("error") WebApiError error
    ) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public WebApiError getError() {
        return error;
    }
}
