package com.parakett.mcp;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class MCPWebsocketComm extends MCPComm {


    public MCPWebsocketComm(String url) {
    }

    @Override
    public ObjectNode sendRequest(ObjectNode request) throws MCPException {
       throw new UnsupportedOperationException("Unimplemented method 'close'");
    }

    @Override
    public void close() throws MCPException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'close'");
    }

}
