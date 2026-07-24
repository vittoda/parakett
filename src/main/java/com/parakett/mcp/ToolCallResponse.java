package com.parakett.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolCallResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolCallResponse.class);

    public enum ToolCallStatus {
        SUCCESS,
        FAILED,
    }

    public ToolCallStatus status;

    private String _mErrorType = null;
    private String _mErrorMessage = null;
    private Integer _mErrorCode = null;
    private JsonNode _mErrorAdditionalData = null;

    private JsonNode _mResult;

    private ToolCallResponse() {

    }

    static ToolCallResponse fromResponse(JsonNode r) throws MCPException {
        ToolCallResponse res = new ToolCallResponse();
        if (r.has("error")) {
            res.status = ToolCallStatus.FAILED;
            JsonNode error = r.get("error");
            JsonNode errorData = error.get("data");
            res._mErrorType = errorData.get("errorType").asText();
            if (errorData.has("additionalData") && (!errorData.get("additionalData").isNull())) {
                res._mErrorAdditionalData = errorData.get("additionalData");
            }

            res._mErrorCode = error.get("code").asInt();
            res._mErrorMessage = error.get("message").asText();
        } else {
            res.status = ToolCallStatus.SUCCESS;
            if((!r.has("result")) || r.get("result").isNull()) {
                LOGGER.warn("Tool response '{}'",r.toPrettyString());
                throw new MCPException("Tool call suceeded. But respnse did not have any 'result' field");
            }
            JsonNode resultNode = r.get("result");
            if((!resultNode.has("content")) || resultNode.get("content").isNull()) {
                LOGGER.warn("Tool response '{}'",r.toPrettyString());
                throw new MCPException("Tool call suceeded. But respnse did not have any 'content' field inside 'result' node");
            }
            ArrayNode contents = (ArrayNode)resultNode.get("content");
            if(contents.size() == 0) {
                 throw new MCPException("Tool call suceeded. But 'content' in the response is empty");
            }
            JsonNode contentNode = contents.get(0);
            String type = contentNode.get("type").asText();
            if (!type.equals("text")) {
                throw new MCPException(
                        "Invalid tool response. Content type , 'text' was expected. Found '" + type + "'");
            }
            JsonNode textNode = contentNode.get("text");
            try {
                res._mResult = JsonUtils.MAPPER.readTree(textNode.textValue());
            } catch (Exception e) {
                LOGGER.warn("Not able to parse the content as JSON. Passing it as it is");
                res._mResult = textNode;
            }
        }
        return res;
    }

    public ObjectNode getResultJSON() {
        ObjectNode r = JsonUtils.MAPPER.createObjectNode();
        if (status == ToolCallStatus.FAILED) {
            r.put("toolStatus", "Failed");
            r.put("failureReason", _mErrorType);
            r.put("errorCode", _mErrorCode);
            r.put("errorMessage", _mErrorMessage);
            if(_mErrorAdditionalData != null) {
                r.set("additionalData", _mErrorAdditionalData);
            }

            return r;
        }
        else {
            r.put("toolStatus", "Success");
            r.set("toolResult", _mResult);
        }

        return r;
    }

}
