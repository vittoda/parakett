package com.parakett.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class MCPComm {
    
    public abstract JsonNode sendRequest(ObjectNode request) throws MCPException;
    public abstract void close() throws MCPException;
}
