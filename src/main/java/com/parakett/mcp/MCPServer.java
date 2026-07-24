package com.parakett.mcp;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;

public class MCPServer {
    public String name;
    public String version;
    public String category;

    public boolean hasErrors = false;

    private HashMap<String, MCPTool> _mTools = new HashMap<>();

    private String _mConnectionType = null;
    private String _mURL = null, _mCommand = null;

    private MCPServer() {

    }

    public MCPServerInstance createInstance() {
        return new MCPServerInstance(this, getConnection());
    }

    private MCPComm getConnection() {
        MCPComm conn = null;
        if (_mConnectionType.equals("websocket")) {
            conn = new MCPWebsocketComm(_mURL);
        } else if (_mConnectionType.equals("stdio")) {
            conn = new MCPStdInOutComm(_mCommand, this.name);
        }

        return conn;

    }

    public MCPTool getToolByName(String toolName) {
        return _mTools.get(toolName);
    }

    public void setHasErrors(boolean hasErrors) {
        this.hasErrors = hasErrors;
    }

    public static MCPServer fromJSON(JsonNode json) throws MCPException {
        MCPServer ms = new MCPServer();
        ms._fromJSON(json);
        return ms;
    }

    public void initialize() throws MCPException {
        // Prepare the request
        ObjectNode request = JsonUtils.MAPPER.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", UUID.randomUUID().toString());
        request.put("method", "initialize");

        MCPComm conn = getConnection();
        JsonNode result = conn.sendRequest(request);
        result = result.get("result");

        JsonNode serverInfo =result.get("serverInfo");
        this.name = serverInfo.get("name").asText();
        this.version = serverInfo.get("version").asText();

        loadToolsDef(conn);
        conn.close();
    }

    private void _fromJSON(JsonNode json) throws MCPException {
        String id = null;
        if (json.has("id")) {
            id = json.get("id").asText();
        }
        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        if (!json.has("name")) {
            throw new MCPException("'name' field is mandatory");
        }

        this.name = json.get("name").asText();
        this.category = "Saas";
        if (json.has("category")) {
            this.category = json.get("category").asText();
        }

        JsonNode connectionDef = json.get("connection");

        String connType = connectionDef.get("type").asText();
        _mConnectionType = connType;
        if (connType.equals("websocket")) {
            _mURL = connectionDef.get("url").asText();
        } else if (connType.equals("stdio")) {
            _mCommand = json.get("command").asText();
        }

        
    }

    private void _processToolsDef(ArrayNode tools) throws MCPException {
        int len = tools.size();
        for (int i = 0; i < len; i++) {
            JsonNode t =  tools.get(i);
            String name = t.get("name").asText();
            String description = t.get("description").asText();
            JsonNode inputSchema = t.get("inputSchema");
            _mTools.put(name, new MCPTool(name, description, inputSchema));
        }
    }

    private void loadToolsDef(MCPComm conn) throws MCPException {

        // Prepare the request
        ObjectNode request = JsonUtils.MAPPER.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", UUID.randomUUID().toString());
        request.put("method", "tools/list");

        JsonNode result = conn.sendRequest(request);
        result = result.get("result");

        if (result.has("tools")) {
            ArrayNode tools = (ArrayNode) result.get("tools");
            _processToolsDef(tools);
        }

    }

    public List<String> getToolNames(boolean includeServerPrefix) {
        List<String> ret = new LinkedList<>();
        for (String key : _mTools.keySet()) {
            if (includeServerPrefix) {
                ret.add(name + "_" + key);
            } else {
                ret.add(key);
            }
        }

        return ret;

    }

    public ArrayNode getToolListJSON() {
        ArrayNode node = JsonUtils.MAPPER.createArrayNode();
        for (String key : _mTools.keySet()) {
            node.add(_mTools.get(key).getDefinitionForToolCalling());
        }

        return node;
    }

}
