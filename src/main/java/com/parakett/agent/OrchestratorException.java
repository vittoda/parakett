package com.parakett.agent;

public class OrchestratorException extends Exception{

    public OrchestratorException(String e) {
        super(e);
    }

    public OrchestratorException(Exception e) {
        super(e);
    }
    
}
