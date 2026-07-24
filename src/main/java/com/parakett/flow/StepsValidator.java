package com.parakett.flow;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;

import java.util.Iterator;
import java.util.LinkedList;

public class StepsValidator {

    private static final String[] SUPPORTED_STEP_TYPES = { "LLM", "Agent" };
    private static final String SUPPORTED_STEP_TYPES_STRING = "LLM, Agent";

    private static final String[] SUPPORTED_FIELDS = { "instruction", "description", "name", "type", "requiresInput",
            "tools", "nextStep", "jsonResponse", "hitlNeeded", "hitlMessage", "agent", };

    public ArrayNode validate(ArrayNode steps, List<String> supportedTools) {

        ArrayNode validationErrors = JsonUtils.MAPPER.createArrayNode();
        int len = steps.size();
        for (int i = 0; i < len; i++) {
            JsonNode step = steps.get(i);
            ArrayNode stepErrors = validateStep(step, supportedTools, i);
            validationErrors.addAll(stepErrors);

        }

        // Individual step validations are done. Now collect all step names. We need to
        // validate for missing step names.
        List<String> stepNames = new LinkedList<String>();
        for (int i = 0; i < len; i++) {
            ObjectNode step = JsonUtils.MAPPER.createObjectNode();
            if (step.has("name")) {
                JsonNode o = step.get("name");
                if (o != null && o.isTextual()) {
                    String name = o.asText();
                    // Check if name exists
                    if (stepNames.contains(name)) {
                        validationErrors
                                .add("Step name '" + name + "' at index is already used. Use a differrent name");
                        continue;
                    }
                    stepNames.add(name);
                }
            }
        }

        for (int i = 0; i < len; i++) {
            ObjectNode step = JsonUtils.MAPPER.createObjectNode();
            StringBuilder notFoudnStepNames = new StringBuilder();

            String stepIdentifier = null;
            if (!step.has("name")) {
                stepIdentifier = "at index " + i;
            } else {
                JsonNode v = step.get("name");
                if (v != null && v.isTextual()) {
                    stepIdentifier = step.get("name").asText();
                }
            }

            if (step.has("nextStep")) {
                JsonNode o = step.get("nextStep");
                if (!(o != null && o.isArray())) {
                    continue;
                }

                ArrayNode nextStep = (ArrayNode) o;
                int stepsLength = nextStep.size();
                for (int j = 0; j < stepsLength; j++) {
                    JsonNode v = nextStep.get(j);
                    if (v == null || !v.isTextual()) {
                        continue;
                    }

                    String nextStepName = v.asText();
                    boolean found = false;
                    for (String sn : stepNames) {
                        if (sn.equals(nextStepName)) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        if (!notFoudnStepNames.isEmpty()) {
                            notFoudnStepNames.append(",");
                        }
                        notFoudnStepNames.append(nextStepName);
                    }

                }
            }

            if (!notFoudnStepNames.isEmpty()) {
                validationErrors.add("Step " + stepIdentifier + " has invalid 'nextStep' values. Steps "
                        + notFoudnStepNames + " not found");
            }
        }

        return validationErrors;

    }

    public ArrayNode validateStep(JsonNode step, List<String> supportedTools, int index) {
        ArrayNode result = JsonUtils.MAPPER.createArrayNode();
        String stepIdentifier = null;
        if (!step.has("name")) {
            result.add("Step at index " + index + " has no 'name' field");
            stepIdentifier = "at index " + index;
        } else {
            JsonNode v = step.get("name");
            if (v != null && v.isTextual()) {
                stepIdentifier = step.get("name").asText();
            } else {
                result.add("Name for the step at index " + index + " should be string");
            }
        }

        if (!step.has("instruction")) {
            result.add("Step " + stepIdentifier + " has no 'instruction' field");
        } else {
            JsonNode v = step.get("instruction");
            if (!(v != null && v.isTextual())) {
                result.add("Step " + stepIdentifier + " has invalid 'instruction' field. Expected a string type");
            }
        }

        if (!step.has("type")) {
            result.add("Step " + stepIdentifier + " has no 'type' field");
        } else {
            JsonNode v = step.get("type");
            if (!(v != null && v.isTextual())) {
                result.add("Step " + stepIdentifier + " has invalid 'type' field. Expected a string type");
            } else {
                String type = v.asText();
                boolean found = false;
                for (String s : SUPPORTED_STEP_TYPES) {
                    if (s.equals(type)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    result.add("Step " + stepIdentifier + " has invalid 'type' field. Supported types are "
                            + SUPPORTED_STEP_TYPES_STRING + ". Found '" + type + "'");
                }
            }
        }

        if (step.has("requiresInput")) {
            JsonNode o = step.get("requiresInput");
            if (!(o != null && o.isBoolean())) {
                result.add(
                        "Step " + stepIdentifier + " has invalid 'requiresInput' field. Expected a true/false value");
            }
        }

        if (step.has("jsonResponse")) {
            JsonNode o = step.get("jsonResponse");
            if (!(o != null && o.isBoolean())) {
                result.add(
                        "Step " + stepIdentifier + " has invalid 'jsonResponse' field. Expected a true/false value");
            }
        }

        if (step.has("tools")) {
            Object o = step.get("tools");
            if (o instanceof ArrayNode arr) {
                boolean valid = true;

                for (int i = 0; i < arr.size(); i++) {
                    JsonNode val = arr.get(i);

                    if (!(val != null && val.isTextual())) {
                        valid = false;
                        break;
                    } else {
                        String name = val.asText();
                        boolean found = false;
                        for (String tn : supportedTools) {
                            if (tn.equals(name)) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            result.add(
                                    "Step " + stepIdentifier + " has invalid tool. Tool '" + name
                                            + "' is not supported");
                        }
                    }
                }

                if (!valid) {
                    result.add(
                            "Step " + stepIdentifier + " has invalid 'tools' field. Expected array of strings");
                }
            }
        }

        if (step.has("hitlNeeded")) {
            JsonNode hitlNeeded = step.get("hitlNeeded");
            if (!hitlNeeded.isBoolean()) {
                result.add("Field 'hitlNeeded' in step '" + stepIdentifier + "' should of of type boolean");
            } else {
                boolean hitlNeededValue = hitlNeeded.asBoolean();
                if (hitlNeededValue) {
                    if (!step.has("hitlMessage")) {
                        result.add(
                                "In step '" + stepIdentifier + "' hitlMessage is needed when 'hitlNeeded' is enabled");
                    } else if (!step.get("hitlMessage").isObject()) {
                        result.add("In step '" + stepIdentifier + "' hitlMessage should be of object type");
                    } else {
                        JsonNode hitlMessage = step.get("hitlMessage");
                        ArrayNode results1 = validateHitlConfig(hitlMessage, stepIdentifier);
                        result.addAll(results1);
                    }
                }
            }
        }

        if (step.has("nextStep")) {
            Object o = step.get("nextStep");
            if (o instanceof ArrayNode arr) {
                boolean valid = true;

                for (int i = 0; i < arr.size(); i++) {
                    JsonNode val = arr.get(i);

                    if (!(val != null && val.isTextual())) {
                        valid = false;
                        break;
                    }
                }

                if (!valid) {
                    result.add(
                            "Step " + stepIdentifier + " has invalid 'nextStep' field. Expected array of strings");
                }
            }
        }

        // Finally check of there are any fields that are not supported.
        StringBuilder unsupportedFields = new StringBuilder();
        JsonNode stepNode = step;

        Iterator<String> fieldNames = stepNode.fieldNames();

        while (fieldNames.hasNext()) {
            String k = fieldNames.next();
            boolean found = false;
            for (String sf : SUPPORTED_FIELDS) {
                if (sf.equals(k)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                if (!unsupportedFields.isEmpty()) {
                    unsupportedFields.append(",");
                }
                unsupportedFields.append(k);
            }
        }

        if (!unsupportedFields.isEmpty()) {
            result.add(
                    "Step " + stepIdentifier + " has unsupported fields. Fields " + unsupportedFields.toString()
                            + " are not supported");
        }

        return result;
    }

    private ArrayNode validateHitlConfig(JsonNode hitlMessage, String stepIdentifier) {
        ArrayNode result = JsonUtils.MAPPER.createArrayNode();
        if (!hitlMessage.has("message")) {
            result.add("In step '" + stepIdentifier + "' hitlMessage should have 'message' field of type string");
        } else {
            JsonNode m = hitlMessage.get("message");
            if (!m.isTextual()) {
                result.add("In step '" + stepIdentifier + "' hitlMessage should have 'message' field of type string");
            }
        }

        if (hitlMessage.has("responseKey")) {
            JsonNode m = hitlMessage.get("responseKey");
            if (!m.isTextual()) {
                result.add("In step '" + stepIdentifier + "' hitlMessage 'responseKey' field should be of type string");
            }
        }

        if (hitlMessage.has("responseType")) {
            JsonNode m = hitlMessage.get("responseType");
            if (!m.isTextual()) {
                result.add("In step '" + stepIdentifier + "' hitlMessage 'responseType' field should be of type string");
            } else {
                String rt = m.asText();
                if (!rt.equals(HITLMessage.HITL_RESPONSE_TYPE_CONFIRMATION)) {
                    result.add("In step '" + stepIdentifier + "' hitlMessage 'responseType' has invalid value. Only '"
                            + HITLMessage.HITL_RESPONSE_TYPE_CONFIRMATION + "' type is supported");
                }
            }
        }

        return result;

    }

}
