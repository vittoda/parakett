package com.parakett.models;

import java.util.List;

public class ModelUserMessage extends ModelMessage{

    public List<String> content = null;

    public ModelUserMessage(List<String> content) {
        this.content = content;
    }
    
}
