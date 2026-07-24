package com.parakett.ws;

import java.io.IOException;
import java.util.HashMap;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSession.class);

    String clientId = null;
    WebSocketSession websocketSession = null;

    private HashMap<String, String> _mSessionVariables = new HashMap<>();

    public ClientSession(String clientId, WebSocketSession websocketSession) {
        this.clientId = clientId;
        this.websocketSession = websocketSession;
    }

    public String getClientId() {
        return this.clientId;
    }

    public void setVar(String name, String value) {
        _mSessionVariables.put(name, value);
    }

    public String getVar(String name) {
        return _mSessionVariables.get(name);
    }

    public void clearVar(String name) {
        _mSessionVariables.remove(name);
    }

    public ObjectNode getAllVars() {
        ObjectNode items = JsonUtils.MAPPER.createObjectNode();
        for(String key : _mSessionVariables.keySet()) {
            items.put(key, _mSessionVariables.get(key));
        }

        return items;
    }

    public void sendMessageToClient(String message) {
        if (this.websocketSession != null && this.websocketSession.isOpen()) {
            try {
                this.websocketSession.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                LOGGER.error("Error sending message to client '{}'." , clientId,e);
            }
        } else {
            LOGGER.info("Client '{}' is not connected.", clientId);
        }
    }
    
}
