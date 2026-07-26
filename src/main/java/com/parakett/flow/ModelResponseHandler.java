package com.parakett.flow;

import com.parakett.models.ModelJSONResponse;
import com.parakett.models.ModelResponse;
import com.parakett.models.ModelThinkingResponse;
import com.parakett.models.ModelToolResponse;
import com.parakett.models.ModelToolResponse.ToolCall;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.mcp.MCPRegistry;
import com.parakett.mcp.ToolCallResponse;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelResponseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelResponseHandler.class);

    public void process(StepRunInstance stepRunInstance, FlowRunner flowRunner,
            ModelResponse response) throws FlowException {

        if (response == null) {
            LOGGER.warn("Response is null for session '{}', step '{}'", flowRunner.getSessionId(),
                    stepRunInstance.getStepDefinition().name);
            flowRunner.currentStepFailed(StepRunInstance.FAIL_REASON_INVALID_RESPONSE, "No response from the model");
            return;
        }

        if (response instanceof ModelThinkingResponse) {
            flowRunner.runCurrentStepAgain();
            return;
        }

        if (response instanceof ModelJSONResponse) {
            ModelJSONResponse jsonRespnse = (ModelJSONResponse) response;
            JsonNode content = jsonRespnse.responseData;
            // Probably steps.
            if (content.has("steps")) {
                StepsValidator sv = new StepsValidator();
                ArrayNode steps = (ArrayNode) content.get("steps");
                if (steps.size() == 0) {// Not sure how this will happen. But if it happens.
                    throw new FlowException("Model responsed with steps. But steps list is empty");
                }
                // TODO: The tools below should be specific to the flow runner ot step group.
                ArrayNode validationErrors = sv.validate(steps, MCPRegistry.getAllToolNames());
                if (validationErrors.size() > 0) {
                    LOGGER.error("Response validation failed. ", steps.toPrettyString());
                    flowRunner.currentStepFailed(StepRunInstance.FAIL_REASON_INVALID_RESPONSE,
                            validationErrors.toString());
                    return;
                }

                // We will be adding as one for current
                StepGroup sg = new StepGroup();
                Step firstStep = sg.addSteps(steps, flowRunner.getModelName());

                //Add end flow step.
                ArrayList<Step> lastSteps= sg.getAllLastSteps();
                if(lastSteps.size() == 0) {
                    LOGGER.error("Steps generated does not have a last step.  This may cause infinite loop");
                    flowRunner.currentStepFailed(StepRunInstance.FAIL_REASON_INVALID_RESPONSE,
                            validationErrors.toString());
                    return;
                }

                sg.addEndFlowStep(flowRunner.getModelName(), lastSteps);

                //Add et cotext step
                sg.addSetStepContextStep(flowRunner.getModelName());

                String lastPrompt = stepRunInstance.getStepDefinition().instruction;
                // When a new step group is created, it should not lose the prompt that created
                // it. Usually for the first prompt.
                flowRunner.clearAndRunStepGroup(sg, firstStep, lastPrompt);
                return;
            }
            String nextStep = null;
            if (content.has("nextStep")) {
                nextStep = content.get("nextStep").asText();
                flowRunner.runStep(nextStep, null);
            } else {
                flowRunner.currentStepCompleted(true);
            }

        } else if (response instanceof ModelToolResponse) {
            ModelToolResponse toolResponse = (ModelToolResponse) response;
             
            ArrayNode toolNames = JsonUtils.MAPPER.createArrayNode();
            for (ToolCall tc : toolResponse.toolCalls) {
                toolNames.add(tc.toolName);
                LOGGER.info("Calling tool '{}'", tc.toolName);
                if (tc.toolName.equals("agent_runStep")) {
                    String stepName = tc.arguments.get("stepName").asText();
                    flowRunner.runStep(stepName, null);
                    ObjectNode toolResult = JsonUtils.MAPPER.createObjectNode();
                    toolResult.put("status", "Success");
                    toolResult.put("toolResult", "Step '" + stepName + "' triggered");
                    flowRunner.addToolResult(tc.toolName, tc.toolId, toolResult);
                } else if (tc.toolName.equals("agent_runErrorStep")) {
                    String stepName = "__error__";
                    flowRunner.runStep(stepName, tc.arguments.get("errorDetails").asText());
                    ObjectNode toolResult = JsonUtils.MAPPER.createObjectNode();
                    toolResult.put("status", "Success");
                    toolResult.put("toolResult", "Error step triggered");
                    flowRunner.addToolResult(tc.toolName, tc.toolId, toolResult);
                } else if (tc.toolName.equals("agent_sendMessage")) {
                    LOGGER.info("Sending message on the channel. Message : {}",
                            tc.arguments.toPrettyString());
                    ObjectNode toolResult = JsonUtils.MAPPER.createObjectNode();
                    ObjectNode r = flowRunner.sendResponse(tc.arguments.get("message").asText());
                    toolResult.put("status", "Success");
                    toolResult.set("toolResult", r);
                    flowRunner.addToolResult(tc.toolName, tc.toolId, toolResult);
                } else if (tc.toolName.equals("agent_endFlow")) {
                    String result = tc.arguments.get("result").asText();
                    flowRunner.setFlowResult(result);
                    ObjectNode toolResult = JsonUtils.MAPPER.createObjectNode();
                    toolResult.put("status", "Success");
                    toolResult.put("flowResult", result);
                    flowRunner.addToolResult(tc.toolName, tc.toolId, toolResult);
                } else if (tc.toolName.equals("agent_setStepContext")) {
                    String stepName = tc.arguments.get("stepName").asText();
                    String context = tc.arguments.get("context").asText();
                    LOGGER.info("Calling agent_setStepContext for step '{}'", stepName);
                    flowRunner.setAdditionalContextForStep(stepName, context);
                    ObjectNode toolResult = JsonUtils.MAPPER.createObjectNode();
                    toolResult.put("status", "Success");
                    toolResult.put("toolResult", "Set context");
                    flowRunner.addToolResult(tc.toolName, tc.toolId, toolResult);
                } else {
                    ToolRunner runner = new ToolRunner(tc.toolName, tc.arguments);

                    long startTime = System.currentTimeMillis();
                    ToolCallResponse toolResult = runner.run(flowRunner.getAgentInstance());
                    long endTime = System.currentTimeMillis();
                    
                    stepRunInstance.addLogInfo("toolRunDuration", (endTime - startTime));
                    flowRunner.addToolResult(tc.toolName, tc.toolId, toolResult.getResultJSON());
                    // Take the next step from current tool run step, and proceed.
                }
                LOGGER.info("Tool call completed for tool '{}'.", tc.toolName);
            }

            stepRunInstance.addLogInfo("selectedTools", toolNames);
            flowRunner.currentStepCompleted(true);
        } else {
            LOGGER.warn("Invalid response. '{}'. Content '{}]", response.getClass().getName(),
                    response.assistantMessage.toPrettyString());
            flowRunner.currentStepCompleted(true);
        }
    }

}
