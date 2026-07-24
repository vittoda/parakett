package com.parakett.models;

import com.fasterxml.jackson.databind.JsonNode;

public class ToolMessage {

    public JsonNode toolDefinition = null;

    public ToolMessage(JsonNode tool) {
        this.toolDefinition = tool;
    }
    
}
