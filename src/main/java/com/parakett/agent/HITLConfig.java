package com.parakett.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class HITLConfig {


    public static final String HITL_TARGET_TYPE_WEBHOOK = "webhook";
    public static final String HITL_TARGET_TYPE_CHANNEL = "channel";
    public static final String HITL_TARGET_TYPE_EVENT = "event";

    public HITLTarget[] targets;

    public HITLConfig(HITLTarget[] targets) {
        this.targets = targets;
    }

    public static HITLConfig loadFromJSON(JsonNode node) {
        ArrayNode targetArray = (ArrayNode)node.get("targets");
        HITLTarget[] targets = new HITLTarget[targetArray.size()];
        for(int i=0;i<targetArray.size();i++) {
            JsonNode on = targetArray.get(i);
            String type = on.get("type").asText();
            String instance = null;
            if(type.equals(HITL_TARGET_TYPE_WEBHOOK) || type.equals(HITL_TARGET_TYPE_CHANNEL)) {
                instance =  on.get("instance").asText();
            }

            targets[i] =  new HITLTarget(type, instance);
        }

        return new HITLConfig(targets);
    }


    public static class HITLTarget {

        public String type, instance;

        public HITLTarget(String type, String instance) {
            this.type = type;
            this.instance = instance;
        }

    }
    
}
