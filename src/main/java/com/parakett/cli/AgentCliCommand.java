package com.parakett.cli;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.agent.Agent;
import com.parakett.agent.AgentRegistry;
import com.parakett.ws.ClientSession;

public class AgentCliCommand extends CliCommand {

    public AgentCliCommand() {
        super("agent");

    }

    @Override
    public CliResponse run(String params, ClientSession session) throws CliException {

        ObjectNode resp = JsonUtils.MAPPER.createObjectNode();

        int subCommandIndex = params.indexOf(" ");
        String subCommand = "list";
        if (subCommandIndex > 0) {
            subCommand = params.substring(0, subCommandIndex);
        }

        switch (subCommand) {
            case "list": {
                ArrayNode agents = AgentRegistry.getAgentList();
                ArrayNode agentNames = JsonUtils.MAPPER.createArrayNode();
                for (int i = 0; i < agents.size(); i++) {
                    ObjectNode c = JsonUtils.MAPPER.createObjectNode();
                    c.put("id", agents.get(i).get("id").asText());
                    c.put("name", agents.get(i).get("name").asText());
                    agentNames.add(c);
                }
                resp.put("status", "status");
                resp.put("type", "table");
                resp.set("message", agentNames);

                return new CliResponse(resp);
            }
            case "details": {
                // Get the agent name
                String agentId = params.substring(subCommandIndex + 1);
                Agent agent = AgentRegistry.getById(agentId);

                resp.put("status", "status");
                resp.put("type", "json");
                resp.set("message", agent.getJSON(true));
                return new CliResponse(resp);
            }
            case "run":
                params = params.substring(subCommandIndex + 1);
                int agentIdIndex = params.indexOf(" ");

                String agentId = params.substring(0, agentIdIndex);
                // Check if this agent really exists
                if (!AgentRegistry.exists(agentId)) {
                    resp.put("status", "error");
                    resp.put("message", "Agent '" + agentId + "' does not exist");
                    return new CliResponse(resp);
                }

                //Parse parameters
                params = params.substring(agentIdIndex + 1);
                params = params.trim();
                ObjectNode options = JsonUtils.MAPPER.createObjectNode();
                while(params.startsWith("--")) {
                    int index = params.indexOf(" ");
                    String option = params.substring(0, index);
                    switch(option) {
                        case "--archive":
                            options.put("archive", true);
                            break;
                        case "--log":
                            options.put("logging", true);
                            break;
                    }
                    params = params.substring(index + 1);
                    params = params.trim();
                }

                String prompt = params;
                CliChannelInstance cl = (CliChannelInstance) CliChannel.INSTANCE.getInstance(agentId);
                cl.messageReceived(session.getClientId(),  prompt, options);

                resp.put("status", "success");
                resp.put("type", "done");
                return new CliResponse(resp);
        }

        resp.put("status", "error");
        resp.put("message", "Invalid agent command.");

        return new CliResponse(resp);
    }

}
