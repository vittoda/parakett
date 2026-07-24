package com.parakett.models;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

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

public class OpenAI extends ModelConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAI.class);

    private static HttpClient _mClient = null;

    private static final int THROTTLE_ERROR_RETRY_COUNT = 0;

    private String _mModelName = null;
    private double _mTemperature = 0.2;

    private boolean _mLogRequests = false;

    private boolean _mRecording = false;
    private boolean _mPlayback = false;
    private String _mRecordingFile = null;

    public OpenAI(String modelName) {
        _mModelName = modelName;
        _mLogRequests = System.getProperty("openAI.model.logRequests", "false").equals("true");
        _mRecording = System.getProperty("fs.openai.recording", "").equals("true");
        _mPlayback = System.getProperty("fs.openai.playback", "").equals("true");
        if (_mRecording || _mPlayback) {
            _mRecordingFile = System.getProperty("parakett.openai.recording.file", "/tmp/openai");
        }
    }

    protected String getCred() {
        return Keys.getCred(getCredKey());
    }

    protected String getCredKey() {
        return "parakett.openai";
    }

    protected boolean logRequests() {
        return _mLogRequests;
    }

    protected String getURL() {
        return "https://api.openai.com/v1/chat/completions";
    }

    @Override
    public ModelResponse sendRequest(FlowMemory memory,
            List<String> additionalSystemMessages,
            List<String> userMessages, List<ToolMessage> tools, boolean jsonResponse) throws ModelException

    {
        String key = getCred();
        if (key == null) {
            throw new ModelException(
                    "Access key not found. Get the access key and add it at ~/.fskeys. Use the key  '" + getCredKey()
                            + "'");
        }

        ObjectNode request = JsonUtils.MAPPER.createObjectNode();
        request.put("model", _mModelName);

        // Let us add system messages.
        ArrayNode messages = JsonUtils.MAPPER.createArrayNode();
        request.set("messages", messages);

        // Add memory
        List<ModelMessage> modelMessages = memory.getAllMessages();
        for (ModelMessage mm : modelMessages) {
            if (mm instanceof ModelSystemMessage) {
                ModelSystemMessage ms = (ModelSystemMessage) mm;
                ObjectNode msg = JsonUtils.MAPPER.createObjectNode();
                msg.put("role", "system");
                msg.put("content", ms.content);
                messages.add(msg);
            } else if (mm instanceof ModelAssistantMessage) {
                ModelAssistantMessage ma = (ModelAssistantMessage) mm;
                ObjectNode msg = JsonUtils.MAPPER.createObjectNode();
                msg.put("role", "assistant");
                if (ma.toolCalls != null) {
                    msg.set("tool_calls", ma.toolCalls);
                } else {
                    msg.put("content", ma.content);
                }
                messages.add(msg);
            } else if (mm instanceof ToolResponseMessage) {
                ToolResponseMessage m = (ToolResponseMessage) mm;
                ObjectNode msg = JsonUtils.MAPPER.createObjectNode();
                msg.put("role", "tool");
                msg.put("tool_call_id", m.toolCallId);
                msg.put("name", m.name);
                msg.put("content", m.result);
                messages.add(msg);
            } else if (mm instanceof ModelUserMessage) {
                ModelUserMessage ms = (ModelUserMessage) mm;
                for (String um : ms.content) {
                    ObjectNode msg = JsonUtils.MAPPER.createObjectNode();
                    msg.put("role", "user");
                    msg.put("content", um);
                    messages.add(msg);
                }
            }
        }

        if (additionalSystemMessages != null) {
            for (String m : additionalSystemMessages) {
                ObjectNode msg = JsonUtils.MAPPER.createObjectNode();
                msg.put("role", "system");
                msg.put("content", m);
                messages.add(msg);
            }
        }

        // User messages
        if (userMessages != null) {
            for (String m : userMessages) {
                ObjectNode msg = JsonUtils.MAPPER.createObjectNode();
                msg.put("role", "user");
                msg.put("content", m);
                messages.add(msg);
            }
        }

        // Add tools.
        if (tools != null && tools.size() > 0) {
            ArrayNode toolsArray = JsonUtils.MAPPER.createArrayNode();
            for (ToolMessage t : tools) {
                ObjectNode function = JsonUtils.MAPPER.createObjectNode();
                function.put("type", "function");
                function.set("function", t.toolDefinition);
                toolsArray.add(function);
            }
            request.set("tools", toolsArray);
        }

        request.put("temperature", _mTemperature);
        // request.put("max_tokens", _mMaxTokens);
        if (jsonResponse) {
            ObjectNode responseType = JsonUtils.MAPPER.createObjectNode();
            responseType.put("type", "json_object");
            request.set("response_format", responseType);
        }

        if (_mClient == null) {
            _mClient = HttpClient.newHttpClient();
        }
        if (_mLogRequests) {
            LOGGER.info(request.toPrettyString());
        }

        int retryInterval = 5000; // 5 seconds
        int timerMultiplier = 1;
        try {
            JsonNode response = null;
            if (_mPlayback) {
                LOGGER.info("Playing back the response");
                response = getResponse(request, _mRecordingFile + "_requests.json",
                        _mRecordingFile + "_responses.json");
                if (response == null) {
                    throw new ModelException("Playback mode returned null response");
                }
            } else {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(getURL()))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + getCred())
                        .POST(HttpRequest.BodyPublishers.ofString(request.toString()))
                        .build();

                Date requestStartTime = new Date();
                HttpResponse<String> httpResponse = _mClient.send(
                        httpRequest,
                        HttpResponse.BodyHandlers.ofString());
                Date endTime = new Date();
                int statusCode = httpResponse.statusCode();
                try {
                    MetricsDB.addRequestMetricForModel(_mModelName, requestStartTime, endTime, statusCode);
                } catch (MetricsException e) {
                    LOGGER.warn("Error saving token metric. ", e.getMessage());
                }

                if (statusCode == 429 || statusCode == 503) {
                    int retryCount = 0;
                    while (retryCount < THROTTLE_ERROR_RETRY_COUNT && (statusCode == 429 || statusCode == 503)) {
                        Thread.sleep(retryInterval);
                        requestStartTime = new Date();
                        httpResponse = _mClient.send(
                                httpRequest,
                                HttpResponse.BodyHandlers.ofString());
                        endTime = new Date();
                        statusCode = httpResponse.statusCode();
                        try {
                            MetricsDB.addRequestMetricForModel(_mModelName, requestStartTime, endTime, statusCode);
                        } catch (MetricsException e) {
                            LOGGER.warn("Error saving token metric. ", e.getMessage());
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
                            "API responded with status code '" + statusCode + "'.\n Message : " + httpResponse.body());
                }

                String responseBody = httpResponse.body();
                response = JsonUtils.MAPPER.readTree(responseBody);
                if (_mRecording) {
                    dumpRequestAndResponse(request, response,
                            _mRecordingFile + "_requests.json", _mRecordingFile + "_responses.json");
                }
            }

            // Request success. Now add additional system messages and user messages to
            // memory.
            if (additionalSystemMessages != null && additionalSystemMessages.size() > 0) {
                for (String s : additionalSystemMessages) {
                    memory.addContent(new ModelSystemMessage(s));
                }
            }

            if (userMessages != null && userMessages.size() > 0) {
                memory.addContent(new ModelUserMessage(userMessages));
            }

            return handleResponse(memory, response, jsonResponse);
        } catch (IOException | InterruptedException e) {
            throw new ModelException(e);
        }

    }

    private ModelResponse handleResponse(FlowMemory memory, JsonNode response, boolean jsonRespnse)
            throws ModelException {

        if (response.has("usage")) {
            JsonNode usageMetadata = response.get("usage");
            int outputTokenSize = usageMetadata.get("completion_tokens").asInt();
            int inputTokenSize = usageMetadata.get("prompt_tokens").asInt();

            try {
                MetricsDB.addTokenMetric(_mModelName, inputTokenSize, outputTokenSize);
            } catch (Exception e) {
                LOGGER.warn("Error saving token metric. ", e.getMessage());
            }
        }
        if (!response.has("choices")) {
            return null;
        }

        if (_mLogRequests) {
            LOGGER.info(response.toPrettyString());
        }

        ArrayNode choices = (ArrayNode) response.get("choices");
        JsonNode choice = choices.get(0); // Asume I will get at least one.
        JsonNode message = choice.get("message");
        // Tool calls gets the preference

        if (message.has("tool_calls") && (!message.get("tool_calls").isNull())) {
            ArrayNode tool_calls = (ArrayNode) message.get("tool_calls");

            // Push it to the memory.
            memory.addContent(new ModelAssistantMessage(null, tool_calls));

            ModelToolResponse mc = new ModelToolResponse(message);

            for (int i = 0; i < tool_calls.size(); i++) {
                JsonNode tool = tool_calls.get(i);
                JsonNode function = tool.get("function");
                String name = function.get("name").asText();
                String id = tool.get("id").asText();
                JsonNode argumentsNode = function.get("arguments");

                JsonNode arguments;
                if (argumentsNode.isObject()) {
                    arguments = argumentsNode;
                } else {
                    try {
                        arguments = JsonUtils.MAPPER.readTree(argumentsNode.asText());
                    } catch (JsonProcessingException e) {
                        throw new ModelException(e);
                    }
                }
                ToolCall tc = new ToolCall(name, id, arguments);
                mc.addToolCall(tc);
            }

            return mc;
        } else if (message.has("content") && (!message.get("content").isNull())) {
            String m = message.get("content").asText();
            memory.addContent(new ModelAssistantMessage(m, null));
            if (jsonRespnse) {
                try {
                    JsonNode contentJSON = JsonUtils.MAPPER.readTree(m);
                    return new ModelJSONResponse(message, contentJSON);
                } catch (JsonProcessingException e) {
                    throw new ModelException(e);
                }
            }

            return new ModelTextResponse(message, m);

        } else {
            memory.addContent(new ModelAssistantMessage(message.toString(), null));
            return new ModelResponse(message);
        }
    }

    private void dumpRequestAndResponse(JsonNode request, JsonNode response, String requestFile,
            String responseFile) {
        String uid = UUID.randomUUID().toString();

        try {
            ObjectNode on = null;
            if (new File(requestFile).exists()) {
                on = (ObjectNode) JsonUtils.MAPPER.readTree(new File(requestFile));
            } else {
                on = JsonUtils.MAPPER.createObjectNode();
            }
            on.set(uid, request);
            FileOutputStream fos = new FileOutputStream(new File(requestFile));
            fos.write(on.toString().getBytes());
            fos.close();

            if (new File(responseFile).exists()) {
                on = (ObjectNode) JsonUtils.MAPPER.readTree(new File(responseFile));
            } else {
                on = JsonUtils.MAPPER.createObjectNode();
            }
            on.set(uid, response);
            fos = new FileOutputStream(new File(responseFile));
            fos.write(on.toString().getBytes());
            fos.close();
        } catch (IOException e) {
            LOGGER.warn("Not able to write the request file.", e);
        }
    }

    private JsonNode getResponse(JsonNode request, String requestFile, String responseFile) {
        try {
            // Load the request file and get the key
            JsonNode on = JsonUtils.MAPPER.readTree(new File(requestFile));
            Iterator<String> fieldNames = on.fieldNames();
            String requestKey = null;
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                JsonNode value = on.get(key);
                if (value.equals(request)) {
                    requestKey = key;
                    break;
                }
            }

            if (requestKey == null) {
                LOGGER.warn("Request key is null for the above request.");
                return null;
            }
            LOGGER.info("Finding rsponse for key '{}]", requestKey);
            on = JsonUtils.MAPPER.readTree(new File(responseFile));
            return on.get(requestKey);

        } catch (IOException e) {
            LOGGER.warn("Not able to write the request file.", e);
            return null;
        }
    }

}
