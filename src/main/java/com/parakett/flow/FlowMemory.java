package com.parakett.flow;

import java.util.LinkedList;
import java.util.List;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.models.ModelMessage;

public class FlowMemory {

    private List<ModelMessage> _mMessages = new LinkedList<>();

    public void addContent(ModelMessage content) {
        _mMessages.add(content);
    }

    public ObjectNode getMergedSnapshot() {

        ObjectNode node = JsonUtils.MAPPER.createObjectNode();
        node.put("text","Not implemented");
        return node;
    }

    public List<ModelMessage> getAllMessages() {
        return _mMessages;
    }

    public void clear() {
        _mMessages.clear();
    }
    
}
