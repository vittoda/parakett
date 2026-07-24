package com.parakett.cli;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class CliResponse {

    public ObjectNode responseObject = null;

    public CliResponse(ObjectNode response) {
        this.responseObject = response;
    }
    
}
