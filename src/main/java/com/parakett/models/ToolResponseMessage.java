package com.parakett.models;

public class ToolResponseMessage extends ModelMessage {

    public String toolCallId, name, result;

    public ToolResponseMessage(String toolCallId, String name, String result) {
        this.toolCallId = toolCallId;
        this.name = name;
        this.result = result;
    }
    
}
