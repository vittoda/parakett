package com.parakett.models;

import com.fasterxml.jackson.databind.JsonNode;

public class ModelAssistantMessage extends ModelMessage {

    public String content = null;
    public JsonNode toolCalls = null;

    public ModelAssistantMessage(String content, JsonNode toolCalls) {
        this.content = content;
        this.toolCalls = toolCalls;
    }
    
}
