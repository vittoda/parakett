package com.parakett.cli;

public class CliException extends Exception {

    public CliException(Exception e) {
        super(e);
    }

    public CliException(String e) {
        super(e);
    }
    
}
