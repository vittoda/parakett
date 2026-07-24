package com.parakett.cli;

import java.util.HashMap;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.parakett.JsonUtils;

public class CliCommandRegistry {

    private static HashMap<String, CliCommand> _mCommands = new HashMap<>();

    public static void addCommand(CliCommand cmd) {
        _mCommands.put(cmd.name, cmd);
    }

    public static CliCommand getCommand(String name) {
       return _mCommands.get(name);
    }

    public static ArrayNode listCommands() {
        ArrayNode list = JsonUtils.MAPPER.createArrayNode();
        for(String k : _mCommands.keySet()) {
            list.add(k);
        }
        return list;
    }
}
