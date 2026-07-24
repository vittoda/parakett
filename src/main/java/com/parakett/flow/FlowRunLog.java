package com.parakett.flow;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;

public class FlowRunLog {

    private List<FlowRunLogItem> _mLogs = new LinkedList<>();

    public FlowRunLog() {

    }

    public FlowRunLogItem add(Step step) {
        FlowRunLogItem item = new FlowRunLogItem(step);
        _mLogs.add(item);
        return item;
    }

    public void updateLast(String status, String failureReason) {
        FlowRunLogItem item = _mLogs.get(_mLogs.size() - 1);
        item.update(status, failureReason);
    }

    public FlowRunLogItem getLastItem() {
        FlowRunLogItem item = _mLogs.get(_mLogs.size() - 1);
        return item;
    }

    public ArrayNode getLogList(int startIndex, int size) {
        int endIndex = startIndex + size;
        if (_mLogs.size() < startIndex + size) {
            endIndex = _mLogs.size();
        }

        ArrayNode result = JsonUtils.MAPPER.createArrayNode();
        for (int i = startIndex; i < endIndex; i++) {
            result.add(_mLogs.get(i).getJSON(false));
        }

        return result;
    }

    public ObjectNode getExtendedDetailsForItemAtIndex(int index) {
        return _mLogs.get(index).getJSON(true);
    }

    public static class FlowRunLogItem {

        public Step step = null;
        public String status = null;
        public String failureReason = null;
        public String instructions = null;

        public ObjectNode llmResponse = null;
        public ObjectNode toolResult = null;
        public String errorMessage = null;
        public ObjectNode additionalLogInfo = null;
        public ObjectNode memory = null;

        public ArrayNode variablesSnapshot = null;

        public FlowRunLogItem(Step step) {
            this.step = step;
            this.status = StepRunInstance.STATUS_WAITING;
            this.additionalLogInfo = JsonUtils.MAPPER.createObjectNode();
        }

        public void setVariables(Variables variables) {
            variablesSnapshot = JsonUtils.MAPPER.createArrayNode();
            for (String key : variables.keys()) {
                Object v = variables.getValue(key);
                ObjectNode vt = JsonUtils.MAPPER.createObjectNode();
                vt.put("name", key);
                if (v instanceof String) {
                    vt.put("value", (String) variables.getValue(key));
                } else if (v instanceof Integer) {
                    vt.put("value", (Integer) variables.getValue(key));
                } else if (v instanceof Long) {
                    vt.put("value", (Long) variables.getValue(key));
                } else if (v instanceof Boolean) {
                    vt.put("value", (Boolean) variables.getValue(key));
                } else if (v instanceof Double) {
                    vt.put("value", (Double) variables.getValue(key));
                } else if (v instanceof Float) {
                    vt.put("value", (Float) variables.getValue(key));
                } else if (v instanceof JsonNode) {
                    vt.set("value", (JsonNode) variables.getValue(key));
                }
                variablesSnapshot.add(vt);
            }

        }

        public void setInstructions(String instructions) {
            this.instructions = instructions;
        }

        public void update(String status, String failureReason) {
            this.failureReason = failureReason;
            this.status = status;
        }

        public void setAdditionalLogInfo(String key, String value) {
            this.additionalLogInfo.put(key, value);
        }

        public void setAdditionalLogInfo(String key, Long value) {
            this.additionalLogInfo.put(key, value);
        }

        public void setAdditionalLogInfo(String key, ArrayNode value) {
            this.additionalLogInfo.set(key, value);
        }

        public void setMemory(ObjectNode memory) {
            this.memory = memory;
        }

        public ObjectNode getJSON(boolean extended) {
            ObjectNode ret = JsonUtils.MAPPER.createObjectNode();
            ObjectNode stepJSON = this.step.getJSON(extended);
            if (extended && this.instructions != null) {
                stepJSON.put("instruction", this.instructions);
            }
            ret.set("step", stepJSON);
            ret.put("status", status);
            ret.put("failureReason", this.failureReason);
            if (extended) {
                ret.set("llmResponse", llmResponse);
                ret.set("additionalLogInfo", additionalLogInfo);
                ret.put("errorMessage", errorMessage);
                ret.set("toolResult", toolResult);
                ret.set("memory", memory);

                if (variablesSnapshot != null) {
                    ret.set("variables", variablesSnapshot);
                }

            }
            return ret;
        }

        public void setLLMResponse(JsonNode llmResponseOriginal) {
            ObjectNode llmResponse = (ObjectNode)llmResponseOriginal.deepCopy(); // We are closing as we need to modify
            this.llmResponse = llmResponse;
            if (llmResponse.has("content")) {
                JsonNode c = llmResponse.get("content");
                if (c != null && c.isTextual()) {
                    String cs = c.asText();
                    if (cs.length() > 0) {
                        try {
                            JsonNode content = JsonUtils.MAPPER.readTree(cs);
                            llmResponse.set("content", content);
                        } catch (JsonProcessingException e) {
                            // Exception in parsing. Set the content as is. Si ignore it.

                        }
                    }
                }
            }
        }

        public void setToolResult(ObjectNode toolResult) {
            this.toolResult = toolResult;
        }

        public void setErrorMessage(String message) {
            this.errorMessage = message;
        }

    }

}
