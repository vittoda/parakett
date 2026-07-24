package com.parakett.flow;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.parakett.JsonUtils;
import com.parakett.mcp.ToolCallResponse;


public class AgentTool {

    private static JsonNode _mAllFunctionsDef = null;

    public ToolCallResponse runTool(String toolName, JsonNode arguments) {
        return null;
    }

    public static JsonNode getDefinitionForFunctionCalling(String toolName) {
        if (_mAllFunctionsDef == null) {
            try {
                InputStream is = AgentTool.class
                        .getClassLoader()
                        .getResourceAsStream("agentTools.json");

                String jsonText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                _mAllFunctionsDef =   JsonUtils.MAPPER.readTree(jsonText);

            } catch (Exception e) {
                e.printStackTrace();
                // Ignore. This should not happen.
            }
        }

        return _mAllFunctionsDef.get(toolName);

    }

    public static List<String> getAllToolNames() {

        List<String> ret = new LinkedList<>();

        ret.add("agent_runStep");
        ret.add("agent_runErrorStep");
        ret.add("agent_sendMessage");
        //ret.add("agent_endFlow"); //We will not let LLM know this tool. This will be added to a endFlow step
        //ret.add("agent_setStepContext"); //We will not let LLM know this tool. This will be added  by server


        return ret;
    }


}
