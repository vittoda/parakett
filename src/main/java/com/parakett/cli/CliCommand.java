package com.parakett.cli;

import com.parakett.ws.ClientSession;

public abstract class CliCommand {

    public String name = null;

    public CliCommand(String name) {
        this.name = name;
    }

    public abstract CliResponse run(String params, ClientSession session) throws CliException;
    
}
