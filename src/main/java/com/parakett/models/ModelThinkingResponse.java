package com.parakett.models;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ModelThinkingResponse extends ModelResponse {

    public ModelThinkingResponse(ObjectNode assistantMessage) {
        super(assistantMessage);
    }
}
