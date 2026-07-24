package com.parakett.flow;


import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;

public class Step {

    public static final String ROOT_STEP_NAME = "root";

    private static final String ERROR_INSTRUCTION = "You need to handle this error. One of the last step generated the error. Step details and error detauls are available below";

    public static final String STEP_TYPE_LLM = "LLM";
    public static final String STEP_TYPE_AGENT = "Agent";

    public String name;
    public String model = null;
    public boolean jsonResponse = false;
    public String description = null, instruction = null;
    public String type = STEP_TYPE_LLM;
    public String agent = null;
    String[] toolNames = null;
    String[] nextSteps = null;

    public boolean hitlNeeded = false;
    public HITLMessage hitlMessage = null;
    public List<String> additionalContextInstructions = null;

    public Step(String name, String description, String instruction, 
            List<String> additionalContextInstructions, //Primarily used when passing info from one agent to another to second agents root step
            String type, String modelName, String agent,
            String[] nextSteps, String[] tools, boolean jsonResponse, 
            boolean hitlNeeded, HITLMessage hitlMessage) {
        this.name = name;
        this.description = description;
        this.instruction = instruction;
        this.type = type;
        this.model = modelName;
        this.toolNames = tools;
        this.nextSteps = nextSteps;
        this.jsonResponse = jsonResponse;
        this.hitlNeeded = hitlNeeded;
        this.agent = agent;
        this.hitlMessage = hitlMessage;
        this.additionalContextInstructions = additionalContextInstructions;
    }

    public ObjectNode getJSON(boolean extended) {
        ObjectNode ret = JsonUtils.MAPPER.createObjectNode();
        ret.put("name", this.name);
        if (extended) {
            ret.put("instruction", this.instruction);

            ArrayNode toolNamesJSON = JsonUtils.MAPPER.createArrayNode();
            if (this.toolNames != null) {
                for (String s : this.toolNames) {
                    toolNamesJSON.add(s);
                }
            }
            ret.set("toolNames", toolNamesJSON);
        }

        return ret;
    }



    public static Step getErrorStrep(String allStepNames[], String modelName) {
        String[] tools = { "agent_runStep", "agent_runErrorStep" };
        return new Step("__error__", "Error handler step", ERROR_INSTRUCTION, null, "LLM", 
            modelName,null,
                allStepNames, tools, true, false,null);
    }

}
