package com.parakett.metrics;

public class MetricsException extends Exception {

    public MetricsException(String e) {
        super(e);
    }

    public MetricsException(Exception e) {
        super(e);
    }
     
}
