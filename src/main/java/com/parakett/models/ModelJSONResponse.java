package com.parakett.models;

import com.fasterxml.jackson.databind.JsonNode;

public class ModelJSONResponse extends ModelResponse {

    public JsonNode responseData = null;

    public ModelJSONResponse(JsonNode assistantMessage, JsonNode obj) {
        super(assistantMessage);
        this.responseData = obj;
    }
    
}
