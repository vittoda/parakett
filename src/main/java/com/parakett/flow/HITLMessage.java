package com.parakett.flow;

import com.fasterxml.jackson.databind.JsonNode;

// Human In The Loop configuration
public class HITLMessage {


    public static final String HITL_RESPONSE_TYPE_CONFIRMATION = "confirmation";

    public String message, responseType, responseKey;

    public HITLMessage(String message, String responseType, String responseKey) {
        this.message = message;
        this.responseType = responseType;
        this.responseKey = responseKey;
    }

    public static HITLMessage loadFromJSON(JsonNode def) {
        String message = def.get("message").asText();
        String responseType = def.has("responseType") ? def.get("responseType").asText() : HITL_RESPONSE_TYPE_CONFIRMATION;
        String responseKey = def.has("responseKey") ? def.get("responseKey").asText() : "response";
       

        return new HITLMessage(message, responseType, responseKey);
    }




    
}
