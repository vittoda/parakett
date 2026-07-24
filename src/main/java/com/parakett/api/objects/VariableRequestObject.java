package com.parakett.api.objects;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class VariableRequestObject {

    @Getter
    @Setter
    @NotNull(message = "name is required")
    private String name;

    @Getter
    @Setter
    @NotNull(message = "value is required")
    private String value;
    
}
