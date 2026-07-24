package com.parakett.api.objects;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class DefaultModelRequest {
    @Getter
    @Setter
    @NotNull(message = "modelName is required")
    private String modelName;
}
