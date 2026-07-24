package com.parakett.flow;

public class FlowNonRecoverableException extends FlowException{

    public FlowNonRecoverableException(String e) {
        super(e);
    }

    public FlowNonRecoverableException(Exception e) {
        super(e);
    }
    
}
