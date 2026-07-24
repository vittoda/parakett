package com.parakett.models;

import com.fasterxml.jackson.databind.JsonNode;

public class ModelTextResponse extends ModelResponse {

    public String responseData = null;

    public ModelTextResponse(JsonNode assistantMessage, String text) {
        super(assistantMessage);
        this.responseData = text;
    }
}