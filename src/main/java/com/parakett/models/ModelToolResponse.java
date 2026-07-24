package com.parakett.models;

import java.util.LinkedList;

import com.fasterxml.jackson.databind.JsonNode;

public class ModelToolResponse extends ModelResponse {

    
    public LinkedList<ToolCall> toolCalls = new LinkedList<>();
    public ModelToolResponse(JsonNode assistantMessage) {
        super(assistantMessage);
    }

    public void addToolCall(ToolCall tc) {
        toolCalls.add(tc);
    }

    public static class ToolCall {

        public JsonNode arguments = null;
        public String toolName = null;
        public String toolId = null;

        public ToolCall(String toolName, String toolId, JsonNode arguments) {
            this.toolName = toolName;
            this.arguments = arguments;
            this.toolId = toolId;
        }

    }

}
