package com.parakett.cli;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.flow.FlowRunnerSessions;
import com.parakett.ws.ClientSession;

public class SessionsCliCommand extends CliCommand {

    public SessionsCliCommand() {
        super("session");

    }

    @Override
    public CliResponse run(String params, ClientSession session) throws CliException {

        ObjectNode resp = JsonUtils.MAPPER.createObjectNode();

        int subCommandIndex = params.indexOf(" ");
        String subCommand = "list";
        if (subCommandIndex > 0) {
            subCommand = params.substring(0, subCommandIndex);
            params = params.substring(subCommandIndex + 1);
        }
        else if(params.length() > 0) {
            subCommand = params;
            params = "";
        }

        switch (subCommand) {
            case "list": {

                //Get session Ids
                ArrayNode list = FlowRunnerSessions.getListForCli();
                
                resp.put("status", "success");
                resp.put("type", "table");
                resp.set("message", list);

                return new CliResponse(resp);
            }
        }

        resp.put("status", "error");
        resp.put("message", "Invalid session command.");

        return new CliResponse(resp);

    }
    
}
