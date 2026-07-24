package com.parakett.api.objects;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class AgentConfig {
    
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

}
