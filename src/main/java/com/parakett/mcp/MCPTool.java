package com.parakett.mcp;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;

public class MCPTool {

    public String name;
    public JsonNode inputSchema;
    public String description;

    public MCPTool(String name, String description, JsonNode inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }

    /*
     * This method will return the definition needed for tool calling by LLM. Not the exact thing as MCP tool
     * definition. 
     */
    public JsonNode getDefinitionForToolCalling() {
        ObjectNode def = JsonUtils.MAPPER.createObjectNode();
        def.put("name", name);
        def.put("description", description);
        def.set("parameters", inputSchema);
        return def;

    }
    
}
