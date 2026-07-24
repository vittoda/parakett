package com.parakett.cli;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.JsonUtils;
import com.parakett.ws.ClientSession;

public class HelpCommand extends CliCommand {

    public HelpCommand() {
        super("help");

    }

    @Override
    public CliResponse run(String params, ClientSession session) throws CliException {
        ObjectNode resp = JsonUtils.MAPPER.createObjectNode();

        ArrayNode list = CliCommandRegistry.listCommands();

        resp.put("status", "success");
        resp.put("type", "list");
        resp.set("message", list);

        return new CliResponse(resp);

    }
}
