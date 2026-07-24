package com.parakett.models;

import com.fasterxml.jackson.databind.JsonNode;

/*
This is just a wrapper class. 
 */
public class ModelResponse {

    public JsonNode assistantMessage = null;

    public ModelResponse(JsonNode assistantMessage) {
        this.assistantMessage = assistantMessage;
    }
    
}
