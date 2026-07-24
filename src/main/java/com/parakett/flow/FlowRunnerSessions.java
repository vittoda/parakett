package com.parakett.flow;

import java.util.HashMap;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;

public class FlowRunnerSessions {

    private static HashMap<String, FlowRunner> _mFlows = new HashMap<>();

    public static void addFlow(FlowRunner runner) {
        _mFlows.put(runner.getSessionId(), runner);
    }

    public static FlowRunner getBySessionId(String sessionId) {
        return _mFlows.get(sessionId);
    }

     public static ArrayNode getAll() {
        ArrayNode result = JsonUtils.MAPPER.createArrayNode();
        for(String key : _mFlows.keySet()) {
            result.add(_mFlows.get(key).getJSON(false));
        }

        return result;
    }

    public static ArrayNode getListForCli() {
        ArrayNode result = JsonUtils.MAPPER.createArrayNode();
        for(String key : _mFlows.keySet()) {
            FlowRunner fr = _mFlows.get(key);
            ObjectNode on = JsonUtils.MAPPER.createObjectNode();
            on.put("sessionId", fr.getSessionId());
            on.put("status", fr.getStatus());
            on.put("agent", fr.getAgentInstance().id);
            result.add(on);
        }

        return result;
    }

    public static void removeFlowRunner(String sessionId) {
        _mFlows.remove(sessionId);
    }
    
}
