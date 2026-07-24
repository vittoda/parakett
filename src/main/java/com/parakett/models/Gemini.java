package com.parakett.models;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.Keys;
import com.parakett.flow.FlowMemory;
import com.parakett.metrics.MetricsDB;
import com.parakett.metrics.MetricsException;
import com.parakett.models.ModelToolResponse.ToolCall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Gemini extends ModelConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(Gemini.class);

    private static HttpClient _mClient = null;
    private static final int THROTTLE_ERROR_RETRY_COUNT = 2;

    private String _mModelName = null;
    private boolean _mLogRequests = false;

    public Gemini(String modelName) {
        _mModelName = modelName;
        _mLogRequests = System.getProperty("gemini", "false").equals("true");
    }

    @Override
    public ModelResponse sendRequest(FlowMemory memory,
            List<String> additionalSystemMessages,
            List<String> userMessages, List<ToolMessage> tools, boolean jsonResponse) throws ModelException {

        String key = Keys.getCred("parakett.gemini");
        if (key == null) {
            throw new ModelException(
                    "Access key not found. Get the access key and add it at ~/.fskeys. Use the key 'parakett.gemini'");
        }

        ObjectNode req = JsonUtils.MAPPER.createObjectNode();
        ObjectNode systemInstruction = JsonUtils.MAPPER.createObjectNode();
        req.set("systemInstruction", systemInstruction);

        ArrayNode parts = JsonUtils.MAPPER.createArrayNode();
        systemInstruction.set("parts", parts);

        List<ModelMessage> messages = memory.getAllMessages();
        for (ModelMessage m : messages) {
            if (m instanceof ModelSystemMessage) {
                ModelSystemMessage ms = (ModelSystemMessage) m;
                ObjectNode text = JsonUtils.MAPPER.createObjectNode();
                text.put("text", ms.content);
                parts.add(text);
            }
        }

        ArrayNode contents = JsonUtils.MAPPER.createArrayNode();
        req.set("contents", contents);

        // Add the memory
        for (ModelMessage m : messages) {
            if (m instanceof ModelUserMessage) {
                ObjectNode content = JsonUtils.MAPPER.createObjectNode();
                contents.add(content);
                content.put("role", "user");
                ArrayNode userNodeParts = JsonUtils.MAPPER.createArrayNode();
                content.set("parts", userNodeParts);
                ModelUserMessage um = (ModelUserMessage) m;
                for (String c : um.content) {
                    ObjectNode text = JsonUtils.MAPPER.createObjectNode();
                    text.put("text", c);
                    userNodeParts.add(text);
                }
            } else if (m instanceof ModelAssistantMessage) {
                ModelAssistantMessage ma = (ModelAssistantMessage) m;
                ObjectNode content = JsonUtils.MAPPER.createObjectNode();
                contents.add(content);

                content.put("role", "model");
                ArrayNode modelNodeParts = JsonUtils.MAPPER.createArrayNode();
                content.set("parts", modelNodeParts);

                ObjectNode msg = JsonUtils.MAPPER.createObjectNode();
                if (ma.toolCalls != null) {
                    msg.set("functionCall", ma.toolCalls);
                } else {
                    msg.put("text", ma.content);
                }
                modelNodeParts.add(msg);
            } else if (m instanceof ToolResponseMessage) {
                ToolResponseMessage ma = (ToolResponseMessage) m;
                ObjectNode content = JsonUtils.MAPPER.createObjectNode();
                contents.add(content);

                content.put("role", "user");
                ArrayNode toolResponseParts = JsonUtils.MAPPER.createArrayNode();
                content.set("parts", toolResponseParts);

                ObjectNode msg = JsonUtils.MAPPER.createObjectNode();
                toolResponseParts.add(msg);
                ObjectNode functionResponse = JsonUtils.MAPPER.createObjectNode();
                msg.set("functionResponse", functionResponse);
                functionResponse.put("name", ma.name);
                ObjectNode output = JsonUtils.MAPPER.createObjectNode();
                functionResponse.set("response", output);
                if (ma.result != null) {
                    output.put("output", ma.result);
                }
            }
        }

        ObjectNode content = JsonUtils.MAPPER.createObjectNode();
        contents.add(content);
        content.put("role", "user");
        parts = JsonUtils.MAPPER.createArrayNode();
        content.set("parts", parts);

        for (String m : userMessages) {
            ObjectNode text = JsonUtils.MAPPER.createObjectNode();
            text.put("text", m);
            parts.add(text);
        }

        // Prepare tools
        if (tools != null && tools.size() > 0) {
            ArrayNode toolsList = JsonUtils.MAPPER.createArrayNode();
            ObjectNode toolsInstance = JsonUtils.MAPPER.createObjectNode();
            toolsList.add(toolsInstance);
            ArrayNode functionDeclarations = JsonUtils.MAPPER.createArrayNode();
            toolsInstance.set("functionDeclarations", functionDeclarations);
            for (ToolMessage tm : tools) {
                functionDeclarations.add(tm.toolDefinition);
            }
            req.set("tools", toolsList);
        }

        if (_mClient == null) {
            _mClient = HttpClient.newHttpClient();
        }

        if (_mLogRequests) {
            LOGGER.info(req.toPrettyString());
        }
        String URL = "https://generativelanguage.googleapis.com/v1beta/models/" + _mModelName
                + ":generateContent?key="
                + key;
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(req.toString()))
                .build();

        int retryInterval = 5000; // 5 seconds
        int timerMultiplier = 1;

        try {
            Date requestStartTime = new Date();
            HttpResponse<String> response = _mClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString());
            Date endTime = new Date();
            int statusCode = response.statusCode();
            try {
                MetricsDB.addRequestMetricForModel(_mModelName, requestStartTime, endTime, statusCode);
            } catch (MetricsException e) {
                LOGGER.warn("Error saving token metric." , e.getMessage());
            }

            if (statusCode == 429 || statusCode == 503) {
                int retryCount = 0;
                while (retryCount < THROTTLE_ERROR_RETRY_COUNT && (statusCode == 429 || statusCode == 503)) {
                    Thread.sleep(retryInterval);
                    requestStartTime = new Date();
                    response = _mClient.send(
                            httpRequest,
                            HttpResponse.BodyHandlers.ofString());
                    endTime = new Date();
                    statusCode = response.statusCode();
                    try {
                        MetricsDB.addRequestMetricForModel(URL, requestStartTime, endTime, statusCode);
                    } catch (MetricsException e) {
                        LOGGER.warn("Error saving token metric." , e.getMessage());
                    }
                    retryCount++;
                    timerMultiplier++;
                    retryInterval = 5000 * timerMultiplier;
                }
            }

            if (statusCode == 403 || statusCode == 401) {
                throw new ModelNonRecoverableException("Model requests unthentication error. Non-recoverable.");
            }
            if (statusCode == 429) {
                throw new ModelNonRecoverableException("Model requests are throttled. Non-recoverable.");
            }
            if (statusCode != 200) {
                throw new ModelException(
                        "API responded with status code '" + statusCode + "'.\n Message : " + response.body());
            }

            // Request success. Now add additional system messages and user messages to
            if (additionalSystemMessages != null && additionalSystemMessages.size() > 0) {
                for (String s : additionalSystemMessages) {
                    memory.addContent(new ModelSystemMessage(s));
                }
            }

            if (userMessages != null && userMessages.size() > 0) {
                memory.addContent(new ModelUserMessage(userMessages));
            }

            String responseBody = response.body();
            return handleResponse(memory, JsonUtils.MAPPER.readTree(responseBody), jsonResponse);
        } catch (IOException | InterruptedException e) {
            throw new ModelException(e);
        }

    }

    private ModelResponse handleResponse(FlowMemory memory, JsonNode response, boolean jsonRespnse)
            throws ModelException {

        if (response.has("usageMetadata")) {
            JsonNode usageMetadata = response.get("usageMetadata");
            int outputTokenSize = usageMetadata.get("candidatesTokenCount").asInt();
            int inputTokenSize = usageMetadata.get("promptTokenCount").asInt();

            try {
                MetricsDB.addTokenMetric(_mModelName, inputTokenSize, outputTokenSize);
            } catch (Exception e) {
                LOGGER.warn("Error saving token metric." , e.getMessage());
            }
        }

        if (!response.has("candidates")) {
            return null;
        }
        ArrayNode candidates = (ArrayNode) response.get("candidates");

        if (candidates.size() == 0 || (!candidates.get(0).has("content"))) {
            throw new ModelNonRecoverableException(
                    "Invalid response from model. It does not contain expected fields. Error code 00100");
        }

        JsonNode content =  (candidates.get(0)).get("content");
        ArrayNode parts = (ArrayNode) content.get("parts");
        if (parts.size() == 0) {
            throw new ModelNonRecoverableException(
                    "Invalid response from model. It does not contain expected fields. Error code 00101");
        }

        // We will use only one text respinse. Not sure on what case it will have more.
        JsonNode part = parts.get(0);
        if (part.has("text")) {
            String m = part.get("text").asText();
            memory.addContent(new ModelAssistantMessage(m, null));
            if (jsonRespnse) {
                try {
                    if (m.startsWith("```") && m.endsWith("```")) {
                        m = m.substring(7, m.length() - 3);
                    }
                    JsonNode contentJSON = JsonUtils.MAPPER.readTree(m);
                    LOGGER.info(contentJSON.toPrettyString());

                    return new ModelJSONResponse(part, contentJSON);
                } catch (JsonProcessingException e) {
                    throw new ModelNonRecoverableException(
                            "Error parsing the response as JSON from the model. Message recieved : " + m);
                }
            }
            return new ModelTextResponse(part, m);
        } else if (part.has("functionCall")) {
            JsonNode functionCall =  part.get("functionCall");
            memory.addContent(new ModelAssistantMessage(null, functionCall));
            String name = functionCall.get("name").asText();
            JsonNode args = functionCall.get("args");
            ToolCall tc = new ToolCall(name, null, args);
            ModelToolResponse mt = new ModelToolResponse(part);
            mt.addToolCall(tc);
            return mt;
        }
        memory.addContent(new ModelAssistantMessage(part.toString(), null));
        return new ModelResponse(part);

    }

}
