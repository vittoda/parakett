package com.parakett.flow;

public class FlowException extends Exception {

    public FlowException(String e) {
        super(e);
    }

    public FlowException(Exception e) {
        super(e);
    }
    
}
