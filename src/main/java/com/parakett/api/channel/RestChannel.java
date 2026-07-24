package com.parakett.api.channel;

import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.parakett.channels.base.CommChannelBase;
import com.parakett.channels.base.CommChannelInstance;

public class RestChannel implements CommChannelBase{

    private HashMap<String, CommChannelInstance> _mInstances = new HashMap<>();
    public static final RestChannel INSTANCE = new RestChannel();

    private RestChannel() {

    }

    @Override
    public String getName() {
        return "REST";
    }

    @Override
    public String getKey() {
       return "rest";
    }

    @Override
    public CommChannelInstance createInstance(String key, JsonNode config) {
      RestChannelInstance instance = new RestChannelInstance();
      _mInstances.put(key, instance);
      return instance;
    }

    @Override
    public CommChannelInstance getInstance(String key) {
        return _mInstances.get(key);
    }
    
}
