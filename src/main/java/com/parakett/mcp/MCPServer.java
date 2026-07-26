package com.parakett.mcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCPServer {

    private static final String[] VALID_INPUT_TYPES = { "string", "number", "integer", "boolean", "object", "array",
            "null" };

    private static final Logger LOGGER = LoggerFactory.getLogger(MCPServer.class);

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

        JsonNode serverInfo = result.get("serverInfo");
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
            JsonNode t = tools.get(i);
            String toolName = t.get("name").asText();
            String description = t.get("description").asText();
            JsonNode inputSchema = t.get("inputSchema");
            ArrayList<String> errors = new ArrayList<>();
            validateInputSchema(inputSchema, "", false, errors);

            if (errors.size() > 0) {
                StringBuilder b = new StringBuilder("Input schema validation error for tool '" + toolName
                        + "' in MCP server " + this.name + "'. Validation errors \n");
                for (String s : errors) {
                    b.append(s).append("\n");
                }
                LOGGER.warn("Invalid tool definition '{}'", inputSchema.toPrettyString());
                throw new InvalidToolDefinitionException(b.toString());
            }
            _mTools.put(toolName, new MCPTool(toolName, description, inputSchema));
        }
    }

    private void validateInputSchema(JsonNode inputSchema, String name, boolean descriptionRequired,
            ArrayList<String> errors) {
        if (!inputSchema.has("type") || !inputSchema.get("type").isTextual()) {
            errors.add("Invalid schema for property '" + name + "'. 'type' attribute not specified");
            return;
        }

        String type = inputSchema.get("type").asText();
        boolean found = false;
        for (String t : VALID_INPUT_TYPES) {
            if (type.equals(t)) {
                found = true;
                break;
            }
        }

        if (!found) {
            errors.add("Invalid type for property '" + name + "'. Type '" + type + "' is not allowed");
            return;
        }

        if (descriptionRequired) {
            if (!inputSchema.has("description") || !inputSchema.get("description").isTextual()) {
                errors.add("Invalid schema for property '" + name + "'. 'description' attribute not specified");
                return;
            }
        }

        if (type.equals("object")) {
            if (!inputSchema.has("properties") || !inputSchema.get("properties").isObject()) {
                errors.add("'properties' attribute is required for property '" + name
                        + "', with details for all the attributes");
                return;
            }

            JsonNode properties = inputSchema.get("properties");
            Iterator<String> fieldNames = properties.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                JsonNode value = properties.get(key);
                validateInputSchema(value, name + "." + key, true, errors);
            }

        } else if (type.equals("array")) {
            if (!inputSchema.has("items") || !inputSchema.get("items").isObject()) {
                errors.add("'properties' attribute is required for property '" + name
                        + "', with details for all the attributes");
                return;
            }

            validateInputSchema(inputSchema.get("items"), name + "[items]", false, errors);

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
