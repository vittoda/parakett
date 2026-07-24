package com.parakett.flow;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ToolStepResult extends StepResult {

    String toolId = null;
    String result = null;

    public ToolStepResult(ObjectNode astMsg, String toolId, String result) {
        super(astMsg);
        this.toolId = toolId;
        this.result = result;
    }
    
}
