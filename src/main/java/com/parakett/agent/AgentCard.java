package com.parakett.agent;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class AgentCard {

    private String _mId = null;
    private Agent _mAgentInstance = null;
    private boolean _mIsRemote = false;

    public AgentCard(String id, Agent agentInstance, boolean isRemote) {
        this._mId = id;
        this._mAgentInstance = agentInstance;
        this._mIsRemote = isRemote;
    }

    public String getId() {
        return _mId;
    }

    public Agent getAgentInstance() {
        return _mAgentInstance;
    }

    public boolean isRemote() {
        return _mIsRemote;
    }

    public ObjectNode getAgentInfo(boolean extended) {
        return this._mAgentInstance.getJSON(extended);
    }

}
