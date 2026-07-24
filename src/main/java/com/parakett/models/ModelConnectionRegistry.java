package com.parakett.models;

import java.util.Date;
import java.util.HashMap;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.parakett.JsonUtils;

public class ModelConnectionRegistry {

    private static HashMap<String, ModelConnection> _mConnections = new HashMap<>();

    private static String _mDefaultModelName = "openai/gpt-4o";

    public static final Date STARTUP_TIME = new Date();

    static {
        _mConnections.put("openai/gpt-4o", new GithubOpenAI("openai/gpt-4o"));
        _mConnections.put("gpt-4o", new OpenAI("gpt-4o"));
        _mConnections.put("gemini-3.1-flash-lite", new Gemini("gemini-3.1-flash-lite"));
    }

    public static ModelConnection getConnectionForModel(String modelName) {
        return _mConnections.get(modelName);
    }

    public static ArrayNode getModelNameList() {
        ArrayNode ret = JsonUtils.MAPPER.createArrayNode();

        for(String name : _mConnections.keySet()) {
            ret.add(name);
        }
        return ret;
    }

    public static void setDefaultModel(String m) {
        _mDefaultModelName = m;
    }

    public static String getDefaultModelName() {
        return _mDefaultModelName;
    }

    public static void initialize() {
        
    }
}
