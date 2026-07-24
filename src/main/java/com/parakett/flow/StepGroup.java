package com.parakett.flow;

import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class StepGroup {

    private static final String SET_STEP_CONTEXT_STEP_INSTRUCTION  = "The step {targetStep} needs additional context. Baed on your context provide the information needed as context to the step {targetStep}. Do the required formatting, cleanup on the information. You have the following additional information about the next step. \n"+
    "agent : {agentName}\n"+
    "agent prompt/instruction : {stepInstruction}\n stepName : {targetStep}\nstep description:{stepDescription}\n\n"+
    "You need to call the tool \"agent_setStepContext\", with this additional context information. Use the step name as {targetStep}" ;

    private HashMap<String, Step> _mSteps = new HashMap<>();

    public StepGroup() {
    }


    public Step getStep(String name) {
        return _mSteps.get(name);
    }

    public void addStep(Step step) {
        _mSteps.put(step.name, step);
    }

    public ArrayList<Step> getAllLastSteps() {
        ArrayList<Step> lastSteps= new ArrayList<>();
        for(Step step : _mSteps.values()) {
            if(step.nextSteps == null || step.nextSteps.length == 0) {
                lastSteps.add(step);
            }
        }

        return lastSteps;
    }

    public Step addSteps(ArrayNode steps, String agentDefaultModelName) {
        int len = steps.size();
        Step firstStep = null;
        String[] stepNames = new String[steps.size()];
        for (int i = 0; i < len; i++) {
            JsonNode step = steps.get(i);
            String name = step.get("name").asText();
            stepNames[i] = name;
            JsonNode desc = step.get("description");
            String description = desc ==null || desc.isNull() ? "" :  desc.asText();
            String instruction = step.get("instruction").asText();
            String type = step.has("type") ? step.get("type").asText() : Step.STEP_TYPE_LLM;

            String nextSteps[] = null;
            if (step.has("nextStep")) {
                ArrayNode nextStepNamesObject = (ArrayNode)step.get("nextStep");
                nextSteps = nextStepNamesObject.size() > 0 ?  new String[nextStepNamesObject.size()+1] : new String[0]; //+1 for error step
                for (int j = 0; j < nextStepNamesObject.size(); j++) {
                        nextSteps[j] = nextStepNamesObject.get(j).asText();
                }

                if(nextStepNamesObject.size() > 0) {
                    nextSteps[nextSteps.length-1] = "__error__";
                }
            }
            else {
                nextSteps = new String[0];
                //nextSteps[0] = "__error__";
            }

            String modelName = agentDefaultModelName;
            if (step.has("model")) {
                modelName = step.get("model").asText();
            }

            ArrayNode toolsObject = step.has("tools") ? (ArrayNode)step.get("tools") : null;
            String[] tools = null;
            if (toolsObject != null) {
                tools = new String[toolsObject.size()];
                for (int j = 0; j < toolsObject.size(); j++) {
                    tools[j] = toolsObject.get(j).asText();
                }
            }

            boolean jsonResponse = false;
            if(step.has("jsonResponse")) {
                jsonResponse = step.get("jsonResponse").asBoolean();
            }

            boolean hitlNeeded = false;
            if(step.has("hitlNeeded")) {
                hitlNeeded = step.get("hitlNeeded").asBoolean();
            }

            HITLMessage hitlConfig = null;
            if(hitlNeeded) {
                //Get and parse the content
                JsonNode hitlMessageNode = step.get("hitlMessage");
                hitlConfig = HITLMessage.loadFromJSON(hitlMessageNode);
            }

            String agent = null;
            if(step.has("agent")) {
                agent = step.get("agent").asText();
            }

            Step st = new Step(name, description, instruction, null, type, modelName, agent, nextSteps, tools, jsonResponse, 
                hitlNeeded, hitlConfig);
            _mSteps.put(name, st);
            if(i == 0) {
                firstStep = st;
            }
        }

        //Add error step.
        Step errorStep = Step.getErrorStrep(stepNames, agentDefaultModelName);
        _mSteps.put("__error__", errorStep);

        return firstStep;

    }

    public void addEndFlowStep(String modelName, ArrayList<Step> lastSteps) {
        Step endFlowStep = new Step("flow_EndFlow",
            "This is the last step for a flow, which will get the result.",
            "This is the last step in the flow. You will need to prepare a result and call the agent_EndFlow, with result of the entore flow. The result will be used by caller for subsequent actions.",
            null,
            Step.STEP_TYPE_LLM,modelName,  null,new String[0],
            new String[] {"agent_endFlow"}, true, false, null);

        //We are not direcly calling the getLastSteps here intentionally. Hvaing no last step is an error. So,
        //this should be handled at model respnse handler. 

        for(Step l : lastSteps) {
            l.nextSteps = new String[]{"flow_EndFlow"};
        }
        _mSteps.put("flow_EndFlow", endFlowStep);
    }

    public void addSetStepContextStep(String modelName) {
        //Identify all the agent steps.
        ArrayList<Step> agentSteps = new ArrayList<>();
        for(Step step : _mSteps.values()) {
            if(step.type.equals(Step.STEP_TYPE_AGENT)) {
                agentSteps.add(step);
            }
        }

        if(agentSteps.size() == 0) {
            return;
        }

        HashMap<String, Step> stepsToAdd = new HashMap<>();

        for(Step agentStep : agentSteps) {
            for(Step step : _mSteps.values()) {
                String nextSteps[] = step.nextSteps;
                if(nextSteps == null || nextSteps.length == 0) {
                    continue;
                }

                for(int i=0;i<nextSteps.length;i++) {
                    String nextStepName = nextSteps[i];
                    if(nextStepName.equals(agentStep.name)) {
                        //We have. 
                        String name = agentStep.name+"_setStepContext";
                        if(!stepsToAdd.containsKey(name)) {
                            //We don't have it added already. 
                            Step toAdd = getSetStepContextStep(agentStep, modelName);
                            stepsToAdd.put(toAdd.name, toAdd);
                        }
                        nextSteps[i] = name;
                    }
                }
            }
        }

        //Now go through all the steps again to to find, where we need to add
        for(Step step : stepsToAdd.values()) {
            _mSteps.put(step.name, step);
        }

    }

    private Step getSetStepContextStep(Step targetStep, String modelName) {
            String instruction = SET_STEP_CONTEXT_STEP_INSTRUCTION.replace("{targetStep}", targetStep.name)
            .replace("{agentName}", targetStep.agent)
            .replace("{stepInstruction}", targetStep.instruction)
            .replace("{stepDescription}", targetStep.description);

         Step setContextStep = new Step(targetStep.name+"_setStepContext",
            "This step will set the additional system context for step '"+targetStep.name+"'",
            instruction,
            null,
            Step.STEP_TYPE_LLM, modelName,  null,new String[]{targetStep.name},
            new String[] {"agent_setStepContext"}, true, false, null); 

        return setContextStep;
    }



}
