package com.parakett.cli;

import java.util.Date;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.agent.AgentCliCommMessageHandler;
import com.parakett.channels.base.CommChannelException;
import com.parakett.channels.base.CommChannelInstance;
import com.parakett.channels.base.MessageContext;
import com.parakett.channels.base.OnMessageHandler;
import com.parakett.channels.base.OutputMessage;
import com.parakett.ws.WSClientSessionCache;

public class CliChannelInstance implements CommChannelInstance {

    private AgentCliCommMessageHandler _mAgentMessageHandler = null;

    public void messageReceived(String clientId, String message, ObjectNode options) {
        CliInputMessage ic = new CliInputMessage(message, clientId, options);
        OutputMessage om = _mAgentMessageHandler.onMessageReceived(ic);
        if (om != null) {
            WSClientSessionCache.get(clientId).sendMessageToClient(om.getText());
        }
    }

    @Override
    public void sendMessage(OutputMessage msg) throws CommChannelException {
        // This will be sent as event. Because any response to cli should be sent on the
        // same flow.
        ObjectNode on = null;
        String m = msg.getText();
        if (m.charAt(0) != '{') {
            on = JsonUtils.MAPPER.createObjectNode();
            on.put("event", true);
            on.put("message", m);
        } else {
            try {
                on = (ObjectNode) JsonUtils.MAPPER.readTree(m);
                on.put("event", true);
            } catch (JsonProcessingException e) {
                on = JsonUtils.MAPPER.createObjectNode();
                on.put("event", true);
                on.put("message", m);
            }
        }
        on.put("timestamp", new Date().getTime());
        CliMessageContext ctx = (CliMessageContext) msg.getContext();
        WSClientSessionCache.get(ctx.clientId).sendMessageToClient(on.toString());
    }

    @Override
    public void registerOnMessageHandler(OnMessageHandler handler) {
        if (handler instanceof AgentCliCommMessageHandler) {
            _mAgentMessageHandler = (AgentCliCommMessageHandler) handler;
            return;
        }
        throw new UnsupportedOperationException(
                "CLI channel allows only AgentCliCommMessageHandler instances to be registered");
    }

    @Override
    public void initialize() throws CommChannelException {
    }

    @Override
    public void getConfirmationResponse(MessageContext context, String message, String responseKey, String requestId)
            throws CommChannelException {
        CliMessageContext ctx = (CliMessageContext) context;
        ObjectNode messageObject = JsonUtils.MAPPER.createObjectNode();
        messageObject.put("type", "userConfirmation");
        messageObject.put("message", message);
        messageObject.put("responseKey", responseKey);
        messageObject.put("requestId", requestId);

        WSClientSessionCache.get(ctx.clientId).sendMessageToClient(messageObject.toString());
    }

}
