package com.parakett.agent;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.channels.base.CommChannelInstance;
import com.parakett.channels.base.InputMessage;
import com.parakett.channels.base.OnMessageHandler;
import com.parakett.channels.base.OutputMessage;
import com.parakett.cli.CliInputMessage;
import com.parakett.flow.FlowException;
import com.parakett.flow.FlowRunner;

public class AgentCliCommMessageHandler implements OnMessageHandler {

    public Agent agent = null;
    private CommChannelInstance _mChannelInstance = null;

    public AgentCliCommMessageHandler(Agent agent, CommChannelInstance channelInstance) {
        this.agent = agent;
        this._mChannelInstance = channelInstance;
    }

    @Override
    public OutputMessage onMessageReceived(InputMessage msg) {

        CliInputMessage im = (CliInputMessage)msg;
        String userMessage = "Message Source : CLI communication channel event\n\n" + msg.getText() ;
        ObjectNode options = im.getOptions();
        boolean archive = false;
        boolean logging = false;
        if(options.has("archive")) {
            archive = options.get("archive").asBoolean();
        }
        if(options.has("logging")) {
            logging = options.get("logging").asBoolean();
        }
        try {
            FlowRunner runner = agent.run(userMessage, null, null, false, logging, archive, 
                    msg.getContext(), _mChannelInstance,
                    null);
            return new OutputMessage("Session Id : " + runner.getSessionId(), msg.getContext());
        } catch (FlowException e) {
            ObjectNode result = JsonUtils.MAPPER.createObjectNode();
            result.put("status", "error");
            result.put("message", e.getMessage());
            return new OutputMessage(result.toString(), msg.getContext());
        } 
    }

}
