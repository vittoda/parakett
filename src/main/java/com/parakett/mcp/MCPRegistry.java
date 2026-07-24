package com.parakett.mcp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.flow.AgentTool;

public class MCPRegistry {

    private static HashMap<String, MCPServer> _mServers = new HashMap<>();

    public static void add(MCPServer server) {
        _mServers.put(server.name, server);
    }

    public static MCPServer getServer(String serverName) {
        return _mServers.get(serverName);
    }

    public static JsonNode getToolDefinitionForToolCalling(String fullToolName) {
        if (fullToolName.startsWith("agent_")) {
            return AgentTool.getDefinitionForFunctionCalling(fullToolName);
        }
        int index = fullToolName.indexOf("_");
        String serverName = fullToolName.substring(0, index);
        String toolName = fullToolName.substring(index + 1);

        MCPServer server = _mServers.get(serverName);
        MCPTool tool = server.getToolByName(toolName);
        ObjectNode def = (ObjectNode)tool.getDefinitionForToolCalling();
        // We need to override the name to prefix with server name;
        def.put("name", serverName + "_" + def.get("name").asText());
        return def;
    }

    public static void initialize(String file) throws MCPException {
        try {
            InputStream is = new FileInputStream(file);

            JsonNode o =  JsonUtils.MAPPER.readTree(is);
            ArrayNode servers = (ArrayNode) o.get("mcpServers");
            int len = servers.size();
            for (int i = 0; i < len; i++) {
                JsonNode serverObject = servers.get(i);
                MCPServer serverDef = MCPServer.fromJSON(serverObject);
                try {
                    serverDef.initialize();
                } catch (MCPException e) {
                    e.printStackTrace();
                    serverDef.setHasErrors(true);
                }

                add(serverDef);
            }

        } catch (IOException e) {
            throw new MCPException(e);
        }
    }

    public static ArrayNode getServersList() {

        ArrayNode list = JsonUtils.MAPPER.createArrayNode();

        for (MCPServer server : _mServers.values()) {
            ObjectNode s = JsonUtils.MAPPER.createObjectNode();
            s.put("name", server.name);
            s.put("hasErrors", server.hasErrors);
            s.put("version", server.version);
            s.put("category", server.category);
            
            list.add(s);
        }

        return list;

    }

    public static List<String> getAllToolNames() {

        List<String> ret = new LinkedList<>();

        for (MCPServer server : _mServers.values()) {
            ret.addAll(server.getToolNames(true));
        }

        ret.add("agent_runStep");
        ret.add("agent_runErrorStep");
        ret.add("agent_sendMessage");
        //ret.add("agent_endFlow"); //We will not let LLM know this tool. This will be added to a endFlow step
        //ret.add("agent_setStepContext");  //We will not let LLM know this tool. This will be added  by server
        

        return ret;
    }

}
