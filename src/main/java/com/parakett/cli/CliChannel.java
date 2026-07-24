package com.parakett.cli;

import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.parakett.channels.base.CommChannelBase;
import com.parakett.channels.base.CommChannelInstance;

public class CliChannel implements CommChannelBase {

    public static final CliChannel INSTANCE = new CliChannel();

    private HashMap<String, CommChannelInstance> _mInstances = new HashMap<>();

    private CliChannel() {

    }

    @Override
    public String getName() {
        return "CLI";
    }

    @Override
    public String getKey() {
        return "cli";
    }

    @Override
    public CommChannelInstance createInstance(String key, JsonNode config) {
        CliChannelInstance instance = new CliChannelInstance();
        _mInstances.put(key, instance);
        return instance;
    }

    @Override
    public CommChannelInstance getInstance(String key) {
        return _mInstances.get(key);
    }

}
