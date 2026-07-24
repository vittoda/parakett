package com.parakett.models;

public class ModelNonRecoverableException extends ModelException {

    public ModelNonRecoverableException(String e) {
        super(e);
    }

     public ModelNonRecoverableException(Exception e) {
        super(e);
    }
    
}
