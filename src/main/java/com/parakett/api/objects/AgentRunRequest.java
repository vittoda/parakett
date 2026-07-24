package com.parakett.api.objects;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class AgentRunRequest {

    @Getter
    @Setter
    @NotNull(message = "enableDebug is required")
    private Boolean enableDebug;

    @Getter
    @Setter
    @NotNull(message = "enableLog is required")
    private Boolean enableLog;

    @Getter
    @Setter
    @NotNull(message = "enableArchive is required")
    private Boolean enableArchive;

    @Getter
    @Setter
    @NotNull(message = "Input is required")
    private String input;

    @Getter
    @Setter
    private List<VariableRequestObject> variables;
    
}
