package com.parakett.mcp;

public class MCPException extends Exception{

    public MCPException(String e) {
        super(e);
    }

    public MCPException(Exception e) {
        super(e);
    }
    
}
