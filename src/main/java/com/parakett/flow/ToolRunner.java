package com.parakett.flow;



import com.fasterxml.jackson.databind.JsonNode;
import com.parakett.agent.Agent;
import com.parakett.mcp.MCPException;
import com.parakett.mcp.MCPServerInstance;
import com.parakett.mcp.ToolCallResponse;

public class ToolRunner {

    private String _mServerName = null;
    private String _mToolName = null;

    private JsonNode _mArguments = null;

    public ToolRunner(String functionCallName, JsonNode arguments) {
        int index = functionCallName.indexOf("_");
        _mServerName = functionCallName.substring(0, index);
        _mToolName = functionCallName.substring(index+1);
        _mArguments = arguments;
    }

    public ToolCallResponse run(Agent agentInstance) throws FlowException{
        if(_mServerName.equals("agent")) {
            //This is an internal tool.
            AgentTool at = new AgentTool();
            return at.runTool(_mToolName, _mArguments);
        }
        MCPServerInstance server =agentInstance.getMCPServerInstance(_mServerName);
        try {
            return server.runTool(_mToolName, _mArguments);
        }
        catch(MCPException e) {
            e.printStackTrace();
            throw new FlowException(e);
        }
    }
    
}
