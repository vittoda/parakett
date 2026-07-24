package com.parakett.models;

import java.util.List;

import com.parakett.flow.FlowMemory;


public abstract class ModelConnection {

     public abstract ModelResponse sendRequest(FlowMemory memory, 
          List<String> additionalSystemMessages, 
          List<String> userMessages, List<ToolMessage> tools, boolean jsonRespnse) throws ModelException;

    
}
