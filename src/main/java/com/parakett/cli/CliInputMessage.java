package com.parakett.cli;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.channels.base.InputMessage;
import com.parakett.channels.base.MessageContext;

public class CliInputMessage extends InputMessage {

    private ObjectNode _mOptions = null;

    private CliMessageContext _mMessageContext = null;

    public CliInputMessage(String message, String clientId, ObjectNode options) {
        super(message);
        _mMessageContext = new CliMessageContext(clientId);
        this._mOptions = options;
    }

    public MessageContext getContext() {
        return _mMessageContext;
    }

    public ObjectNode getOptions() {
        return _mOptions;
    }

}

